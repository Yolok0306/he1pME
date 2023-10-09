package org.yolok.he1pME.listener;

import jakarta.validation.constraints.NotNull;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.service.MessageEventService;

@Component
public class JDAEventListener extends ListenerAdapter {

    @Autowired
    private MessageEventService messageEventService;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        messageEventService.execute(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        messageEventService.execute(event.getMessage());
    }
}
