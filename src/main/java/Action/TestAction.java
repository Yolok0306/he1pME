package Action;

import Service.CommonService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class TestAction extends CommonService implements Action {
    @Override
    public String getInstruction() {
        return "123";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        Optional.ofNullable(getIdFromDB("Yolok")).ifPresent(Yolok ->
                Optional.ofNullable(getUrlFromDB("BlueHead", IMAGE)).ifPresent(img ->
                        replyByDefaultTemplate(messageChannel, Yolok, "456", img)));
    }
}
