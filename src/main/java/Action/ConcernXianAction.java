package Action;

import Service.ReplyService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class ConcernXianAction extends ReplyService implements Action {
    @Override
    public String getAction() {
        return "concernXian";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getUrlFromDB(IMAGE, "NeNeYa");
        img.ifPresent(image -> replyByXianTemplate(messageChannel, "記得交心得喔~", image));
    }
}