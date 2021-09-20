package Action;

import Service.MainService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class AskXunAction extends MainService implements Action {
    @Override
    public String getAction() {
        return "askXun";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getUrlFromDB(IMAGE, "RainbowAqua");
        img.ifPresent(image -> replyByXunTemplate(messageChannel, "打LOL嗎?", image));
    }
}
