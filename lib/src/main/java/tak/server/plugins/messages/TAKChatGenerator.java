package tak.server.plugins.messages;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.plugins.messaging.MessageConverter;

public class TAKChatGenerator {
	//private static final String CHAT_TEMPLATE = "<event version=\"2.0\" uid=\"GeoChat.|||UID|||\" type=\"b-t-f\" how=\"h-g-i-g-o\" time=\"|||TIME|||\" start=\"|||TIME|||\" stale=\"|||STALE|||\"><point lat=\"0.0\" lon=\"0.0\" hae=\"999999.0\" ce=\"999999.0\" le=\"999999.0\"/><detail><__chat parent="RootContactGroup" senderCallsign=\"TAKBot\" chatroom=\"|||DST_CALLSIGN|||\" id=\"|||ID|||\"><chatgrp id=\"|||ID|||\" uid1=\"|||UID1|||\" uid0=\"|||UID0|||\"/></__chat><remarks time=\"2024-02-07T05:02:41Z\" source=\"daf0e27b-1ba2-08db-992e-153a2c73ea4b\" to=\"1a677971-bfba-a731-f86b-64c2317f7097\">|||CHAT_TEXT|||</remarks><link relation=\"p-p\" type=\"a-f-G-U-C-I\" uid=\"TAKBot\"/><marti><dest callsign=\"|||DST_CALLSIGN|||\"/></marti></detail></event>";
	private static final String CHAT_TEMPLATE = "<event version=\"2.0\" uid=\"|||SRC_UID|||\" type=\"b-t-f\" how=\"h-g-i-g-o\" time=\"|||TIME|||\" start=\"|||TIME|||\" stale=\"|||STALE|||\"><point lat=\"0.0\" lon=\"0.0\" hae=\"999999.0\" ce=\"999999.0\" le=\"999999.0\"/><detail><__chat parent=\"RootContactGroup\" messageId=\"|||MSG_UID|||\" senderCallsign=\"|||SRC_CALLSIGN|||\" chatroom=\"|||SRC_CALLSIGN|||\" id=\"|||SRC_UID|||\"><chatgrp id=\"|||DST_UID|||\" uid1=\"|||DST_UID|||\" uid0=\"|||SRC_UID|||\"/></__chat><remarks time=\"|||TIME|||\" to=\"|||DST_CALLSIGN|||\">|||TEXT|||</remarks><link relation=\"p-p\" type=\"a-f-G-U-C-I\" uid=\"|||SRC_UID|||\"/><marti><dest callsign=\"|||DST_CALLSIGN|||\"/></marti></detail></event>";
	private static final Logger LOGGER = LoggerFactory.getLogger(TAKChatGenerator.class);
	
	public static Message generateChat(Message messageToReverse, String chatText, String botCallsign) throws Exception {
		Pattern senderCallsignPattern = Pattern.compile("senderCallsign=\"(.*?)\"");
		Pattern senderUIDPattern = Pattern.compile("uid0=\"(.*?)\"");
		
		String destCallsign;
		String dstUID;

		if(messageToReverse == null) {
			destCallsign = "All Chat Rooms";
			dstUID = "All Chat Rooms";
		} else {
			destCallsign = getSingleMatch(messageToReverse, senderCallsignPattern);//"BRAVO-123";;
			dstUID =  getSingleMatch(messageToReverse, senderUIDPattern);//"1a677971-bfba-a731-f86b-64c2317f7097";
		}

		String srcCallsign = botCallsign;
		String srcUID = botCallsign;
		
		SimpleDateFormat dateFormater = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
		Date now = new Date();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
	    calendar.setTime(now);
	    
	    // ** TODO fix this (something wrong with the time zone, so manually adding 10 hours for Hawaii to GMT
	    //calendar.add(Calendar.HOUR_OF_DAY, 10);

		String msgUID = UUID.randomUUID().toString();
	    
		String nowStr = dateFormater.format(calendar.getTime());
		//LOGGER.info("Updated to add 10 hours:" + nowStr);
		
		calendar.add(Calendar.HOUR_OF_DAY, 12);
	    
	    String staleStr = dateFormater.format(calendar.getTime());
		
		String newCoT = CHAT_TEMPLATE.replaceAll("\\|\\|\\|TEXT\\|\\|\\|", chatText.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
		newCoT = newCoT.replaceAll("\\|\\|\\|STALE\\|\\|\\|", staleStr);
		newCoT = newCoT.replaceAll("\\|\\|\\|TIME\\|\\|\\|", nowStr);
		newCoT = newCoT.replaceAll("\\|\\|\\|UID\\|\\|\\|", destCallsign);
		newCoT = newCoT.replaceAll("\\|\\|\\|SRC_UID\\|\\|\\|", srcUID);
		newCoT = newCoT.replaceAll("\\|\\|\\|DST_UID\\|\\|\\|", dstUID);
		newCoT = newCoT.replaceAll("\\|\\|\\|SRC_CALLSIGN\\|\\|\\|", srcCallsign);
		newCoT = newCoT.replaceAll("\\|\\|\\|DST_CALLSIGN\\|\\|\\|", destCallsign);
		newCoT = newCoT.replaceAll("\\|\\|\\|MSG_UID\\|\\|\\|", msgUID);
		// ** TODO fill the rest of this in - look for easier examples (most chat isn't this complex)
		
		LOGGER.info("\n\nChat Message Generated:\n" + newCoT + "\n\n");
		
		MessageConverter converter = new MessageConverter();
		Message newMsg = converter.cotStringToDataMessage(newCoT, new HashSet(Arrays.asList(messageToReverse.getGroupsList().toArray())), "TAKBot");
		return newMsg;
	}
	
	private static String getSingleMatch(Message message, Pattern p) {
		Matcher m = p.matcher(message.getPayload().getCotEvent().getDetail().getXmlDetail());
		if(m.find()) {
			return m.group(1);
		}
		
		return null;
	}
}
