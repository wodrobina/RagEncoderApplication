package eu.wodrobina.ragencoderapplication.reranking;

import eu.wodrobina.ragencoderapplication.index.SearchResult;
import java.util.List;

/**
 * Interface for reranking search results based on a specific strategy.
 */
public interface Reranker {
    /**
     * Re-ranks candidates using the provided query and logic.
     *
     * @param query The original user query string.
     * @param candidates The initial set of results from the vector store.
     * @param limit The final number of items to return.
     * @return A ranked list of search results.
     */
    List<SearchResult> rerank(String query, List<SearchResult> candidates, int limit);
}
