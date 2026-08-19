package org.yolok.he1pME.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.action.Action;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.service.CallActionService;
import org.yolok.he1pME.service.MessageEventService;
import org.yolok.he1pME.service.MusicService;
import org.yolok.he1pME.service.SlashCommandEventService;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JDAEventListener extends ListenerAdapter {

    private final ApplicationContext applicationContext;

    private final CallActionService callActionService;

    private final SlashCommandEventService slashCommandEventService;

    private final MessageEventService messageEventService;

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<SlashCommandData> slashCommandDataList = new ArrayList<>();
        
        Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(He1pME.class))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getAnnotation(He1pME.class))
                .forEach(he1pME -> {
                    SlashCommandData slashCommandData = Commands.slash(he1pME.instruction(), he1pME.description());
                    slashCommandData.setNSFW(he1pME.nsfw());
                    slashCommandData.setContexts(InteractionContextType.GUILD);
                    Arrays.stream(he1pME.options()).forEach(option -> {
                        addOption(slashCommandData, option);
                    });
                    slashCommandDataList.add(slashCommandData);
                });

        applicationContext.getBeansOfType(Action.class).values().stream()
                .filter(value -> value.getClass().isAnnotationPresent(He1pME.class))
                .forEach(action -> {
                    He1pME he1pME = action.getClass().getAnnotation(He1pME.class);
                    SlashCommandData slashCommandData = Commands.slash(he1pME.instruction(), he1pME.description());
                    slashCommandData.setNSFW(he1pME.nsfw());
                    Arrays.stream(he1pME.options()).forEach(option -> {
                        addOption(slashCommandData, option);
                    });
                    slashCommandDataList.add(slashCommandData);
                });
        
        event.getJDA().updateCommands().addCommands(slashCommandDataList).queue();
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        String guildId = event.getGuild().getId();
        List<SlashCommandData> slashCommandDataList = callActionService.getCallActionList(guildId).stream()
                .map(callAction -> {
                    return Commands.slash(callAction.getAction(), callAction.getDescription());
                })
                .toList();
        event.getGuild().updateCommands().addCommands(slashCommandDataList).queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        slashCommandEventService.execute(event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        messageEventService.execute(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        messageEventService.execute(event.getMessage());
    }

    private void addOption(SlashCommandData slashCommandData, He1pME.Option option) {
        slashCommandData.addOption(option.optionType(), option.name(), option.description(), option.required());
    }
}
