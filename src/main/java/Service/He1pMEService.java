package Service;

import discord4j.core.event.domain.message.MessageCreateEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class He1pMEService extends MainService {
    private final ExtraService extraService = new ExtraService();

    public void chat(final MessageCreateEvent event) {
        final String content = Optional.of(event.getMessage().getContent()).orElse("");
        if (checkSign(content)) {
            final String instruction = format(content);
            final Optional<List<String>> response = getActionFromDB(instruction);
            if (response.isPresent()) {
                for (final String text : response.get()) {
                    runMethodByName(event, text);
                }
            }
        }
    }

    private void runMethodByName(final MessageCreateEvent event, final String text) {
        try {
            final Method method = extraService.getClass().getDeclaredMethod(text, MessageCreateEvent.class);
            method.invoke(extraService, event);
        } catch (final NoSuchMethodException noSuchMethodException) {
            Objects.requireNonNull(event.getMessage().getChannel().block()).createMessage(text).block();
        } catch (final InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }
}
