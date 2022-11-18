package entity;

import lombok.Builder;
import lombok.Getter;

import java.awt.*;

@Getter
@Builder
public class CallAction {
    private String id;
    private String message;
    private String image;
    private Color color;
}
