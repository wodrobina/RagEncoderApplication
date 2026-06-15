package eu.wodrobina.ragencoderapplication.chunking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextChunker implements Chunker {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_][\\w.]*)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?[\\w.*]+\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "(?m)(?:^|\\n)\\s*(?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*" +
                    "(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static|strictfp)\\s+)*" +
                    "(class|interface|enum|record|@interface)\\s+([A-Za-z_$][\\w$]*)\\b"
    );

    private static final List<String> CONTROL_KEYWORDS = List.of(
            "if", "for", "while", "switch", "catch", "try", "else", "do", "synchronized"
    );

    private final int chunkSize;
    private final int overlap;

    public TextChunker(
            @Value("${rag.chunk-size:1200}") int chunkSize,
            @Value("${rag.chunk-overlap:200}") int overlap
    ) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("Overlap must not be negative");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("Overlap must be smaller than chunk size");
        }

        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<Chunk> chunk(String sourceId, String text, Map metadata) {
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : new HashMap<>(metadata);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = normalize(text);

        if (!isJavaSource(sourceId, safeMetadata)) {
            return chunkAsPlainText(sourceId, normalized, safeMetadata);
        }

        List<ChunkCandidate> candidates = javaChunks(normalized);

        if (candidates.isEmpty()) {
            return chunkAsPlainText(sourceId, normalized, safeMetadata);
        }

        return materializeCandidates(sourceId, normalized, safeMetadata, candidates);
    }

    private List<ChunkCandidate> javaChunks(String source) {
        String masked = maskCommentsAndStrings(source);
        String packageName = findPackage(source).orElse("");
        String importBlock = findImportBlock(source);
        String fileContext = buildFileContext(packageName, importBlock);

        List<JavaType> types = findTypes(source, masked);

        if (types.isEmpty()) {
            return List.of(new ChunkCandidate(
                    source,
                    0,
                    source.length(),
                    Map.of(
                            "chunker", "java-structure-v1",
                            "java_member_kind", "file",
                            "java_package", packageName
                    )
            ));
        }

        List<ChunkCandidate> chunks = new ArrayList<>();

        for (JavaType type : types) {
            List<JavaMember> members = findMembers(source, masked, type);

            String typeHeader = source.substring(type.startOffset(), Math.min(type.bodyStartOffset() + 1, source.length())).trim();
            String typeContext = buildTypeContext(fileContext, typeHeader);

            String overview = buildTypeOverview(source, type, members, fileContext);
            if (!overview.isBlank()) {
                chunks.add(new ChunkCandidate(
                        overview,
                        type.startOffset(),
                        type.bodyStartOffset() + 1,
                        Map.of(
                                "chunker", "java-structure-v1",
                                "java_member_kind", "type_overview",
                                "java_package", packageName,
                                "java_type", type.name(),
                                "java_type_kind", type.kind()
                        )
                ));
            }

            for (JavaMember member : members) {
                String memberSource = source.substring(member.startOffset(), member.endOffset()).trim();
                String content = typeContext + "\n\n" + memberSource;

                chunks.add(new ChunkCandidate(
                        content,
                        member.startOffset(),
                        member.endOffset(),
                        Map.of(
                                "chunker", "java-structure-v1",
                                "java_member_kind", member.kind(),
                                "java_package", packageName,
                                "java_type", type.name(),
                                "java_type_kind", type.kind(),
                                "java_member", member.name()
                        )
                ));
            }

            if (members.isEmpty()) {
                String wholeType = source.substring(type.startOffset(), type.endOffset()).trim();
                chunks.add(new ChunkCandidate(
                        fileContext.isBlank() ? wholeType : fileContext + "\n\n" + wholeType,
                        type.startOffset(),
                        type.endOffset(),
                        Map.of(
                                "chunker", "java-structure-v1",
                                "java_member_kind", "type",
                                "java_package", packageName,
                                "java_type", type.name(),
                                "java_type_kind", type.kind()
                        )
                ));
            }
        }

        return chunks;
    }

    private List<Chunk> materializeCandidates(
            String sourceId,
            String normalized,
            Map<String, Object> baseMetadata,
            List<ChunkCandidate> candidates
    ) {
        List<Chunk> result = new ArrayList<>();
        LineIndex lineIndex = LineIndex.from(normalized);
        String fileName = resolveFileName(sourceId, baseMetadata);
        String fileType = resolveFileType(fileName);

        int chunkIndex = 0;

        for (ChunkCandidate candidate : candidates) {
            String content = candidate.content().trim();

            if (content.isBlank()) {
                continue;
            }

            if (content.length() <= chunkSize) {
                result.add(toChunk(
                        sourceId,
                        content,
                        baseMetadata,
                        candidate.metadata(),
                        chunkIndex++,
                        candidate.startOffset(),
                        candidate.endOffset(),
                        lineIndex,
                        fileName,
                        fileType
                ));
                continue;
            }

            List<TextSlice> slices = splitLargeContent(content);
            for (TextSlice slice : slices) {
                int startOffset = candidate.startOffset() + slice.start();
                int endOffset = Math.min(candidate.startOffset() + slice.end(), candidate.endOffset());

                result.add(toChunk(
                        sourceId,
                        slice.content(),
                        baseMetadata,
                        candidate.metadata(),
                        chunkIndex++,
                        startOffset,
                        endOffset,
                        lineIndex,
                        fileName,
                        fileType
                ));
            }
        }

        return result;
    }

    private Chunk toChunk(
            String sourceId,
            String content,
            Map<String, Object> baseMetadata,
            Map<String, Object> chunkMetadata,
            int chunkIndex,
            int startOffset,
            int endOffset,
            LineIndex lineIndex,
            String fileName,
            String fileType
    ) {
        Map<String, Object> metadata = new HashMap<>(baseMetadata);
        metadata.putAll(chunkMetadata);
        metadata.put("chunk_index", chunkIndex);
        metadata.put("start_offset", startOffset);
        metadata.put("end_offset", endOffset);
        metadata.put("start_line", lineIndex.lineOf(startOffset));
        metadata.put("end_line", lineIndex.lineOf(Math.max(startOffset, endOffset - 1)));

        String contentHash = generateHash(content);

        return new Chunk(
                stableId(sourceId, chunkIndex, content),
                content,
                Map.copyOf(metadata),
                sourceId,
                chunkIndex,
                contentHash,
                contentHash,
                fileName,
                fileType
        );
    }

    private List<Chunk> chunkAsPlainText(String sourceId, String text, Map<String, Object> metadata) {
        List<Chunk> chunks = new ArrayList<>();
        LineIndex lineIndex = LineIndex.from(text);
        String fileName = resolveFileName(sourceId, metadata);
        String fileType = resolveFileType(fileName);

        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String content = text.substring(start, end).trim();

            if (!content.isBlank()) {
                Map<String, Object> chunkMetadata = new HashMap<>(metadata);
                chunkMetadata.put("chunk_index", index);
                chunkMetadata.put("start_offset", start);
                chunkMetadata.put("end_offset", end);
                chunkMetadata.put("start_line", lineIndex.lineOf(start));
                chunkMetadata.put("end_line", lineIndex.lineOf(Math.max(start, end - 1)));
                chunkMetadata.put("chunker", "text-fixed-v1");

                String contentHash = generateHash(content);

                chunks.add(new Chunk(
                        stableId(sourceId, index, content),
                        content,
                        Map.copyOf(chunkMetadata),
                        sourceId,
                        index,
                        contentHash,
                        contentHash,
                        fileName,
                        fileType
                ));
            }

            if (end == text.length()) {
                break;
            }

            start = end - overlap;
            index++;
        }

        return chunks;
    }

    private List<TextSlice> splitLargeContent(String content) {
        List<TextSlice> slices = new ArrayList<>();

        int start = 0;

        while (start < content.length()) {
            int preferredEnd = Math.min(start + chunkSize, content.length());
            int end = findGoodSplitPoint(content, start, preferredEnd);

            String slice = content.substring(start, end).trim();

            if (!slice.isBlank()) {
                slices.add(new TextSlice(start, end, slice));
            }

            if (end == content.length()) {
                break;
            }

            start = Math.max(0, end - overlap);
        }

        return slices;
    }

    private int findGoodSplitPoint(String content, int start, int preferredEnd) {
        if (preferredEnd == content.length()) {
            return preferredEnd;
        }

        int paragraphBreak = content.lastIndexOf("\n\n", preferredEnd);
        if (paragraphBreak > start + chunkSize / 2) {
            return paragraphBreak;
        }

        int lineBreak = content.lastIndexOf('\n', preferredEnd);
        if (lineBreak > start + chunkSize / 2) {
            return lineBreak;
        }

        return preferredEnd;
    }

    private List<JavaType> findTypes(String source, String masked) {
        List<JavaType> types = new ArrayList<>();
        Matcher matcher = TYPE_PATTERN.matcher(masked);

        while (matcher.find()) {
            int declarationStart = skipLeadingWhitespace(source, matcher.start());
            int bodyStart = masked.indexOf('{', matcher.end());

            if (bodyStart < 0) {
                continue;
            }

            int bodyEnd = findMatchingBrace(masked, bodyStart);
            if (bodyEnd < 0) {
                continue;
            }

            types.add(new JavaType(
                    matcher.group(1),
                    matcher.group(2),
                    declarationStart,
                    bodyStart,
                    bodyEnd + 1
            ));
        }

        return types.stream()
                .sorted(Comparator.comparingInt(JavaType::startOffset))
                .toList();
    }

    private List<JavaMember> findMembers(String source, String masked, JavaType type) {
        List<JavaMember> members = new ArrayList<>();

        int depth = 0;
        int scanStart = type.bodyStartOffset();
        int scanEnd = type.endOffset();

        for (int i = scanStart; i < scanEnd; i++) {
            char current = masked.charAt(i);

            if (current == '{') {
                depth++;

                if (depth == 2) {
                    Optional<JavaMember> member = tryCreateMember(source, masked, type, i);
                    member.ifPresent(members::add);
                }
            } else if (current == '}') {
                depth--;
            }
        }

        return members.stream()
                .distinct()
                .sorted(Comparator.comparingInt(JavaMember::startOffset))
                .toList();
    }

    private Optional<JavaMember> tryCreateMember(String source, String masked, JavaType type, int bodyStart) {
        int headerStart = findMemberHeaderStart(masked, type.bodyStartOffset() + 1, bodyStart);
        String header = source.substring(headerStart, bodyStart).trim();

        if (!looksLikeMethodOrConstructor(header)) {
            return Optional.empty();
        }

        String memberName = resolveMemberName(header, type.name());
        if (memberName.isBlank()) {
            return Optional.empty();
        }

        int bodyEnd = findMatchingBrace(masked, bodyStart);
        if (bodyEnd < 0) {
            return Optional.empty();
        }

        String kind = memberName.equals(type.name()) ? "constructor" : "method";

        return Optional.of(new JavaMember(
                kind,
                memberName,
                includeLeadingAnnotations(source, headerStart, type.bodyStartOffset() + 1),
                bodyEnd + 1
        ));
    }

    private boolean looksLikeMethodOrConstructor(String header) {
        String compact = header.strip();

        if (!compact.contains("(") || !compact.contains(")")) {
            return false;
        }

        if (compact.contains("=")) {
            return false;
        }

        String firstWord = firstWord(compact);
        if (CONTROL_KEYWORDS.contains(firstWord)) {
            return false;
        }

        return !compact.endsWith(";");
    }

    private String resolveMemberName(String header, String typeName) {
        String beforeParen = header.substring(0, header.indexOf('(')).trim();

        if (beforeParen.isBlank()) {
            return "";
        }

        String[] tokens = beforeParen
                .replace("\n", " ")
                .replace("\t", " ")
                .trim()
                .split("\\s+");

        if (tokens.length == 0) {
            return "";
        }

        String lastToken = tokens[tokens.length - 1];

        if (lastToken.contains(".")) {
            lastToken = lastToken.substring(lastToken.lastIndexOf('.') + 1);
        }

        if (lastToken.equals(typeName)) {
            return typeName;
        }

        return lastToken.replaceAll("[^A-Za-z0-9_$]", "");
    }

    private int findMemberHeaderStart(String masked, int minOffset, int bodyStart) {
        int current = bodyStart - 1;

        while (current >= minOffset) {
            char c = masked.charAt(current);

            if (c == ';' || c == '}') {
                return current + 1;
            }

            current--;
        }

        return minOffset;
    }

    private int includeLeadingAnnotations(String source, int headerStart, int minOffset) {
        int currentLineStart = lineStart(source, headerStart);

        while (currentLineStart > minOffset) {
            int previousLineEnd = currentLineStart - 1;
            int previousLineStart = lineStart(source, Math.max(minOffset, previousLineEnd - 1));
            String previousLine = source.substring(previousLineStart, previousLineEnd).trim();

            if (previousLine.isBlank() || previousLine.startsWith("@")) {
                currentLineStart = previousLineStart;
                continue;
            }

            break;
        }

        return Math.max(minOffset, currentLineStart);
    }

    private String buildTypeOverview(String source, JavaType type, List<JavaMember> members, String fileContext) {
        String typeHeader = source.substring(type.startOffset(), Math.min(type.bodyStartOffset() + 1, source.length())).trim();

        List<String> memberSignatures = members.stream()
                .map(member -> source.substring(member.startOffset(), Math.min(member.bodyStartOffset(), source.length())).trim())
                .filter(signature -> !signature.isBlank())
                .toList();

        StringBuilder overview = new StringBuilder();

        if (!fileContext.isBlank()) {
            overview.append(fileContext).append("\n\n");
        }

        overview.append(typeHeader).append("\n");

        if (!memberSignatures.isEmpty()) {
            overview.append("\n// Members:\n");
            for (String signature : memberSignatures) {
                overview.append(signature.replaceAll("\\s+", " ")).append(" { ... }\n");
            }
        }

        return overview.toString().trim();
    }

    private Optional<String> findPackage(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(matcher.group(1));
    }

    private String findImportBlock(String source) {
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        StringBuilder imports = new StringBuilder();

        while (matcher.find()) {
            imports.append(matcher.group()).append("\n");
        }

        return imports.toString().trim();
    }

    private String buildFileContext(String packageName, String importBlock) {
        StringBuilder context = new StringBuilder();

        if (packageName != null && !packageName.isBlank()) {
            context.append("package ").append(packageName).append(";");
        }

        if (importBlock != null && !importBlock.isBlank()) {
            if (!context.isEmpty()) {
                context.append("\n\n");
            }
            context.append(importBlock);
        }

        return context.toString().trim();
    }

    private String buildTypeContext(String fileContext, String typeHeader) {
        if (fileContext == null || fileContext.isBlank()) {
            return typeHeader;
        }

        return fileContext + "\n\n" + typeHeader;
    }

    private boolean isJavaSource(String sourceId, Map<String, Object> metadata) {
        String fileName = resolveFileName(sourceId, metadata);
        String fileType = resolveFileType(fileName);

        return "java".equalsIgnoreCase(fileType) || fileName.toLowerCase().endsWith(".java");
    }

    private String resolveFileName(String sourceId, Map<String, Object> metadata) {
        Object fileName = metadata.get("fileName");
        if (fileName instanceof String value && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
            return value;
        }

        Object snakeCaseFileName = metadata.get("file_name");
        if (snakeCaseFileName instanceof String value && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
            return value;
        }

        Object filePath = metadata.get("file_path");
        if (filePath instanceof String value && !value.isBlank()) {
            try {
                Path name = Path.of(value).getFileName();
                if (name != null) {
                    return name.toString();
                }
            } catch (Exception ignored) {
                return value;
            }
        }

        if (sourceId != null && !sourceId.isBlank()) {
            try {
                Path name = Path.of(sourceId).getFileName();
                if (name != null) {
                    return name.toString();
                }
            } catch (Exception ignored) {
                return sourceId;
            }
        }

        return "unknown";
    }

    private String resolveFileType(String fileName) {
        if (fileName == null || fileName.isBlank() || "unknown".equalsIgnoreCase(fileName)) {
            return "unknown";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "unknown";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private String maskCommentsAndStrings(String source) {
        StringBuilder masked = new StringBuilder(source);
        State state = State.CODE;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            switch (state) {
                case CODE -> {
                    if (current == '/' && next == '/') {
                        masked.setCharAt(i, ' ');
                        masked.setCharAt(i + 1, ' ');
                        i++;
                        state = State.LINE_COMMENT;
                    } else if (current == '/' && next == '*') {
                        masked.setCharAt(i, ' ');
                        masked.setCharAt(i + 1, ' ');
                        i++;
                        state = State.BLOCK_COMMENT;
                    } else if (current == '"') {
                        masked.setCharAt(i, ' ');
                        state = State.STRING;
                    } else if (current == '\'') {
                        masked.setCharAt(i, ' ');
                        state = State.CHAR;
                    }
                }
                case LINE_COMMENT -> {
                    if (current == '\n') {
                        state = State.CODE;
                    } else {
                        masked.setCharAt(i, ' ');
                    }
                }
                case BLOCK_COMMENT -> {
                    masked.setCharAt(i, current == '\n' ? '\n' : ' ');
                    if (current == '*' && next == '/') {
                        masked.setCharAt(i + 1, ' ');
                        i++;
                        state = State.CODE;
                    }
                }
                case STRING -> {
                    masked.setCharAt(i, current == '\n' ? '\n' : ' ');
                    if (current == '\\') {
                        if (i + 1 < source.length()) {
                            masked.setCharAt(i + 1, source.charAt(i + 1) == '\n' ? '\n' : ' ');
                            i++;
                        }
                    } else if (current == '"') {
                        state = State.CODE;
                    }
                }
                case CHAR -> {
                    masked.setCharAt(i, current == '\n' ? '\n' : ' ');
                    if (current == '\\') {
                        if (i + 1 < source.length()) {
                            masked.setCharAt(i + 1, source.charAt(i + 1) == '\n' ? '\n' : ' ');
                            i++;
                        }
                    } else if (current == '\'') {
                        state = State.CODE;
                    }
                }
            }
        }

        return masked.toString();
    }

    private int findMatchingBrace(String source, int openBraceOffset) {
        int depth = 0;

        for (int i = openBraceOffset; i < source.length(); i++) {
            char current = source.charAt(i);

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;

                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private int lineStart(String source, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, source.length()));

        for (int i = safeOffset; i >= 0; i--) {
            if (i == 0 || source.charAt(i - 1) == '\n') {
                return i;
            }
        }

        return 0;
    }

    private int skipLeadingWhitespace(String source, int offset) {
        int current = Math.max(0, offset);

        while (current < source.length() && Character.isWhitespace(source.charAt(current))) {
            current++;
        }

        return current;
    }

    private String firstWord(String value) {
        String[] parts = value.strip().split("\\s+");
        return parts.length == 0 ? "" : parts[0];
    }

    private static String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private static String stableId(String sourceId, int index, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(String.valueOf(sourceId).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(Integer.toString(index).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate chunk id", e);
        }
    }

    private static String generateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate content hash", e);
        }
    }

    private enum State {
        CODE,
        LINE_COMMENT,
        BLOCK_COMMENT,
        STRING,
        CHAR
    }

    private record JavaType(
            String kind,
            String name,
            int startOffset,
            int bodyStartOffset,
            int endOffset
    ) {
    }

    private record JavaMember(
            String kind,
            String name,
            int startOffset,
            int endOffset
    ) {
        int bodyStartOffset() {
            return startOffset;
        }
    }

    private record ChunkCandidate(
            String content,
            int startOffset,
            int endOffset,
            Map<String, Object> metadata
    ) {
    }

    private record TextSlice(
            int start,
            int end,
            String content
    ) {
    }

    private record LineIndex(List<Integer> lineStarts) {

        static LineIndex from(String text) {
            List<Integer> starts = new ArrayList<>();
            starts.add(0);

            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n' && i + 1 < text.length()) {
                    starts.add(i + 1);
                }
            }

            return new LineIndex(List.copyOf(starts));
        }

        int lineOf(int offset) {
            int safeOffset = Math.max(0, offset);

            int low = 0;
            int high = lineStarts.size() - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                int lineStart = lineStarts.get(mid);

                if (lineStart == safeOffset) {
                    return mid + 1;
                }

                if (lineStart < safeOffset) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }

            return Math.max(1, high + 1);
        }
    }
}