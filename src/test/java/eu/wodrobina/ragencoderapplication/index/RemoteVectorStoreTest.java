package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.config.VectorStoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RemoteVectorStoreTest {

    @Mock
    private VectorStoreProperties properties;

    private RemoteVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        when(properties.getBaseUrl()).thenReturn("http://localhost:8080");
        vectorStore = new RemoteVectorStore(properties);
    }

    @Test
    void RemoteVectorStore_shouldSuccessfullyInitializeWithProperties() {
        assertNotNull(vectorStore);
    }

    @Test
    void RemoteVectorStore_upsert_doesNotThrowException() {
        List<VectorDocument> docs = new ArrayList<>();
        assertDoesNotThrow(() -> vectorStore.upsert(docs, "test-collection"));
    }
}
