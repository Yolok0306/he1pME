package Service;

import Action.Action;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.codec.binary.StringUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class He1pMEService extends MainService {
    private final MusicService musicService = new MusicService();
    List<String> musicActions = musicService.getMusicMethod();
    Set<Class<? extends Action>> actions = new Reflections("Action").getSubTypesOf(Action.class);

    public void chat(final MessageCreateEvent event) {
        final String content = Optional.of(event.getMessage().getContent()).orElse("");
        if (checkSign(content)) {
            final String instruction = format(content);
            final Optional<List<String>> responsesOpt = getActionFromDB(instruction);
            responsesOpt.ifPresent(responses -> responses.forEach(response -> {
                if (musicActions.contains(response)) {
                    executeMusicAction(event, response);
                } else {
                    executeAction(event, response);
                }
            }));
        }

    }

    private void executeMusicAction(final MessageCreateEvent event, final String response) {
        try {
            final Method method = musicService.getClass().getDeclaredMethod(response, MessageCreateEvent.class);
            method.invoke(musicService, event);
        } catch (final NoSuchMethodException noSuchMethodException) {
            final Method method;
            try {
                method = musicService.getClass().getDeclaredMethod(response);
                method.invoke(musicService);
            } catch (final NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
                exception.printStackTrace();
            }
        } catch (final InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    private void executeAction(final MessageCreateEvent event, final String response) {
        actions.stream()
                .filter(action -> {
                    try {
                        return StringUtils.equals(response, action.getDeclaredConstructor().newInstance().getAction());
                    } catch (final InstantiationException | IllegalAccessException | InvocationTargetException
                            | NoSuchMethodException exception) {
                        exception.printStackTrace();
                    }
                    return false;
                }).forEach(action -> {
                    try {
                        action.getDeclaredConstructor().newInstance().execute(event);
                    } catch (final InstantiationException | IllegalAccessException | InvocationTargetException
                            | NoSuchMethodException exception) {
                        exception.printStackTrace();
                    }
                });
    }
}
