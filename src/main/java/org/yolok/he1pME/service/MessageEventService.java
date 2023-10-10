package org.yolok.he1pME.service;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MessageEventService {

    @Autowired
    private GoodBoyService goodBoyService;

    public void execute(Message message) {
        if (isNotInstructionChannel(message.getChannel())) {
            goodBoyService.checkContent(message);
        } else if (!message.getContentRaw().startsWith("!") && !Objects.requireNonNull(message.getAuthor()).isBot()) {
            message.delete().queue();
        }
    }

    private boolean isNotInstructionChannel(MessageChannel messageChannel) {
        String messageChannelName = messageChannel.getName();
        return !StringUtils.contains(messageChannelName, "指令") &&
                !StringUtils.containsIgnoreCase(messageChannelName, "instruction");
    }
}
