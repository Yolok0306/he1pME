package Service;

import Action.Action;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.codec.binary.StringUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class He1pMEService extends MainService {
    private final MusicService musicService = new MusicService();
    Set<Class<? extends Action>> actions;

    public He1pMEService() {
        final Reflections reflections = new Reflections("Action");
        actions = reflections.getSubTypesOf(Action.class);
    }

    public void chat(final MessageCreateEvent event) {
        final String content = Optional.of(event.getMessage().getContent()).orElse("");
        if (checkSign(content)) {
            final String instruction = format(content);
            final Optional<List<String>> responsesOpt = getActionFromDB(instruction);
            responsesOpt.ifPresent(responses -> responses.forEach(response -> executeAction(event, response)));
        }
    }

    private void executeAction(final MessageCreateEvent event, final String response) {
        actions.stream()
                .filter(action -> {
                    try {
                        return StringUtils.equals(response, action.getDeclaredConstructor().newInstance().getAction());
                    } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    return false;
                }).forEach(action -> {
                    try {
                        action.getDeclaredConstructor().newInstance().execute(event);
                    } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                });
    }
}
