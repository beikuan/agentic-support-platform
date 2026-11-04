package org.vaadin.marcus.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

/**
 *  In the real world, ingesting documents would happen separately, not in the same application.
 */
@Component
public class DocumentationIngestor implements CommandLineRunner {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Tokenizer tokenizer;
    private final Resource termsOfService;
    private final boolean ingestOnStartup;

    public DocumentationIngestor(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            Tokenizer tokenizer,
            @Value("classpath:terms-of-service.txt") Resource termsOfService,
            @Value("${support.rag.ingest-on-startup:true}") boolean ingestOnStartup) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.tokenizer = tokenizer;
        this.termsOfService = termsOfService;
        this.ingestOnStartup = ingestOnStartup;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!ingestOnStartup) return;
        Document doc;
        try (var input = termsOfService.getInputStream()) {
            doc = Document.from(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        var ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(50, 0, tokenizer))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(doc);
    }
}
