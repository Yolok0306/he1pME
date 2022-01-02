package Service;

import Action.Action;
import Execute.He1pME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class He1pMEService {
    private final String SIGN = "$";
    private final Set<String> chatRoomIdSet = new HashSet<>();
    private final Set<String> musicActionSet = new HashSet<>();
    private final Map<String, Class<? extends Action>> actionMap = new HashMap<>();

    public He1pMEService() {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("ServerData").scan(scanSpec);
        items.forEach(item -> {
            if (StringUtils.equals(item.getString("name"), "AllowChatRoom")) {
                chatRoomIdSet.add(item.getString("id"));
            } else if (StringUtils.equals(item.getString("name"), "Token")) {
                He1pME.token = item.getString("id");
            }
        });
        dynamoDB.shutdown();

        musicActionSet.addAll(Arrays.stream(MusicService.class.getDeclaredMethods()).filter(method ->
                method.getModifiers() == Modifier.PROTECTED).map(Method::getName).collect(Collectors.toSet()));

        actionMap.putAll(new Reflections("Action").getSubTypesOf(Action.class).stream()
                .collect(Collectors.toMap(action -> {
                    try {
                        return action.getDeclaredConstructor().newInstance().getInstruction();
                    } catch (final Exception exception) {
                        exception.printStackTrace();
                    }
                    return null;
                }, Function.identity(), (existing, replacement) -> existing)));
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
        } else if (actionMap.containsKey(instruction)) {
            executeAction(event, instruction);
        } else {
            new CallActionService().callAction(event, instruction);
        }
    }

    private String format(final String content) {
        final String[] array = content.split(" ");
        final String instruction = array[0];
        return new StringBuilder(instruction).delete(0, SIGN.length()).toString();
    }

    private void executeMusicAction(final MessageCreateEvent event, final String response) {
        try {
            final Method method = MusicService.class.getDeclaredMethod(response, MessageCreateEvent.class);
            method.invoke(new MusicService(), event);
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    private void executeAction(final MessageCreateEvent event, final String instruction) {
        final Class<? extends Action> action = actionMap.get(instruction);
        try {
            action.getDeclaredConstructor().newInstance().execute(event);
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }
}
