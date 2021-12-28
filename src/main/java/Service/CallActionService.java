package Service;

import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.*;

public class CallActionService extends CommonService {
    private final Table table = dynamoDB.getTable("CallAction");

    protected Set<String> getAllCallAction() {
        final ScanSpec scanSpec = new ScanSpec();
        final ItemCollection<ScanOutcome> items = table.scan(scanSpec);
        final Set<String> result = new HashSet<>();
        items.forEach(item -> result.add(item.getString("action")));
        return result;
    }

    protected void callAction(final MessageCreateEvent event, final String instruction) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final CallAction callAction = getCallAction(instruction);
        reply(messageChannel, callAction.id, callAction.message, callAction.image, callAction.color);
    }

    private CallAction getCallAction(final String searchValue) {
        final Map<String, String> nameMap = Collections.singletonMap("#key", "action");
        final Map<String, Object> valueMap = Collections.singletonMap(":value", searchValue);
        final QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#key = :value")
                .withNameMap(nameMap).withValueMap(valueMap);
        final ItemCollection<QueryOutcome> items = table.query(querySpec);
        final CallAction result = new CallAction();
        items.forEach(item -> {
            result.id = getIdFromDB(item.getString("name"));
            result.message = item.getString("message");
            result.image = getUrlFromDB(item.getString("image"), IMAGE);
            final String[] colorArray = item.getString("color").split(", ");
            final int red = Integer.parseInt(colorArray[0]);
            final int green = Integer.parseInt(colorArray[1]);
            final int blue = Integer.parseInt(colorArray[2]);
            result.color = Color.of(red, green, blue);
        });
        return result;
    }

    private void reply(final MessageChannel messageChannel, final String id, final String msg, final String img, final Color color) {
        messageChannel.createMessage("<@" + id + "> " + msg).block();
        messageChannel.createMessage(EmbedCreateSpec.create().withColor(color).withImage(img)).block();
    }

    private static class CallAction {
        String id;
        String message;
        String image;
        Color color;
    }
}
