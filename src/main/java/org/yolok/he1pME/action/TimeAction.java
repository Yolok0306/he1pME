package org.yolok.he1pME.action;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@He1pME(instruction = "time", description = "取得現在時間",
        options = {
                @He1pME.Option(name = "zone-id", description = "zone id", required = false)
        }, example = "time [zone-id]")
public class TimeAction implements Action {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("zone-id");
        ZoneId zoneId = option == null ? ZoneId.systemDefault() : ZoneId.of(option.getAsString());
        String content = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(zoneId));
        event.reply(content).setEphemeral(true).queue();
    }
}
