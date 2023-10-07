package org.yolok.he1pME.plugin;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;

public class GuildAudioManager {
    /**
     * Track scheduler for the player.
     */
    public final AudioTrackScheduler scheduler;
    private final AudioPlayer player;

    /**
     * Creates a player and a track scheduler.
     *
     * @param manager Audio player manager to use for creating the player.
     */
    public GuildAudioManager(AudioPlayerManager manager, Guild guild) {
        player = manager.createPlayer();
        scheduler = new AudioTrackScheduler(player, guild);
        player.addListener(scheduler);
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }
}
