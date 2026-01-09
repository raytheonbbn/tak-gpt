package tak.server.plugins.agent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.models.langchain4j.LangChain4j;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.Part;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.reactivex.rxjava3.core.Flowable;
import tak.server.plugins.LLMChatManager;
import tak.server.plugins.config.TAKContext;
import tak.server.plugins.messages.CotType;
import tak.server.plugins.messages.TAKMessageGenerator;

/**
 * An agent that uses Google's ADK to interact with LLMs. Tools are provided for some basic TAK interactions.
 */
public class GoogleAgentChatManager implements LLMChatManager {
    private Integer port = 11434;
	private String host = "127.0.0.1";
	private String modelName = "llama3.2:1b";
    private String systemPrompt;
    private String botName = "Agent TAK";
    private InMemoryRunner runner;
    private Session session;
    private static final TAKMessageGenerator MSG_GENERATOR = TAKMessageGenerator.getInstance();

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAgentChatManager.class);

    public GoogleAgentChatManager(Map<String, ? extends Object> config) {
        if(config.containsKey("modelPort")) {
			port = (Integer)config.get("modelPort");
		}
		if(config.containsKey("modelHost")) {
			host = (String)config.get("modelHost");
		}
		if(config.containsKey("modelName")) {
			modelName = (String)config.get("modelName");
		}
        if(config.containsKey("botName")) {
            botName = (String)config.get("botName");
        }

        systemPrompt = loadSystemPromptFromConfig(config);

        // ** user can configure the system prompt, but we also add instructions on how to respond to requests for tools
        systemPrompt = "You are a helpful local assistant. If you are asked to create a map marker, do so using the provided tool and if successful simply respond 'Marked added'" + (systemPrompt == null ? "" : systemPrompt);
        
        LOGGER.info("Will look for models at http://" + host + ":" + port);
		LOGGER.info("Will use model: " + modelName);

        OllamaChatModel model = OllamaChatModel.builder()
        .baseUrl("http://" + host + ":" + port)
		.modelName(modelName)
        .build();

        LlmAgent agent = LlmAgent.builder()
            .name(botName)
            .model(new LangChain4j(model))
            .instruction(systemPrompt)
            .tools(
                FunctionTool.create(GoogleAgentChatManager.class, "createMarker")
            )
            .build();
        
        runner = new InMemoryRunner(agent);

        session = runner.sessionService()
            .createSession(runner.appName(), "tak_user")
            .blockingGet();
    }

    /**
     * Example query: Create a map marker of type a-h-G with callsign foxtrot at latitude 42.382 and longitude -71.075
     * Example query: Create a map marker of type hostile ground with callsign foxtrot2 at latitude 42.382 and longitude -71.075
     * Example query: Create a hostile ground map marker with callsign foxtrot7 at latitude 42.382 and longitude -71.075
     */
    @Schema(name="create_tak_map_marker", description="Create a map marker in TAK. This is not for interacting with the Google Maps API, but for interacting with the TAK map which is unrelated to Google Maps. When successful, simply respond 'Marker added'")
    public static Map<String, String> createMarker(@Schema(name = "type",
                description = "The type of marker to create. Types are specified as strings of characters with dashes between them, for example a-h-G, or from this set of values: 'hostile ground', 'friendly ground', 'neutral ground', 'unknown ground', 'hostile air', 'friendly air', 'neutral air', 'unknown air'")
        String type,
        @Schema(name = "callsign",
                description = "The callsign of the marker to create. This can be any alphanumeric string")
        String callsign,
        @Schema(name = "lat",
                description = "The latitude of the marker to create. These values are specified as numeric values with one or more decimal places")
        String lat,
        @Schema(name = "lon",
                description = "The longitude of the marker to create. These values are specified as numeric values with one or more decimal places")
        String lon) {
            LOGGER.info("createMarker called");

            Map<String,String> map = new HashMap<>();

            String cotType;
            if(type.contains("-")) {
                cotType = type;
            } else {
                try {
                    CotType cotTypeValue = CotType.valueOf(type.toUpperCase().replace(" ", "_"));
                    cotType = cotTypeValue.getType();
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid marker type: " + type);
                    map.put("status", "failure");
                    map.put("report", "Invalid CotType: " + type);
                    return map;
                }
            }

            try {
                Message msg = MSG_GENERATOR.generateMarker(cotType, callsign, Float.valueOf(lat), Float.valueOf(lon), null);
                MSG_GENERATOR.send(msg);
                map.put("status", "success");
                map.put("report", "Created a marker of type: " + type + ", callsign: " + callsign + ", at lat: " + lat + ", lon: " + lon);
                LOGGER.info("Created new map marker message: " + msg.toString());
            } catch (DocumentException e) {
                LOGGER.error("Exception creating message to send", e);
                map.put("status", "failure");
                map.put("report", "Unable to create marker");
            }

        return  map;
    }

    @Override
    public String sendChatRequest(String messageText, TAKContext context) throws IOException {
        Content userMsg = Content.fromParts(Part.fromText(messageText));
        Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg);
        StringBuilder result = new StringBuilder();
        
        
        try {
            result.append(events.blockingLast().stringifyContent());
        } catch (Throwable t) {
            // ** if the above line fails, the exception doesn't always get to where it needs to be - so capturing here
            LOGGER.info("ERROR encountered: " + t.getMessage());
            result.append("Error generating response");
        }
        

        LOGGER.info("Completed interaction with agent. Response is: " + result.toString());
        return result.toString();
    }
}
