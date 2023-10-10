package org.yolok.he1pME.plugin;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Objects;

@Slf4j
@AllArgsConstructor
public class ResultHandler implements AudioLoadResultHandler {

    private final SlashCommandInteractionEvent event;

    private final AudioTrackScheduler scheduler;

    @Override
    public void trackLoaded(AudioTrack track) {
        if (!scheduler.queue(track)) {
            event.getChannel().sendMessage("Play music failed").queue();
            log.error("trackLoaded failed");
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = Objects.requireNonNullElse(playlist.getSelectedTrack(), playlist.getTracks().get(0));
        if (!scheduler.queue(firstTrack)) {
            event.getChannel().sendMessage("Play music with play list failed").queue();
            log.error("playlistLoaded failed");
        }
    }

    @Override
    public void noMatches() {
        event.getChannel().sendMessage("Load music failed").queue();
        log.error("noMatches");
        if (scheduler.getPlayer().getPlayingTrack() == null && scheduler.getQueue().isEmpty()) {
            scheduler.getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void loadFailed(FriendlyException e) {
        event.getChannel().sendMessage("Load music failed").queue();
        log.error("loadFailed", e);
        if (scheduler.getPlayer().getPlayingTrack() == null && scheduler.getQueue().isEmpty()) {
            scheduler.getAudioManager().closeAudioConnection();
        }
    }
}
