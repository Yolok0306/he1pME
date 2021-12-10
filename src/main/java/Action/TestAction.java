package Action;

import Service.MainService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class TestAction extends MainService implements Action {
    @Override
    public String getAction() {
        return "test";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        Optional.ofNullable(getTokenFromDB("Yolok")).ifPresent(Yolok ->
                Optional.ofNullable(getUrlFromDB(IMAGE, "BlueHead")).ifPresent(img ->
                        replyByDefaultTemplate(messageChannel, Yolok, "456", img)));
    }
}
