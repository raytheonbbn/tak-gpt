package tak.server.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.plugins.config.TAKContext;

/**
 * A general interface for an agent/bot that responds to chat messages
 */
public interface LLMChatManager {
	static final Logger LOGGER = LoggerFactory.getLogger(LLMChatManager.class);

	/**
	 * Called when a chat message is sent to the specific agent/bot. The implementer should respond with an appropriate message.
	 * @param messageText The text of the chat message
	 * @param context Some context (e.g., location, callsign of sender) that can be used to customize the response
	 * @return A response to the message received
	 * @throws IOException
	 */
	public String sendChatRequest(String messageText, TAKContext context) throws IOException;
	
	/**
	 * A utility function to load a system prompt from the config file or the file system
	 * This function looks for a config property called systemPrompt. If present it will
	 * use that value as the system prompt. Otherwise, it will look for a config property
	 * called systemPromptFilePath and load the system prompt from that file.
	 * @param config The plugin's loaded configuration map
	 * @return The system prompt or null if none supplied
	 */
	default String loadSystemPromptFromConfig(Map<String, ? extends Object> config) {
        String systemPrompt = null;

		if(config.containsKey("systemPrompt")) {
			systemPrompt = (String)config.get("systemPrompt");
		} else if (config.containsKey("systemPromptFilePath")) {
			String systemPromptFilePath = (String)config.get("systemPromptFilePath");
            try {
                systemPrompt = Files.readString(Paths.get(systemPromptFilePath));
            } catch (Exception e) {
                LOGGER.error("Unable to load system prompt. Using no system prompt!", e);
            }
		}
        LOGGER.info("Using system prompt: " + systemPrompt);

		return systemPrompt;
    }
}
