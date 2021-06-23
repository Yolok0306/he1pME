package Service;

import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExtraService extends MainService {
    protected void replyMessageEmbed(final MessageChannel channel) {
        channel.createMessage("<@" + getPersonalInfo("Yolok") + "> 又再玩糞Game?").block();
        channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(getJsonValue(IMAGE, "Rushia"))).block();
    }

    protected void getCurrentTime(final MessageChannel channel) {
        channel.createMessage(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).block();
    }
}
