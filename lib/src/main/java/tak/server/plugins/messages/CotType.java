package tak.server.plugins.messages;

public enum CotType {
    HOSTILE_GROUND("a-h-G"),
    FRIENDLY_GROUND("a-f-G"),
    NEUTRAL_GROUND("a-n-G"),
    UNKNOWN_GROUND("a-u-G"),
    HOSTILE_AIR("a-h-A"),
    FRIENDLY_AIR("a-f-A"),
    NEUTRAL_AIR("a-n-A"),
    UNKNOWN_AIR("a-u-A");

    private final String type;

    CotType(String string) {
        this.type = string;
    }

    public String getType() {
        return type;
    }
    
}