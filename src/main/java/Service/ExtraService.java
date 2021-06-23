package Service;

import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ExtraService extends MainService {
    protected void replyMessageEmbed(final MessageChannel channel) {
        final Optional<String> id = getId("Yolok");
        final Optional<String> img = getURL(IMAGE, "Rushia");
        if (id.isPresent() && img.isPresent()) {
            channel.createMessage("<@" + id.get() + "> 又再玩糞Game?").block();
            channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img.get())).block();
        }
    }

    protected void getCurrentTime(final MessageChannel channel) {
        channel.createMessage(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).block();
    }
}
