package tak.server.plugins.agent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import tak.server.plugins.LLMChatManager;
import tak.server.plugins.config.TAKContext;

/**
 * A bot/agent manager for an Ollama-served LLM
 * 
 * To start ollama listening on more than just 127.0.0.1 do the following:
 * set OLLAMA_HOST=0.0.0.0
 * ollama serve
 * 
 * To change the port Ollama runs on you can set the OLLAMA_HOST environment variable to 0.0.0.0:11435
 */
public class OllamaChatManager implements LLMChatManager {
	private Assistant assistant;
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaChatManager.class);
	private Integer port = 11434;
	private String host = "127.0.0.1";
	private String modelName = "mistral-small";
	private String ragDirectory = null;
	private String embeddingModelName = null;
	private boolean RAG = false;
	
	public OllamaChatManager(Map<String, ? extends Object> config) {
		LOGGER.info("Ollama Chat Manager Initializing");
		LOGGER.info("Using properties: " + config.keySet());
		
		if(config.containsKey("modelPort")) {
			port = (Integer)config.get("modelPort");
		}
		if(config.containsKey("modelHost")) {
			host = (String)config.get("modelHost");
		}
		if(config.containsKey("modelName")) {
			modelName = (String)config.get("modelName");
		}
		if(config.containsKey("ragDirectory")) {
			ragDirectory  = (String)config.get("ragDirectory");

			if(config.containsKey("embeddingModel")) {
				RAG = true;
				embeddingModelName = (String)config.get("embeddingModel");
			} else {
				RAG = false;
				LOGGER.error("RAG directory provided, but not embedding model specified. Please specify embeddingModel paramater");
			}
		}
		
		LOGGER.info("Will look for models at http://" + host + ":" + port);
		LOGGER.info("Will use model: " + modelName);
		if(RAG) {
			LOGGER.info("Will look for RAG documents at: " + ragDirectory);
		}

		String systemPrompt = loadSystemPromptFromConfig(config);
		Function<Object, String> systemPromptSupplier = x -> {
            return systemPrompt;
        };
		
		
		LOGGER.info("Starting chat assistant");
		AiServices<Assistant> asstBldr = AiServices.builder(Assistant.class)
				.chatModel(
					OllamaChatModel.builder()
						.baseUrl("http://" + host + ":" + port)
						.modelName(modelName)
						.build())
				.chatMemory(MessageWindowChatMemory.withMaxMessages(10)); // it should remember 10 latest messages
		
		if(systemPrompt != null && !systemPrompt.isEmpty()) {
			asstBldr.systemMessageProvider(systemPromptSupplier);
		}
        		
		if(RAG) {
			asstBldr.contentRetriever(createContentRetriever()); // it should have access to our documents
		}
		
		assistant = asstBldr.build();
		LOGGER.info("Ollama Chat Manager Initialization Complete");
	}

	@Override
	public String sendChatRequest(String messageText, TAKContext context) throws IOException {
        String answer = assistant.answer(messageText);
        LOGGER.info("Asked TAK GPT: " + messageText + ", and TAK GPT answered: " + answer);
        return answer;
	}
	
	private ContentRetriever createContentRetriever() {
		OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
			.baseUrl("http://" + host + ":" + port)
			.modelName(embeddingModelName)
			.build();

		EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
		ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
			.embeddingStore(embeddingStore)
			.embeddingModel(embeddingModel)
			.maxResults(5) // Number of relevant chunks to retrieve
			.build();

		List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(Paths.get(ragDirectory), new ApachePdfBoxDocumentParser());
		LOGGER.info("Found " + documents.size() + " documents for RAG in " + Paths.get(ragDirectory));
		
		File dir = new File(ragDirectory);
		LOGGER.info("Files: " + Arrays.asList(dir.list()));

		for(Document document : documents) {
			DocumentSplitter splitter = DocumentSplitters.recursive(600, 0);
			List<TextSegment> segments = splitter.split(document);
			List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
			embeddingStore.addAll(embeddings, segments);
		}

		return contentRetriever;
    }
}
