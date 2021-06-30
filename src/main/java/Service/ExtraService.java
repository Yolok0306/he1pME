package Service;

import Service.MusicService.LavaPlayerAudioProvider;
import Service.MusicService.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Color;
import discord4j.voice.AudioProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class ExtraService extends MainService {
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
    private final AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
    private final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer);
    private final AudioProvider audioProvider = new LavaPlayerAudioProvider(audioPlayer);

    public ExtraService() {
        audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
    }

    protected void botJoinChatroom(final MessageCreateEvent event) {
        final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
        voiceChannel.ifPresent(channel -> channel.join(spec -> spec.setProvider(audioProvider)).block());
    }

    protected void botLeaveChatroom(final MessageCreateEvent event) {
        final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
        voiceChannel.ifPresent(channel -> channel.sendDisconnectVoiceState().block());
    }

    private Optional<VoiceChannel> getVoiceChannel(final MessageCreateEvent event) {
        final Member member = event.getMember().orElse(null);
        if (Optional.ofNullable(member).isPresent()) {
            final VoiceState voiceState = member.getVoiceState().block();
            if (Optional.ofNullable(voiceState).isPresent()) {
                return Optional.ofNullable(voiceState.getChannel().block());
            }
        }
        return Optional.empty();
    }

    protected void botPlayMusic(final MessageCreateEvent event) {
        final String content = event.getMessage().getContent();
        final String[] array = content.split(" ");
        if (array.length > 1) {
            botJoinChatroom(event);
            audioPlayerManager.loadItem(array[1], trackScheduler);
        }
    }

    protected void botPauseMusic(final MessageCreateEvent event) {
        audioPlayer.setPaused(true);
    }

    protected void botResumeMusic(final MessageCreateEvent event) {
        audioPlayer.setPaused(false);
    }

    protected void botStopMusic(final MessageCreateEvent event) {
        audioPlayer.stopTrack();
        botLeaveChatroom(event);
    }

    protected void replyMessageEmbed(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> id = getId("Yolok");
        final Optional<String> img = getURL(IMAGE, "Rushia");
        if (id.isPresent() && img.isPresent()) {
            channel.createMessage("<@" + id.get() + "> 又再玩糞Game?").block();
            channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img.get())).block();
        }
    }

    protected void replyMessageEmbedForXun(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> id = getId("Xun");
        final Optional<String> img = getURL(IMAGE, "Rushia");
        if (id.isPresent() && img.isPresent()) {
            channel.createMessage("<@" + id.get() + "> 又再玩糞Game?").block();
            channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img.get())).block();
        }
    }

    protected void getCurrentTime(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        channel.createMessage(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).block();
    }
}
