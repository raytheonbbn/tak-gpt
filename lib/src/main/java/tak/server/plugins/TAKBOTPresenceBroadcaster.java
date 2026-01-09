package tak.server.plugins;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.plugins.messaging.MessageConverter;

/**
 * Preiodically broadcast a-f-G-U-C-I messages for a given bot/agent
 */
public class TAKBOTPresenceBroadcaster {
	private static final Logger LOGGER = LoggerFactory.getLogger(TAKBOTPresenceBroadcaster.class);
	private static final String DEFAULT_GROUP_NAME = "__ANON__";
	private static final String COT_TEMPLATE = "<event version=\"2.0\" uid=\"|||BOTNAME|||\" type=\"a-f-G-U-C-I\" how=\"m-g\" time=\"|||TIME|||\" start=\"|||TIME|||\" stale=\"|||STALE|||\"><point lat=\"|||LAT|||\" lon=\"|||LON|||\" hae=\"999999.0\" ce=\"999999.0\" le=\"999999.0\"/><detail><contact callsign=\"|||BOTNAME|||\" endpoint=\"*:-1:stcp\"/><__group name=\"Cyan\" role=\"Team Member\"/><takv device=\"Server\" platform=\"TAK Server\" os=\"Linux - 0\" version=\"5.0\"/><link relation=\"p-p\" type=\"a-f-G-U-C-I\" uid=\"|||BOTNAME|||\"/></detail></event>";
	private final String botName;
	private Double latitude = 0.0;
	private Double longitude = 0.0;
	private final Set<String> groups = new HashSet<>();
	
	public static void main(String[] args) throws DocumentException {
		List<String> groups = new ArrayList<>();
		TAKBOTPresenceBroadcaster b = new TAKBOTPresenceBroadcaster("takGPT", 0.0, 0.0, groups);
		b.generatePresenceMessage();
	}

	/**
	 * Create a broadcaster for a given bot/agent
	 * @param botName The name of the bot.
	 * @param latitude The latitude of the bot (where it should appear on TAK users' maps).
	 * @param longitude The longitude of the bot  (where it should appear on TAK users' maps).
	 * @param groups The list of groups the bot is in (and thus the groups it will be available to).
	 */
	public TAKBOTPresenceBroadcaster(String botName, Double latitude, Double longitude, List<String> groups) {
		this.botName = botName;
		this.latitude = latitude;
		this.longitude = longitude;

		if(groups == null || groups.isEmpty()) {
			this.groups.add(DEFAULT_GROUP_NAME);
		} else {
			this.groups.addAll(groups);
		}
		LOGGER.info("BOT [" + botName + "] configured to use groups: " + this.groups);
	}

	/**
	 * Generate a presence message (aka a self report of location, aka a-f-G-U-C-I message).
	 */
	public Message generatePresenceMessage() throws DocumentException {
		SimpleDateFormat dateFormater = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
		Date now = new Date();
		String nowStr = dateFormater.format(now);
		
		Calendar calendar = Calendar.getInstance();
	    calendar.setTime(now);
	    calendar.add(Calendar.HOUR_OF_DAY, 12);
	    
	    String staleStr = dateFormater.format(calendar.getTime());
		
		String cot = COT_TEMPLATE.replaceAll("\\|\\|\\|TIME\\|\\|\\|", nowStr)
						.replaceAll("\\|\\|\\|STALE\\|\\|\\|", staleStr)
						.replaceAll("\\|\\|\\|BOTNAME\\|\\|\\|", botName)
						.replaceAll("\\|\\|\\|LAT\\|\\|\\|", latitude.toString())
						.replaceAll("\\|\\|\\|LON\\|\\|\\|", longitude.toString());
		
		LOGGER.info("\n\nPresence Message:\n" + cot + "\n\n");
		
		MessageConverter converter = new MessageConverter();
		Message presenceMsg = converter.cotStringToDataMessage(cot, this.groups, Integer.toString(System.identityHashCode(cot)));
		
		LOGGER.info("\n\nPresence Message as Message:" + presenceMsg.toString() + "\n\n");
		
		return presenceMsg;
	}
}
