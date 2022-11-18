package plugin;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;

public final class GuildAudioManager {
    private final AudioPlayer player;
    /**
     * Track scheduler for the player.
     */
    public final AudioTrackScheduler scheduler;

    /**
     * Creates a player and a track scheduler.
     *
     * @param manager Audio player manager to use for creating the player.
     */
    public GuildAudioManager(final AudioPlayerManager manager, final Guild guild) {
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
