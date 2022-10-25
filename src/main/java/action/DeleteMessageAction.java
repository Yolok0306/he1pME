package action;

import annotation.help;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import util.CommonUtil;

@help(example = "deleteMessage [number]", description = "清除多筆文字頻道的訊息")
public class DeleteMessageAction implements Action {
    @Override
    public String getInstruction() {
        return "deleteMessage";
    }

    @Override
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        final String regex = "\\" + CommonUtil.SIGN + getInstruction() + "\\p{Blank}*";
        final String userNumber = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        final int number = Math.max(Integer.parseInt(userNumber), 0);
        messageChannel.getHistoryBefore(message, number).complete().getRetrievedHistory()
                .forEach(previousMessage -> messageChannel.deleteMessageById(previousMessage.getId()).queue());
        messageChannel.deleteMessageById(message.getId()).queue();
    }
}
