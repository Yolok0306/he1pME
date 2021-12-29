package Service;

import Action.Action;
import Execute.He1pME;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class He1pMEService extends CommonService {
    private final String SIGN = "$";
    private final MusicService musicService = new MusicService();
    private final CallActionService callActionService = new CallActionService();
    private final Set<String> chatRoomIdSet = new HashSet<>();
    private final Set<String> callActionSet = new HashSet<>();
    private final Set<String> musicActionSet = Arrays.stream(MusicService.class.getDeclaredMethods()).filter(method ->
            method.getModifiers() == Modifier.PROTECTED).map(Method::getName).collect(Collectors.toSet());
    private final Set<Class<? extends Action>> actionSet = new Reflections("Action").getSubTypesOf(Action.class);

    public He1pMEService() {
        final ScanSpec scanSpec = new ScanSpec();
        ItemCollection<ScanOutcome> items;

        items = dynamoDB.getTable("ServerData").scan(scanSpec);
        items.forEach(item -> {
            if (StringUtils.equals(item.getString("name"), "AllowChatRoom")) {
                chatRoomIdSet.add(item.getString("id"));
            } else if (StringUtils.equals(item.getString("name"), "Token")) {
                He1pME.token = item.getString("id");
            }
        });

        items = dynamoDB.getTable("CallAction").scan(scanSpec);
        items.forEach(item -> callActionSet.add(item.getString("action")));
    }

    public void receiveMessage(final MessageCreateEvent event) {
        final String channelId = Optional.of(event.getMessage().getChannelId().asString()).orElse("");
        final String content = Optional.of(event.getMessage().getContent()).orElse("");

        if (!chatRoomIdSet.contains(channelId) || !content.startsWith(SIGN)) {
            return;
        }

        final String instruction = format(content);
        if (musicActionSet.contains(instruction)) {
            executeMusicAction(event, instruction);
        } else if (callActionSet.contains(instruction)) {
            callActionService.callAction(event, instruction);
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
        if (CollectionUtils.isEmpty(actionSet)) {
            return;
        }

        actionSet.stream().filter(action -> {
            try {
                return StringUtils.equals(instruction, action.getDeclaredConstructor().newInstance().getInstruction());
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                exception.printStackTrace();
            }
            return false;
        }).findFirst().ifPresent(action -> {
            try {
                action.getDeclaredConstructor().newInstance().execute(event);
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
                exception.printStackTrace();
            }
        });
    }
}
