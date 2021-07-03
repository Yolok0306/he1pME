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

    protected void botPlayMusic(final MessageCreateEvent event) {
        final String content = event.getMessage().getContent();
        final String[] array = content.split(" ");
        if (array.length > 1) {
            botJoinChatroom(event);
            audioPlayerManager.loadItem(array[1], trackScheduler);
        }
    }

    protected void botStopMusic(final MessageCreateEvent event) {
        audioPlayer.stopTrack();
        botLeaveChatroom(event);
    }

    private void botJoinChatroom(final MessageCreateEvent event) {
        final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
        voiceChannel.ifPresent(channel -> channel.join(spec -> spec.setProvider(audioProvider)).block());
    }

    private void botLeaveChatroom(final MessageCreateEvent event) {
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

    protected void botPauseMusic(final MessageCreateEvent event) {
        audioPlayer.setPaused(true);
    }

    protected void botResumeMusic(final MessageCreateEvent event) {
        audioPlayer.setPaused(false);
    }

    protected void replyMessageEmbed(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> id = getId("Yolok");
        final Optional<String> img = getURL(IMAGE, "BlueHead");
        if (id.isPresent() && img.isPresent()) {
            channel.createMessage("<@" + id.get() + "> 456").block();
            channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 255)).setImage(img.get())).block();
        }
    }

    protected void replyMessageEmbedForWarnXun(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getURL(IMAGE, "Rushia");
        img.ifPresent(image -> replyMessageEmbedByXunTemplate("又再玩糞Game?", image, channel));
    }

    protected void replyMessageEmbedForAskXun(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getURL(IMAGE, "RainbowAqua");
        img.ifPresent(image -> replyMessageEmbedByXunTemplate("打LOL嗎?", image, channel));
    }

    protected void replyMessageEmbedForConcernXun(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getURL(IMAGE, "AreYouOk");
        img.ifPresent(image -> replyMessageEmbedByXunTemplate("主播人咧?", image, channel));
    }

    private void replyMessageEmbedByXunTemplate(final String msg, final String img,final MessageChannel channel){
        channel.createMessage("<@405739724595265541> " +  msg + "?").block();
        channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img)).block();
    }

    protected void getCurrentTime(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        channel.createMessage(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).block();
    }
}
