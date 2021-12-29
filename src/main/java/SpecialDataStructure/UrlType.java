package SpecialDataStructure;

public enum UrlType {
    IMAGE("Image"),
    YOUTUBE("Youtube"),
    TWITCH("Twitch");

    private final String type;

    UrlType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
