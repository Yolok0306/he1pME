package Action;

import Service.MainService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class ConcernYueAction extends MainService implements Action {
    @Override
    public String getAction() {
        return "concernYue";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        Optional.ofNullable(getTokenFromDB("Yue")).ifPresent(Yue ->
                Optional.ofNullable(getUrlFromDB(IMAGE, "AngryAqua")).ifPresent(img ->
                        replyByDefaultTemplate(messageChannel, Yue, "醒了嗎?", img)));
    }
}
