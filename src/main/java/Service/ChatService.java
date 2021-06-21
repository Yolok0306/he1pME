package Service;

import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.time.Instant;

public class ChatService extends MainService {

    protected void replyMessageEmbed(final MessageChannel channel) {
        final String img = getJsonValue(IMAGE, "BlueHead");
        final String yt = getJsonValue(YOUTUBECHANNEL, "Xun");
        if (img.length() > 0 && yt.length() > 0) {
            channel.createEmbed(spec -> spec
                    .setColor(Color.of(0, 255, 127))
                    .setAuthor("setAuthor", yt, img)
                    .setImage(img)
                    .setTitle("setTitle/setUrl")
                    .setUrl(yt)
                    .setDescription("setDescription\nbig D: is setImage\nsmall D: is setThumbnail\n<-- setColor")
                    .addField("addField", "inline = true", true)
                    .addField("addFIeld", "inline = true", true)
                    .addField("addFile", "inline = false", false)
                    .setThumbnail(img)
                    .setFooter("setFooter --> setTimestamp", img)
                    .setTimestamp(Instant.now())
            ).block();
        }
    }
}
