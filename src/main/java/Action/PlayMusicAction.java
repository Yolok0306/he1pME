package Action;

import Service.MusicService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.VoiceChannel;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class PlayMusicAction extends MusicService implements Action {

    @Override
    public String getAction() {
        return "playMusic";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        final String content = event.getMessage().getContent();
        final String[] array = content.split(" ");
        if (array.length > 1) {
            if (!checkChannelContainBot(event)) {
                final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
                voiceChannel.ifPresent(channel -> channel.join(spec -> spec.setProvider(audioProvider)).block());
            }
            audioPlayerManager.loadItemOrdered(audioPlayerManager, array[1], new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(final AudioTrack audioTrack) {
                    trackScheduler.queue(audioTrack);
                }

                @Override
                public void playlistLoaded(final AudioPlaylist audioPlaylist) {
                    final AtomicReference<AudioTrack> firstTrack = new AtomicReference<>(audioPlaylist.getSelectedTrack());
                    firstTrack.set(audioPlaylist.getTracks().get(0));
                }

                @Override
                public void noMatches() {
                    Objects.requireNonNull(event.getMessage().getChannel().block())
                            .createMessage("Could not play: " + array[1]).block();
                }

                @Override
                public void loadFailed(final FriendlyException exception) {
                    Objects.requireNonNull(event.getMessage().getChannel().block())
                            .createMessage(exception.getMessage()).block();
                }
            });
        }
    }

}
