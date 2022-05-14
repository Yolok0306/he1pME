package Plugin;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.voice.AudioProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Service.MusicService.PLAYER_MANAGER;

public final class GuildAudioManager {
    private static final Map<Snowflake, GuildAudioManager> MANAGERS = new ConcurrentHashMap<>();
    private final AudioPlayer player;
    private final AudioTrackScheduler scheduler;
    private final LavaPlayerAudioProvider provider;

    public static GuildAudioManager of(final Snowflake guild) {
        return MANAGERS.computeIfAbsent(guild, ignored -> new GuildAudioManager(guild));
    }

    private GuildAudioManager(final Snowflake guild) {
        player = PLAYER_MANAGER.createPlayer();
        scheduler = new AudioTrackScheduler(player, guild);
        provider = new LavaPlayerAudioProvider(player);
        player.addListener(scheduler);
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public AudioTrackScheduler getScheduler() {
        return scheduler;
    }

    public AudioProvider getProvider() {
        return provider;
    }

    public List<AudioTrack> getQueue() {
        return scheduler.getQueue();
    }
}
