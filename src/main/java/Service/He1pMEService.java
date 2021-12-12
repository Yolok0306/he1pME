package Service;

import Action.Action;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class He1pMEService extends CommonService {
    private final String SIGN = "/";
    private final List<String> chatRoomIdList = new ArrayList<>(getAllowChatRoom());
    private final MusicService musicService = new MusicService();
    private final List<String> musicActionList = Arrays.stream(MusicService.class.getDeclaredMethods()).filter(method ->
            method.getModifiers() == Modifier.PROTECTED).map(Method::getName).collect(Collectors.toList());
    private final List<Class<? extends Action>> actionList =
            new ArrayList<Class<? extends Action>>(new Reflections("Action").getSubTypesOf(Action.class));

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
            executeAction(event, instruction);
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

    private void executeAction(final MessageCreateEvent event, final String instruction) {
        if (CollectionUtils.isEmpty(actionList)) {
            return;
        }

        final Optional<Class<? extends Action>> actionOpt = actionList.stream().filter(action -> {
            try {
                return StringUtils.equals(instruction, action.getDeclaredConstructor().newInstance().getInstruction());
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                exception.printStackTrace();
            }
            return false;
        }).findFirst();

        if (actionOpt.isPresent()) {
            final Class<? extends Action> action = actionOpt.get();
            try {
                action.getDeclaredConstructor().newInstance().execute(event);
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                exception.printStackTrace();
            }
        } else {
            final CallSomeoneService callSomeoneService = new CallSomeoneService();
            callSomeoneService.CallSomeone(event, instruction);
        }
    }
}
