package Action;

import Annotation.help;
import Util.CommonUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

@help(example = "deleteMessage [number]", description = "清除多筆文字頻道的訊息")
public class DeleteMessageAction implements Action {
    @Override
    public String getInstruction() {
        return "deleteMessage";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final String regex = "\\" + CommonUtil.SIGN + getInstruction() + "\\p{Blank}*";
        final String userNumber = event.getMessage().getContent().replaceAll(regex, StringUtils.EMPTY);
        final int number = StringUtils.isNotBlank(userNumber) ? Integer.parseInt(userNumber) : 0;
        if (number < 1) {
            return;
        }

        event.getMessage().getChannel().subscribe(messageChannel ->
                messageChannel.getMessagesBefore(Snowflake.of(Instant.now())).collectList().subscribe(messageList ->
                        messageList.stream().limit(number).forEach(message -> message.delete().block())));
    }
}
