package Action;

import Service.MusicService;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import discord4j.core.event.domain.message.MessageCreateEvent;

public class SkipMusicAction extends MusicService implements Action {

    @Override
    public String getAction() {
        return "skipMusic";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        if (audioPlayer.getPlayingTrack().getState() == AudioTrackState.PLAYING) {
            trackScheduler.nextTrack();
        }
    }
}
