package Service;

import Service.musicPlugin.LavaPlayerAudioProvider;
import Service.musicPlugin.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MusicService {
    private final AudioPlayerManager audioPlayerManager;
    protected final AudioPlayer audioPlayer;
    protected final TrackScheduler trackScheduler;
    protected final AudioProvider audioProvider;

    public MusicService() {
        audioPlayerManager = new DefaultAudioPlayerManager();
        audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);

        audioPlayer = audioPlayerManager.createPlayer();
        trackScheduler = new TrackScheduler(audioPlayer);
        audioProvider = new LavaPlayerAudioProvider(audioPlayer);
    }

    protected void botJoinChatroom(final MessageCreateEvent event) {
        final Member member = event.getMember().orElse(null);
        if (Optional.ofNullable(member).isPresent()) {
            final VoiceState voiceState = member.getVoiceState().block();
            if (Optional.ofNullable(voiceState).isPresent()) {
                final VoiceChannel channel = voiceState.getChannel().block();
                if (Optional.ofNullable(channel).isPresent()) {
                    channel.join(spec -> spec.setProvider(audioProvider)).block();
                }
            }
        }
    }

    protected void botPlayMusic(final String content) {
        final List<String> command = Arrays.asList(content.split(" "));
        audioPlayerManager.loadItem(command.get(1), trackScheduler);
    }
}
