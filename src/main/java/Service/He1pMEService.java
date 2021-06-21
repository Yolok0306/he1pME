package Service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class He1pMEService extends MainService {
    public void chat(final MessageCreateEvent event) {
        final String content = Optional.of(event.getMessage().getContent()).orElse("");
        if (checkSign(content)) {
            final String instruction = format(content);
            final MessageChannel channel = event.getMessage().getChannel().block();
            if (getJsonValue(instruction).length > 0) {
                final String[] response = getJsonValue(instruction);
                if (response[0].equals(REPLYTEXT)) {
                    channel.createMessage(response[1]).block();
                } else {
                    runMethodByName(channel, response);
                }
            }
        }
    }

    private void runMethodByName(final MessageChannel channel, final String[] response) {
        final ChatService obj = new ChatService();
        try {
            final Method method = obj.getClass().getDeclaredMethod(response[0], MessageChannel.class);
            method.invoke(obj, channel);
        } catch (final InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
