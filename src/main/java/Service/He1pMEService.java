package Service;

import Action.Action;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class He1pMEService extends MainService {
    private final String SIGN = "/";
    private final List<String> chatRoomIdList = new ArrayList<>(getActionFromDB("getChatRoomId"));
    private final MusicService musicService = new MusicService();
    private final List<String> musicActionList;
    Set<Class<? extends Action>> actionSet = new Reflections("Action").getSubTypesOf(Action.class);

    public He1pMEService() {
        musicActionList = Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(method -> method.getModifiers() == Modifier.PROTECTED)
                .map(Method::getName)
                .collect(Collectors.toList());
    }

    public void receiveMessage(final MessageCreateEvent event) {
        final String channelId = Optional.of(event.getMessage().getChannelId().asString()).orElse("");
        final String content = Optional.of(event.getMessage().getContent()).orElse("");

        if (!chatRoomIdList.contains(channelId) || !content.startsWith(SIGN)) {
            return;
        }

        final String instruction = format(content);
        if (musicActionList.contains(instruction)) {
            executeMusicAction(event, instruction);
        } else {
            final List<String> responseList = getActionFromDB(instruction);
            if (CollectionUtils.isEmpty(responseList)) {
                return;
            }

            for (final String response : responseList) {
                executeAction(event, response);
            }
        }
    }

    private String format(final String content) {
        final String[] array = content.split(" ");
        final String instruction = array[0];
        return new StringBuilder(instruction).delete(0, SIGN.length()).toString();
    }

    private void executeMusicAction(final MessageCreateEvent event, final String response) {
        try {
            final Method method = musicService.getClass().getDeclaredMethod(response, MessageCreateEvent.class);
            method.invoke(musicService, event);
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            exception.printStackTrace();
        }
    }

    private void executeAction(final MessageCreateEvent event, final String response) {
        actionSet.stream().filter(action -> {
            try {
                return StringUtils.equals(response, action.getDeclaredConstructor().newInstance().getAction());
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                exception.printStackTrace();
            }
            return false;
        }).forEach(action -> {
            try {
                action.getDeclaredConstructor().newInstance().execute(event);
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                exception.printStackTrace();
            }
        });
    }
}
