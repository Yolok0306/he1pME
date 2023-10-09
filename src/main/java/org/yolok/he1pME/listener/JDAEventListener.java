package org.yolok.he1pME.listener;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.service.CallActionService;
import org.yolok.he1pME.service.MessageEventService;
import org.yolok.he1pME.util.CommonUtil;

import java.util.List;
import java.util.Objects;

@Component
public class JDAEventListener extends ListenerAdapter {

    @Autowired
    private CallActionService callActionService;

    @Autowired
    private MessageEventService messageEventService;

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        String guildId = Objects.requireNonNull(event.getGuild()).getId();
        List<SlashCommandData> slashCommandDataList = callActionService.getSlashCommandDataList(guildId);
        CommonUtil.JDA.updateCommands().addCommands(slashCommandDataList).queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        callActionService.execute(event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        messageEventService.execute(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        messageEventService.execute(event.getMessage());
    }
}
