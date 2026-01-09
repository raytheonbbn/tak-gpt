package tak.server.plugins.agent;

import java.io.IOException;
import java.util.Map;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel;
import tak.server.plugins.LLMChatManager;
import tak.server.plugins.config.TAKContext;

/**
 * A bot/agent manager for a Vertex AI-served LLM
 * 
 * BETA - not yet tested with authentication
 */
public class VertexAIChatManager implements LLMChatManager{
    private String projectID;
    private String location;
    private String modelName;
    private String serviceAccountFileLocation;

    public VertexAIChatManager(Map<String, ? extends Object> config) {
        if(!config.containsKey("projectID")
            || !config.containsKey("locationID")
            || !config.containsKey("modelName")
            || !config.containsKey("serviceAccountFileLocation")){
                throw new RuntimeException("Unable to start, missing configuration parameter(s)");
        }

        serviceAccountFileLocation = (String)config.get("serviceAccountFileLocation");
        projectID = (String)config.get("projectID");
        location = (String)config.get("locationID");
        modelName = (String)config.get("modelName");

        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", serviceAccountFileLocation);
    }

    @Override
    public String sendChatRequest(String messageText, TAKContext context) throws IOException {
        ChatModel model = VertexAiGeminiChatModel.builder()
            .project(projectID)
            .location(location)
            .modelName(modelName)
            .build();

        return model.chat("Explain quantum computing in one sentence.");
    }

}
