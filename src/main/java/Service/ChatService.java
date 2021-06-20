package Service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.time.Instant;
import java.util.Optional;

public class ChatService extends MainService {
    final String URL = "URL";
    final String IMAGE = "image";

    public void chat(final MessageCreateEvent event) {
        final String content = Optional.of(event.getMessage().getContent()).orElse("");
        if (checkSign(content)) {
            final String instruction = format(content);
            final MessageChannel channel = event.getMessage().getChannel().block();
            reply(instruction, channel);
        }
    }

    private void reply(final String instruction, final MessageChannel channel) {
        final String response = getJsonValue(instruction);
        if (response.equals("IMG&URL")) {
            getMessageEmbeds(channel);
        } else if (response.length() > 0) {
            channel.createMessage(response).block();
        }
    }

    private void getMessageEmbeds(final MessageChannel channel) {
        final String IMAGE_URL = getJsonValue(IMAGE, "IMAGE_URL");
        final String ANY_URL = getJsonValue(URL, "ANY_URL");
        if (IMAGE_URL.length() > 0 && ANY_URL.length() > 0) {
            channel.createEmbed(spec -> spec
                    .setColor(Color.of(0, 255, 127))
                    .setAuthor("setAuthor", ANY_URL, IMAGE_URL)
                    .setImage(IMAGE_URL)
                    .setTitle("setTitle/setUrl")
                    .setUrl(ANY_URL)
                    .setDescription("setDescription\nbig D: is setImage\nsmall D: is setThumbnail\n<-- setColor")
                    .addField("addField", "inline = true", true)
                    .addField("addFIeld", "inline = true", true)
                    .addField("addFile", "inline = false", false)
                    .setThumbnail(IMAGE_URL)
                    .setFooter("setFooter --> setTimestamp", IMAGE_URL)
                    .setTimestamp(Instant.now())
            ).block();
        }
    }
}
