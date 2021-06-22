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
                for (final String text : response) {
                    runMethodByName(channel, text);
                }
            }
        }
    }

    private void runMethodByName(final MessageChannel channel, final String text) {
        final ExtraService extraService = new ExtraService();
        try {
            final Method method = extraService.getClass().getDeclaredMethod(text, MessageChannel.class);
            method.invoke(extraService, channel);
        } catch (final NoSuchMethodException noSuchMethodException) {
            channel.createMessage(text).block();
        } catch (final InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }
}
