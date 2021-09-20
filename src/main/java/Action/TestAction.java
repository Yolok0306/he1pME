package Action;

import Service.MainService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.util.Objects;
import java.util.Optional;

public class TestAction extends MainService implements Action {
    @Override
    public String getAction() {
        return "test";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> id = getTokenFromDB("Yolok");
        final Optional<String> img = getUrlFromDB(IMAGE, "BlueHead");
        if (id.isPresent() && img.isPresent()) {
            channel.createMessage("<@" + id.get() + "> 456").block();
            channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 255)).setImage(img.get())).block();
        }
    }
}
