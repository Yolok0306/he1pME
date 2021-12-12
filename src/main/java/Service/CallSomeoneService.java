package Service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;

public class CallSomeoneService extends CommonService {

    protected void CallSomeone(final MessageCreateEvent event, final String instruction) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
    }
}
