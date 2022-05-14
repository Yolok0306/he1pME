package Service;

import Annotation.help;
import Plugin.GuildAudioManager;
import Util.CommonUtil;
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
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.VoiceChannelJoinSpec;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MusicService {
    public static final AudioPlayerManager PLAYER_MANAGER;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize to minimize allocations
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);
    }

    @help(example = "play [musicURI]", description = "撥放音樂")
    protected void play(final MessageCreateEvent event) {
        if (!checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            voiceChannel.join(VoiceChannelJoinSpec.builder().build()
                    .withProvider(GuildAudioManager.of(voiceChannel.getGuildId()).getProvider())).block();
        }

        final String content = event.getMessage().getContent();
        final String musicSource = content.replaceAll("\\" + CommonUtil.SIGN + "play\\p{Blank}++", StringUtils.EMPTY);
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent() && StringUtils.isNotBlank(musicSource)) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
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
                    event.getMessage().getChannel().subscribe(messageChannel ->
                            messageChannel.createMessage("Could not play: " + musicSource).block());
                }

                @Override
                public void loadFailed(final FriendlyException exception) {
                    event.getMessage().getChannel().subscribe(messageChannel ->
                            messageChannel.createMessage(exception.getMessage()).block());
                }
            });
        }
    }

    @help(example = "stop", description = "停止撥放音樂")
    protected void stop(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            voiceChannel.sendDisconnectVoiceState().block();
        }
    }

    @help(example = "np", description = "顯示歌曲的播放資訊")
    protected void np(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            final AudioPlayer audioPlayer = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer();
            if (Objects.nonNull(audioPlayer.getPlayingTrack()) && audioPlayer.getPlayingTrack().isSeekable()) {
                final AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
                final String title = "播放資訊";
                final String desc = CommonUtil.descFormat("Title : " + audioTrackInfo.title) + StringUtils.LF +
                        CommonUtil.descFormat("Author : " + audioTrackInfo.author) + StringUtils.LF +
                        CommonUtil.descFormat("Time : " + timeFormat(audioTrackInfo.length));
                CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
            }
        }
    }

    @help(example = "list", description = "顯示播放清單")
    protected void list(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            final List<AudioTrack> queue = GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().getQueue();
            if (queue.isEmpty()) {
                final String title = "播放清單有0首歌 :";
                final String desc = "播放清單為空";
                CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
            } else {
                final String title = "播放清單有" + queue.size() + "首歌 :";
                final String desc = queue.stream()
                        .filter(Objects::nonNull)
                        .map(audioTrack -> CommonUtil.descStartWithDiamondFormat("◆ " + audioTrack.getInfo().title))
                        .collect(Collectors.joining(StringUtils.LF));
                CommonUtil.replyByHe1pMETemplate(event, title, desc.toString(), null);
            }
        }
    }

    @help(example = "skip", description = "跳過這首歌曲")
    protected void skip(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            if (!GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().getQueue().isEmpty()) {
                GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().skip();
            }
        }
    }

    @help(example = "pause", description = "暫停/撥放歌曲")
    protected void pause(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            final boolean control = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().isPaused();
            GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().setPaused(!control);
        }
    }

    @help(example = "clear", description = "清空播放清單")
    protected void clear(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && getVoiceChannel(event).isPresent()) {
            final VoiceChannel voiceChannel = getVoiceChannel(event).get();
            GuildAudioManager.of(voiceChannel.getGuildId()).getQueue().clear();
        }
    }

    private Boolean checkChannelContainBot(final MessageCreateEvent event) {
        final AtomicReference<Boolean> result = new AtomicReference<>(false);
        CommonUtil.BOT.getSelf().subscribe(user -> getVoiceChannel(event).ifPresent(voiceChannel ->
                result.set(voiceChannel.isMemberConnected(user.getId()).block())));
        return result.get();
    }

    private Optional<VoiceChannel> getVoiceChannel(final MessageCreateEvent event) {
        final AtomicReference<VoiceChannel> voiceChannel = new AtomicReference<>();
        event.getMember().flatMap(member -> Optional.ofNullable(member.getVoiceState().block()))
                .ifPresent(voiceState -> voiceChannel.set(voiceState.getChannel().block()));
        return Optional.ofNullable(voiceChannel.get());
    }

    private String timeFormat(final long milliseconds) {
        final long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        return hours == 0 ? minutes + ":" + seconds : hours + ":" + minutes + ":" + seconds;
    }
}
