package org.yolok.he1pME.plugin;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class AudioEventListener extends AudioEventAdapter {

    private final AudioTrackScheduler audioTrackScheduler;

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        log.info("Started playing track: {} ({})", track.getInfo().title, track.getInfo().uri);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        log.info("Track ended: {} ({}), reason: {}", track.getInfo().title, track.getInfo().uri, endReason);
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            audioTrackScheduler.nextTrack();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        log.error("Track exception for: {} ({})", track.getInfo().title, track.getInfo().uri, exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        log.warn("Track stuck for: {} ({}), thresholdMs: {}", track.getInfo().title, track.getInfo().uri, thresholdMs);
    }
}
