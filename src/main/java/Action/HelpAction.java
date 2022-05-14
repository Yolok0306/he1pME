package Action;

import Annotation.help;
import Service.MusicService;
import Util.CommonUtil;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@help(example = "help", description = "查看全部指令")
public class HelpAction implements Action {
    @Override
    public String getInstruction() {
        return "help";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final Set<Class<? extends Action>> actionSet = new Reflections("Action").getSubTypesOf(Action.class);
        if (CollectionUtils.isNotEmpty(actionSet)) {
            final String title = "一般指令";
            final String desc = actionSet.stream()
                    .filter(Objects::nonNull)
                    .filter(action -> action.isAnnotationPresent(help.class))
                    .sorted(Comparator.comparing(action -> {
                        try {
                            return action.getDeclaredConstructor().newInstance().getInstruction();
                        } catch (final Exception exception) {
                            exception.printStackTrace();
                        }
                        return StringUtils.EMPTY;
                    }))
                    .map(action -> action.getAnnotation(help.class))
                    .map(help -> CommonUtil.SIGN + help.example() + StringUtils.SPACE + help.description())
                    .collect(Collectors.joining(StringUtils.LF));
            CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
        }

        final Set<Method> musicMethodSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(help.class)).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(musicMethodSet)) {
            final String title = "音樂指令";
            final String desc = musicMethodSet.stream()
                    .filter(Objects::nonNull)
                    .filter(musicMethod -> musicMethod.isAnnotationPresent(help.class))
                    .sorted(Comparator.comparing(Method::getName))
                    .map(musicMethod -> musicMethod.getAnnotation(help.class))
                    .map(help -> CommonUtil.SIGN + help.example() + StringUtils.SPACE + help.description())
                    .collect(Collectors.joining(StringUtils.LF));
            CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
        }

        final Map<String, String> callActionMap = getCallActionMap();
        if (CollectionUtils.isNotEmpty(callActionMap.entrySet())) {
            final String title = "客製化指令";
            final String desc = callActionMap.entrySet().stream()
                    .filter(Objects::nonNull)
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> CommonUtil.SIGN + entry.getKey() + StringUtils.SPACE + entry.getValue())
                    .map(CommonUtil::descFormat)
                    .collect(Collectors.joining(StringUtils.LF));
            CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
        }
    }

    private Map<String, String> getCallActionMap() {
        final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().build());
        final ItemCollection<ScanOutcome> items = dynamoDB.getTable("CallAction").scan();

        final Map<String, String> callActionNameMap = new HashMap<>();
        if (items.iterator().hasNext()) {
            items.iterator().forEachRemaining(item ->
                    callActionNameMap.put(item.getString("action"), item.getString("description")));
        } else {
            log.info("There is no data in the CallAction table");
        }

        dynamoDB.shutdown();
        return callActionNameMap;
    }
}
