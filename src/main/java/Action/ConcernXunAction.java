package Action;

import Service.MainService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class ConcernXunAction extends MainService implements Action {
    @Override
    public String getAction() {
        return "concernXun";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        Optional.ofNullable(getUrlFromDB(IMAGE, "MikasaConcern")).ifPresent(image ->
                replyByXunTemplate(messageChannel, "主播人咧?", image));
    }
}
