package org.yolok.he1pME.service;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageEventService {

    @Value("${sign}")
    private String sign;

    private final GoodBoyService goodBoyService;

    public void execute(Message message) {
        if (StringUtils.startsWith(message.getContentRaw(), sign)) {
            return;
        }

        goodBoyService.checkContent(message);
    }
}
