package tak.server.plugins.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.azure.AzureOpenAIServiceVersion;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClient.Builder;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import tak.server.plugins.LLMChatManager;
import tak.server.plugins.config.TAKContext;


public class OpenAIAPIChatManager implements LLMChatManager {
    private OpenAIClient client;
    private String systemPrompt;
    private String modelName = "";
    private String apiVersion = null;
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIAPIChatManager.class);

    public OpenAIAPIChatManager(Map<String, ? extends Object> config) {
        String apiKey = "";
        if(config.containsKey("apiKey")) {
            apiKey = (String)config.get("apiKey");
        } else {
            LOGGER.error("Unable to locate property for apiKey in configuration");
        }

        String baseURL = "";
        if(config.containsKey("baseURL")) {
            baseURL = (String)config.get("baseURL");
        } else {
            LOGGER.error("Unable to locate property for baseURL in configuration");
        }

        if(config.containsKey("modelName")) {
            modelName = (String)config.get("modelName");
        } else {
            LOGGER.error("Unable to locate property for modelName in configuration");
        }

        if(config.containsKey("apiVersion")) {
            apiVersion = (String)config.get("apiVersion");
        }
        

        systemPrompt = loadSystemPromptFromConfig(config);
        
        client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseURL)
            .build();
    }

    public OpenAIAPIChatManager(Map<String, ? extends Object> config, String apiKey, String baseURL) {
       
        systemPrompt = loadSystemPromptFromConfig(config);
        
        Builder builder = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseURL);

        if(apiVersion != null && !apiVersion.isEmpty()) {
            builder = builder.azureServiceVersion(AzureOpenAIServiceVersion.fromString(apiVersion));
        }
 

         client = builder.build();
    }

    @Override
    public String sendChatRequest(String messageText, TAKContext context) throws IOException {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .addUserMessage(messageText)
            .addSystemMessage(systemPrompt)
            .model(modelName) //gpt-4-1-mini-2025-04-14-ft-cc9dc3d17609468799a62e4d42974a4b
            .build();
        
        String result;
        try {
            LOGGER.info("T1");
            ChatCompletion chatCompletion = client.chat().completions().create(params);
            LOGGER.info("T2");
            result = chatCompletion.choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .collect(Collectors.joining());
            LOGGER.info("T3");
        } catch (Throwable t) {
            LOGGER.error("Exception while calling LLM", t);
            result = "Error while talking to bot";
        }
        return result;
    }
    
}
