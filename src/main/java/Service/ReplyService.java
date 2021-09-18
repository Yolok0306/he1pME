package Service;

import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.util.Optional;

public class ReplyService extends MainService {
    protected void replyByXunTemplate(final MessageChannel messageChannel, final String msg, final String img) {
        final Optional<String> id = getTokenFromDB("Xun");
        id.ifPresent(xun -> {
            messageChannel.createMessage("<@" + xun + "> " + msg).block();
            messageChannel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img)).block();
        });
    }

    protected void replyByDefaultTemplate(final MessageChannel messageChannel, final String id, final String msg, final String img) {
        messageChannel.createMessage("<@" + id + "> " + msg).block();
        messageChannel.createEmbed(spec -> spec.setColor(Color.BLUE).setImage(img)).block();
    }

    protected void replyByHe1pMETemplate(final MessageChannel messageChannel, final String msg) {
        messageChannel.createEmbed(spec -> {
            spec.setTitle(msg);
            spec.setColor(Color.of(255, 192, 203));
        }).block();
    }
}
