package action;

import annotation.help;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import util.CommonUtil;

@Slf4j
@help(example = "deleteMessage [number]", description = "清除多筆文字頻道的訊息")
public class DeleteMessageAction implements Action {
    @Override
    public String getInstruction() {
        return "deleteMessage";
    }

    @Override
    public void execute(final Message message) {
        final String regex = String.format("\\%s%s\\p{Blank}*", CommonUtil.SIGN, getInstruction());
        final String userNumber = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        try {
            final int number = Integer.parseInt(userNumber);
            message.getChannel().getHistoryBefore(message, number).complete().getRetrievedHistory()
                    .forEach(previousMessage -> message.getChannel().deleteMessageById(previousMessage.getId()).queue());
        } catch (final IllegalArgumentException numberFormatException) {
            log.error("\"{}\" cannot be executed because number = {}!", message.getContentRaw(), userNumber);
        }
        message.delete().queue();
    }
}