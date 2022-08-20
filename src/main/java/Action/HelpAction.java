package Action;

import Annotation.help;
import Service.MusicService;
import Util.CommonUtil;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
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
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        final EmbedCreateFields.Author author = EmbedCreateFields.Author.of(member.getTag(), StringUtils.EMPTY, member.getAvatarUrl());

        final Set<Class<? extends Action>> actionSet = new Reflections("Action").getSubTypesOf(Action.class);
        if (CollectionUtils.isNotEmpty(actionSet)) {
            final EmbedCreateSpec.Builder embedCreateSpecBuilder = EmbedCreateSpec.builder().title("一般指令");
            actionSet.stream()
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
                    .forEach(help -> embedCreateSpecBuilder.addField(CommonUtil.SIGN + help.example(), help.description(), Boolean.FALSE));
            messageChannel.createMessage(embedCreateSpecBuilder.color(CommonUtil.HE1PME_COLOR).author(author).build()).block();
        }

        final Set<Method> musicMethodSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(help.class)).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(musicMethodSet)) {
            final EmbedCreateSpec.Builder embedCreateSpecBuilder = EmbedCreateSpec.builder().title("音樂指令");
            musicMethodSet.stream()
                    .filter(Objects::nonNull)
                    .filter(musicMethod -> musicMethod.isAnnotationPresent(help.class))
                    .sorted(Comparator.comparing(Method::getName))
                    .map(musicMethod -> musicMethod.getAnnotation(help.class))
                    .forEach(help -> embedCreateSpecBuilder.addField(CommonUtil.SIGN + help.example(), help.description(), Boolean.FALSE));
            messageChannel.createMessage(embedCreateSpecBuilder.color(CommonUtil.HE1PME_COLOR).author(author).build()).block();
        }

        final Map<String, String> callActionMap = getCallActionMap();
        if (CollectionUtils.isNotEmpty(callActionMap.entrySet())) {
            final EmbedCreateSpec.Builder embedCreateSpecBuilder = EmbedCreateSpec.builder().title("客製化指令");
            callActionMap.entrySet().stream()
                    .filter(Objects::nonNull)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> embedCreateSpecBuilder.addField(CommonUtil.SIGN + entry.getKey(), entry.getValue(), Boolean.FALSE));
            messageChannel.createMessage(embedCreateSpecBuilder.color(CommonUtil.HE1PME_COLOR).author(author).build()).block();
        }
    }

    private Map<String, String> getCallActionMap() {
        final AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(CommonUtil.REGIONS)
                .withCredentials(new AWSStaticCredentialsProvider(CommonUtil.BASIC_AWS_CREDENTIALS)).build();
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
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
