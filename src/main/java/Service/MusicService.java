package Service;

import MusicPlugin.LavaPlayerAudioProvider;
import MusicPlugin.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Color;
import discord4j.voice.AudioProvider;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MusicService extends MainService {
    protected AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
    protected AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
    protected TrackScheduler trackScheduler = new TrackScheduler(audioPlayer);
    protected AudioProvider audioProvider = new LavaPlayerAudioProvider(audioPlayer);

    public MusicService() {
        audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
    }

    protected Boolean checkChannelContainBot(final MessageCreateEvent event) {
        final AtomicReference<Boolean> result = new AtomicReference<>(false);
        final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
        voiceChannel.ifPresent(channel ->
                result.set(channel.isMemberConnected(Snowflake.of(getTokenFromDB("he1pME").get())).block())
        );
        return result.get();
    }

    protected Optional<VoiceChannel> getVoiceChannel(final MessageCreateEvent event) {
        final Member member = event.getMember().orElse(null);
        if (Optional.ofNullable(member).isPresent()) {
            final VoiceState voiceState = member.getVoiceState().block();
            if (Optional.ofNullable(voiceState).isPresent()) {
                return Optional.ofNullable(voiceState.getChannel().block());
            }
        }
        return Optional.empty();
    }

    protected void replyByHe1pMETemplate(final MessageChannel messageChannel, final String msg) {
        messageChannel.createEmbed(spec -> {
            spec.setTitle(msg);
            spec.setColor(Color.of(255, 192, 203));
        }).block();
    }
}
