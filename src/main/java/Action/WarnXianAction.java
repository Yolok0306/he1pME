package Action;

import Service.MainService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class WarnXianAction extends MainService implements Action {
    @Override
    public String getAction() {
        return "warnXian";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getUrlFromDB(IMAGE, "AngryNeNe");
        img.ifPresent(image -> replyByXianTemplate(messageChannel, "又想寫心得?", image));
    }
}
