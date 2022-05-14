package Entity;

import discord4j.rest.util.Color;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CallAction {
    private String id;
    private String message;
    private String image;
    private Color color;
}
