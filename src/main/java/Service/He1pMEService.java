package Service;

import Action.Action;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class He1pMEService extends CommonService {
    private final String SIGN = "$";
    private final MusicService musicService = new MusicService();
    private final CallActionService callActionService = new CallActionService();
    private final Set<String> chatRoomIdSet = getAllowChatRoom();
    private final Set<String> musicActionSet = Arrays.stream(MusicService.class.getDeclaredMethods()).filter(method ->
            method.getModifiers() == Modifier.PROTECTED).map(Method::getName).collect(Collectors.toSet());
    private final Set<String> callActionSet = callActionService.getAllCallAction();
    private final Set<Class<? extends Action>> actionSet = new Reflections("Action").getSubTypesOf(Action.class);

    private Set<String> getAllowChatRoom() {
        final Map<String, String> nameMap = Collections.singletonMap("#key", "name");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", "AllowChatRoom");
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = dynamoDB.getTable("AllOfId").query(querySpec);
        final Set<String> result = new HashSet<>();
        items.forEach(item -> result.add(item.getString("id")));
        return result;
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
