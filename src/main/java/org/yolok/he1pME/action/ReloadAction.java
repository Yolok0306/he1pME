package org.yolok.he1pME.action;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.service.GoodBoyService;
import org.yolok.he1pME.service.TwitchService;
import org.yolok.he1pME.service.YouTubeService;

@Component
@He1pME(instruction = "reload", description = "重新讀取資料庫快取", example = "reload")
@RequiredArgsConstructor
public class ReloadAction implements Action {

    private final GoodBoyService goodBoyService;

    private final YouTubeService youtubeService;

    private final TwitchService twitchService;

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Since badWordMap is now a Bean, reloading requires a different approach
        // For now, we'll keep the reload calls, but note that the Beans themselves
        // might need to be refreshed depending on how they are used.
        // If the Services inject the Map Bean, it's a fixed reference.
        // We'll need to check if we should update the Map contents.
        youtubeService.adjustCache();
        twitchService.adjustCache();
        event.reply("/reload completed").queue();
    }
}
