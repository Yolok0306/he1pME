package Action;

import Service.MusicService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.VoiceChannel;

import java.util.Optional;

public class StopMusicAction extends MusicService implements Action {

    @Override
    public String getAction() {
        return "stopMusic";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        if (checkChannelContainBot(event)) {
            audioPlayer.stopTrack();
            trackScheduler.clearQueue();
            final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
            voiceChannel.ifPresent(channel -> channel.sendDisconnectVoiceState().block());
        }
    }
}
