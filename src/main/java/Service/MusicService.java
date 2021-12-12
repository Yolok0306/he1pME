package Service;

import MusicPlugin.GuildAudioManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.VoiceChannelJoinSpec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MusicService extends CommonService {
    public static final AudioPlayerManager PLAYER_MANAGER;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize to minimize allocations
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);
    }

    protected void play(final MessageCreateEvent event) {
        join(event);
        final String content = event.getMessage().getContent();
        final String[] array = content.split(" ");
        if (checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent() && array.length == 2) {
            final VoiceChannel voiceChannel = getVoiceChannel(event);
            final String musicSource = content.split(" ")[1];
            PLAYER_MANAGER.loadItem(musicSource, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(final AudioTrack track) {
                    GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().play(track);
                }

                @Override
                public void playlistLoaded(final AudioPlaylist playlist) {
                    final AtomicReference<AudioTrack> firstTrack = new AtomicReference<>(playlist.getSelectedTrack());
                    firstTrack.set(playlist.getTracks().get(0));
                }

                @Override
                public void noMatches() {
                    Objects.requireNonNull(event.getMessage().getChannel().block())
                            .createMessage("Could not play: " + musicSource).block();
                }

                @Override
                public void loadFailed(final FriendlyException exception) {
                    Objects.requireNonNull(event.getMessage().getChannel().block())
                            .createMessage(exception.getMessage()).block();
                }
            });
        }
    }

    protected void stop(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event);
            GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().stopTrack();
            GuildAudioManager.of(voiceChannel.getGuildId()).clearQueue();
        }
        leave(event);
    }

    protected void join(final MessageCreateEvent event) {
        if (!checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event);
            voiceChannel.join(VoiceChannelJoinSpec.builder().build()
                    .withProvider(GuildAudioManager.of(voiceChannel.getGuildId()).getProvider())).block();
        }
    }

    protected void leave(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event);
            voiceChannel.sendDisconnectVoiceState().block();
        }
    }

    protected void np(final MessageCreateEvent event) {
        if (!checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent()) {
            return;
        }

        final VoiceChannel voiceChannel = getVoiceChannel(event);
        final AudioPlayer audioPlayer = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer();
        if (Objects.requireNonNull(audioPlayer.getPlayingTrack()).isSeekable()) {
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            final AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
            final String title = "Title : " + audioTrackInfo.title;
            final String author = "\nAuthor : " + audioTrackInfo.author;
            final String time = "\nTime : " + timeFormat(audioTrackInfo.length);
            replyByHe1pMETemplate(messageChannel, title + author + time);
        }
    }

    protected void list(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event);
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            final List<AudioTrack> queue = GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().getQueue();
            if (queue.isEmpty()) {
                replyByHe1pMETemplate(messageChannel, "Queue is empty!");
            } else {
                //TODO set embed with hyperLink
                final StringBuilder result = new StringBuilder("Queue contains " + queue.size() + " songs :");
                queue.forEach(audioTrack -> result.append("\n").append(audioTrack.getInfo().title));
                replyByHe1pMETemplate(messageChannel, result.toString());
            }
        }
    }

    protected void skip(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event);
            if (!GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().getQueue().isEmpty()) {
                GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().skip();
            }
        }
    }

    protected void pause(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && Optional.ofNullable(getVoiceChannel(event)).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event);
            final boolean control = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().isPaused();
            GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().setPaused(!control);
        }
    }

    protected void resume(final MessageCreateEvent event) {
        pause(event);
    }

    private Boolean checkChannelContainBot(final MessageCreateEvent event) {
        final AtomicReference<Boolean> result = new AtomicReference<>(false);
        Optional.ofNullable(getIdFromDB("he1pME")).ifPresent(token ->
                Optional.ofNullable(getVoiceChannel(event)).ifPresent(channel ->
                        result.set(channel.isMemberConnected(Snowflake.of(token)).block())));
        return result.get();
    }

    private VoiceChannel getVoiceChannel(final MessageCreateEvent event) {
        final AtomicReference<VoiceChannel> voiceChannel = new AtomicReference<>();
        event.getMember().flatMap(member -> Optional.ofNullable(member.getVoiceState().block()))
                .ifPresent(voiceState -> voiceChannel.set(voiceState.getChannel().block()));
        return voiceChannel.get();
    }

    private String timeFormat(long time) {
        time /= 1000;
        final long hours = time / 3600;
        time -= (hours * 3600);
        final String minutes = String.format("%02d", time / 60);
        final String seconds = String.format("%02d", time % 60);
        return hours == 0 ? minutes + ":" + seconds : hours + ":" + minutes + ":" + seconds;
    }
}
