package org.yolok.he1pME.service;

import net.dv8tion.jda.api.entities.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.util.CommonUtil;

import java.util.Objects;

@Service
public class MessageEventService {

    @Autowired
    private GoodBoyService goodBoyService;

    public void execute(Message message) {
        if (CommonUtil.isNotInstructionChannel(message.getChannel().getName())) {
            goodBoyService.checkContent(message);
        } else if (!message.getContentRaw().startsWith("!") && !Objects.requireNonNull(message.getAuthor()).isBot()) {
            message.delete().queue();
        }
    }
}
