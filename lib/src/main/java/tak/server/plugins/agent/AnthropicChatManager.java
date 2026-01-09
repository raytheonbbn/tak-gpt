package tak.server.plugins.agent;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

import tak.server.plugins.LLMChatManager;
import tak.server.plugins.config.TAKContext;

public class AnthropicChatManager implements LLMChatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicChatManager.class);
    private Map<String, ? extends Object> config;
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private String systemPrompt;

    public AnthropicChatManager(Map<String, ? extends Object> config) {
        this.config = config;

        if(config.containsKey("apiKey")) {
            apiKey = (String)config.get("apiKey");
        } else {
            LOGGER.error("Unable to locate property for apiKey in configuration");
        }

        if(config.containsKey("baseUrl")) {
            baseUrl = (String)config.get("baseUrl");
        } else {
            LOGGER.error("Unable to locate property for baseUrl in configuration");
        }

        if(config.containsKey("modelName")) {
            modelName = (String)config.get("modelName");
        } else {
            LOGGER.error("Unable to locate property for modelName in configuration");
        }

        systemPrompt = loadSystemPromptFromConfig(config);
        
    }

    @Override
    public String sendChatRequest(String messageText, TAKContext context) throws IOException {
        
        AnthropicClient client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

        String positionText = "\n\nMy current position: " + context.getLat() + ", " + context.getLon() + "\nFacing: 313.6° (NW)\nField of view: 75° with range 50m";

        MessageCreateParams params = MessageCreateParams.builder()
            .maxTokens(1024L)
            .addUserMessage(messageText + positionText)
            .system(systemPrompt)
            .model(modelName)
            .build();

        Message message = client.messages().create(params);

        StringBuilder builder = new StringBuilder();
        for(ContentBlock block : message.content()) {
            if(block.isText()) {
                builder.append(block.asText().text());
            }
        }

        return builder.toString();
    }
}
