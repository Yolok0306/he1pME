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
        final Optional<String> id = getTokenFromDB("Yue");
        final Optional<String> img = getUrlFromDB(IMAGE, "AngryAqua");
        if (id.isPresent() && img.isPresent()) {
            replyByDefaultTemplate(messageChannel, id.get(), "醒了嗎?", img.get());
        }
    }
}
