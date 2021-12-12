package Action;

import Service.CommonService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;
import java.util.Optional;

public class CallXianAction extends CommonService implements Action {
    @Override
    public String getInstruction() {
        return "callXian";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        Optional.ofNullable(getUrlFromDB("NeNeShake", IMAGE)).ifPresent(image ->
                replyByXianTemplate(messageChannel, "集合!!!", image));
    }
}
