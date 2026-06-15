package eu.wodrobina.ragencoderapplication.reranking;

import eu.wodrobina.ragencoderapplication.index.SearchResult;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LexicalOverlapReranker implements Reranker {

    private static final Pattern NON_TOKEN_CHARS = Pattern.compile("[^\\p{L}\\p{N}\\s]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final double vectorWeight;
    private final double lexicalWeight;

    public LexicalOverlapReranker(double vectorWeight, double lexicalWeight) {
        this.vectorWeight = vectorWeight;
        this.lexicalWeight = lexicalWeight;
    }

    @Override
    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }

        return candidates.stream()
                .map(result -> new RankedItem(result, calculateFinalScore(query, result)))
                .sorted(Comparator.comparingDouble(RankedItem::finalScore).reversed())
                .limit(limit)
                .map(RankedItem::result)
                .toList();
    }

    private double calculateFinalScore(String query, SearchResult result) {
        double overlapScore = calculateOverlap(query, result.content());
        return vectorWeight * result.score() + lexicalWeight * overlapScore;
    }

    private double calculateOverlap(String query, String content) {
        if (query == null || content == null) {
            return 0.0;
        }

        Set<String> queryTokens = tokenize(query);
        Set<String> contentTokens = tokenize(content);

        if (queryTokens.isEmpty() || contentTokens.isEmpty()) {
            return 0.0;
        }

        long commonCount = queryTokens.stream()
                .filter(contentTokens::contains)
                .count();

        return (double) commonCount / queryTokens.size();
    }

    private Set<String> tokenize(String text) {
        String normalized = NON_TOKEN_CHARS.matcher(text.toLowerCase()).replaceAll(" ");

        return Arrays.stream(WHITESPACE.split(normalized))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private record RankedItem(SearchResult result, double finalScore) {}
}