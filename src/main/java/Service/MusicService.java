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
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent() && array.length == 2) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
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
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().stopTrack();
            GuildAudioManager.of(voiceChannel.getGuildId()).clearQueue();
        }
        leave(event);
    }

    protected void join(final MessageCreateEvent event) {
        if (!checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            voiceChannel.join(VoiceChannelJoinSpec.builder().build()
                    .withProvider(GuildAudioManager.of(voiceChannel.getGuildId()).getProvider())).block();
        }
    }

    protected void leave(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            voiceChannel.sendDisconnectVoiceState().block();
        }
    }

    protected void np(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            final AudioPlayer audioPlayer = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer();
            if (Objects.requireNonNull(audioPlayer.getPlayingTrack()).isSeekable()) {
                final AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
                final String title = "播放資訊";
                final String desc = "◆ 標題 : " + titleFormat(audioTrackInfo.title) + "\n◆ 創作者 : " +
                        audioTrackInfo.author + "\n◆ 音樂長度 : " + timeFormat(audioTrackInfo.length);
                replyByHe1pMETemplate(event, title, desc, null);
            }
        }
    }

    protected void list(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            final List<AudioTrack> queue = GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().getQueue();
            if (queue.isEmpty()) {
                final String title = "播放清單有0首歌 :";
                final String desc = "播放清單為空";
                replyByHe1pMETemplate(event, title, desc, null);
            } else {
                //TODO set embed with hyperLink
                final String title = "播放清單有" + queue.size() + "首歌 :";
                final StringBuilder desc = new StringBuilder();
                queue.forEach(audioTrack -> desc.append(titleFormat(audioTrack.getInfo().title)).append("\n"));
                replyByHe1pMETemplate(event, title, desc.toString(), null);
            }
        }
    }

    protected void skip(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            if (!GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().getQueue().isEmpty()) {
                GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().skip();
            }
        }
    }

    protected void pause(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            final boolean control = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().isPaused();
            GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().setPaused(!control);
        }
    }

    protected void resume(final MessageCreateEvent event) {
        pause(event);
    }

    private Boolean checkChannelContainBot(final MessageCreateEvent event) {
        final AtomicReference<Boolean> result = new AtomicReference<>(false);
        getIdFromDB("he1pME").ifPresent(token -> getVoiceChannel(event).ifPresent(channel ->
                result.set(channel.isMemberConnected(Snowflake.of(token)).block())));
        return result.get();
    }

    private Optional<VoiceChannel> getVoiceChannel(final MessageCreateEvent event) {
        final AtomicReference<VoiceChannel> voiceChannel = new AtomicReference<>();
        event.getMember().flatMap(member -> Optional.ofNullable(member.getVoiceState().block()))
                .ifPresent(voiceState -> voiceChannel.set(voiceState.getChannel().block()));
        return Optional.ofNullable(voiceChannel.get());
    }

    private String titleFormat(final String title) {
        return title.length() > 38 ? title.substring(0, 35) + "..." : title;
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
