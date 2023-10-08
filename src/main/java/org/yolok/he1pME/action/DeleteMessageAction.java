package org.yolok.he1pME.action;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.util.CommonUtil;

import java.util.List;

@Slf4j
@Component
@Help(example = "deleteMessage [number]", description = "清除多筆文字頻道的訊息")
public class DeleteMessageAction implements Action {
    @Override
    public String getInstruction() {
        return "deleteMessage";
    }

    @Override
    public void execute(Message message) {
        String regex = String.format("\\%s%s\\p{Blank}*", CommonUtil.SIGN, getInstruction());
        String userNumber = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        try {
            int number = Integer.parseInt(userNumber);
            List<Message> messageList = message.getChannel().getHistoryBefore(message, number).complete().getRetrievedHistory();
            message.getChannel().asTextChannel().deleteMessages(messageList).queue();
        } catch (Exception e) {
            log.error("Cannot execute {}, number = {}", message.getContentRaw(), userNumber, e);
        }

        try {
            message.delete().queue();
        } catch (Exception e) {
            log.error("Deleting message: {} failed", message.getContentRaw(), e);
        }
    }
}
