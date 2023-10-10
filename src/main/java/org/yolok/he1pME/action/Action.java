package org.yolok.he1pME.action;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface Action {

    void execute(SlashCommandInteractionEvent event);
}
