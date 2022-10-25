package action;

import annotation.help;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import service.MusicService;
import util.CommonUtil;

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
        final Set<MessageEmbed> messageEmbedSet = new HashSet<>();
        final Set<Class<? extends Action>> actionSet = new Reflections("action").getSubTypesOf(Action.class);
        if (CollectionUtils.isNotEmpty(actionSet)) {
            final EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("一般指令");
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
                    .forEach(help -> embedBuilder.addField(CommonUtil.SIGN + help.example(), help.description(), Boolean.FALSE));
            embedBuilder.setColor(CommonUtil.HE1PME_COLOR).setAuthor(member.getUser().getAsTag(), null, CommonUtil.getRealAvatarUrl(member));
            messageEmbedSet.add(embedBuilder.build());
        }

        final Set<Method> musicMethodSet = Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(help.class)).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(musicMethodSet)) {
            final EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("音樂指令");
            musicMethodSet.stream()
                    .filter(Objects::nonNull)
                    .filter(musicMethod -> musicMethod.isAnnotationPresent(help.class))
                    .sorted(Comparator.comparing(Method::getName))
                    .map(musicMethod -> musicMethod.getAnnotation(help.class))
                    .forEach(help -> embedBuilder.addField(CommonUtil.SIGN + help.example(), help.description(), Boolean.FALSE));
            embedBuilder.setColor(CommonUtil.HE1PME_COLOR).setAuthor(member.getUser().getAsTag(), null, CommonUtil.getRealAvatarUrl(member));
            messageEmbedSet.add(embedBuilder.build());
        }

        final Map<String, String> callActionMap = getCallActionMap();
        if (CollectionUtils.isNotEmpty(callActionMap.entrySet())) {
            final EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("客製化指令");
            callActionMap.entrySet().stream()
                    .filter(Objects::nonNull)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> embedBuilder.addField(CommonUtil.SIGN + entry.getKey(), entry.getValue(), Boolean.FALSE));
            embedBuilder.setColor(CommonUtil.HE1PME_COLOR).setAuthor(member.getUser().getAsTag(), null, CommonUtil.getRealAvatarUrl(member));
            messageEmbedSet.add(embedBuilder.build());
        }
        messageChannel.sendMessageEmbeds(messageEmbedSet).queue();
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
