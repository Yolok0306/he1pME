package Action;

import Service.MusicService;
import discord4j.core.event.domain.message.MessageCreateEvent;

public class PauseMusicAction extends MusicService implements Action {
    @Override
    public String getAction() {
        return "pauseMusic";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final boolean control = audioPlayer.isPaused();
        audioPlayer.setPaused(!control);
    }
}
