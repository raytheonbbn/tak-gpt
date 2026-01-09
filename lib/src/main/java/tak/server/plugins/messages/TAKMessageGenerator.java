package tak.server.plugins.messages;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import tak.server.plugins.MessageSender;
import tak.server.plugins.MessageSenderReceiverBase;
import tak.server.plugins.TAKChatBotBase;
import tak.server.plugins.messaging.MessageConverter;

public class TAKMessageGenerator {
    private static final TAKMessageGenerator INSTANCE = new TAKMessageGenerator();
    private static final Logger LOGGER = LoggerFactory.getLogger(TAKMessageGenerator.class);
    private static MessageSenderReceiverBase messageSenderReceiverBase;
    private static final String TEMPLATE = """
        <event
            version=\"2.0\"
            uid=\"|||MARKER_UID|||\"
            type=\"|||TYPE|||\"
            how=\"m-g\"
            time=\"|||TIME|||\"
            start=\"|||TIME|||\"
            stale=\"|||STALE|||\">
                <point  lat=\"|||LAT|||\"
                        lon=\"|||LON|||\"
                        hae=\"999999.0\"
                        ce=\"999999.0\"
                        le=\"999999.0\"/>
                <detail>
                    <contact
                        callsign=\"|||MARKER_NAME|||\"/>
                    <takv
                        device=\"Chrome - 143\"
                        platform=\"WebTAK\"
                        os=\"Windows - 10\"
                        version=\"4.10.3\"/>
                    <archive/>
                    <usericon
                        iconsetpath=\"COT_MAPPING_2525B/\"/>
                </detail>
        </event>""";
            
    private static MessageConverter converter;

    private TAKMessageGenerator() {
        
    }
    public static TAKMessageGenerator getInstance() {
        return INSTANCE;
    }   

    public void init(MessageConverter converter, MessageSenderReceiverBase messageSenderReceiverBase) {
        INSTANCE.setMessageConverter(converter);
        INSTANCE.setSender(messageSenderReceiverBase);
        LOGGER.info("Initialized TAK Message Generator with: " + converter);
    }
    private void setSender(MessageSenderReceiverBase messageSenderReceiverBase) {
        this.messageSenderReceiverBase = messageSenderReceiverBase;
    }
    private void setMessageConverter(MessageConverter converter) {
        this.converter = converter;
    }

    public void send(Message msg) {
        messageSenderReceiverBase.send(msg);
    }
    
    public Message generateMarker(String type, String name, Float lat, Float lon, Set<String> groups) throws DocumentException {
        SimpleDateFormat dateFormater = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
		Date now = new Date();
		String nowStr = dateFormater.format(now);
		
		Calendar calendar = Calendar.getInstance();
	    calendar.setTime(now);
	    calendar.add(Calendar.HOUR_OF_DAY, 12);
	    
	    String staleStr = dateFormater.format(calendar.getTime());

        String messageTemplate = TEMPLATE.replace("|||MARKER_UID|||", UUID.randomUUID().toString())
        .replace("|||TYPE|||", type)
        .replace("|||TIME|||", nowStr)
        .replace("|||STALE|||", staleStr)
        .replace("|||LAT|||", lat.toString())
        .replace("|||LON|||", lon.toString())
        .replace("|||MARKER_NAME|||", name);
        return converter.cotStringToDataMessage(messageTemplate, groups, Integer.toString(System.identityHashCode(this)));
        
    }
}
