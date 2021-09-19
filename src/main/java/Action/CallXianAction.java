package Action;

import Service.ReplyService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class CallXianAction extends ReplyService implements Action {
    @Override
    public String getAction() {
        return "callXian";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getUrlFromDB(IMAGE, "NeNeShake");
        img.ifPresent(image -> replyByXianTemplate(messageChannel, "集合!!!", image));
    }
}
