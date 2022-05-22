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
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
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

    @help(example = "play [musicURI]", description = "播放音樂")
    protected void play(final MessageChannel messageChannel, final Message message, final Member member) {
        final VoiceChannel voiceChannel = getVoiceChannel(member).orElse(null);
        if (Objects.isNull(voiceChannel)) {
            return;
        }

        final String regex = "\\" + CommonUtil.SIGN + "play\\p{Blank}*";
        final String musicSource = message.getContent().replaceAll(regex, StringUtils.EMPTY);
        if (StringUtils.isBlank(musicSource)) {
            return;
        }

        if (!isChannelContainBot(voiceChannel)) {
            voiceChannel.join(VoiceChannelJoinSpec.builder().build()
                    .withProvider(GuildAudioManager.of(voiceChannel.getGuildId()).getProvider())).block();
        }

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
                messageChannel.createMessage("Could not play: " + musicSource).block();
            }

            @Override
            public void loadFailed(final FriendlyException exception) {
                messageChannel.createMessage(exception.getMessage()).block();
            }
        });
    }

    @help(example = "stop", description = "停止播放音樂")
    protected void stop(final MessageChannel messageChannel, final Message message, final Member member) {
        final VoiceChannel voiceChannel = getVoiceChannel(member).orElse(null);
        if (Objects.nonNull(voiceChannel) && isChannelContainBot(voiceChannel)) {
            voiceChannel.sendDisconnectVoiceState().block();
        }
    }

    @help(example = "np", description = "顯示歌曲的播放資訊")
    protected void np(final MessageChannel messageChannel, final Message message, final Member member) {
        final VoiceChannel voiceChannel = getVoiceChannel(member).orElse(null);
        if (Objects.isNull(voiceChannel) || !isChannelContainBot(voiceChannel)) {
            return;
        }

        final AudioPlayer audioPlayer = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer();
        if (Objects.nonNull(audioPlayer.getPlayingTrack()) && audioPlayer.getPlayingTrack().isSeekable()) {
            final AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
            final String title = "播放資訊";
            final String desc = CommonUtil.descFormat("Title : " + audioTrackInfo.title) + StringUtils.LF +
                    CommonUtil.descFormat("Author : " + audioTrackInfo.author) + StringUtils.LF +
                    CommonUtil.descFormat("Time : " + timeFormat(audioTrackInfo.length));
            CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, StringUtils.EMPTY);
        }
    }

    @help(example = "list", description = "顯示播放清單")
    protected void list(final MessageChannel messageChannel, final Message message, final Member member) {
        final VoiceChannel voiceChannel = getVoiceChannel(member).orElse(null);
        if (Objects.isNull(voiceChannel) || !isChannelContainBot(voiceChannel)) {
            return;
        }

        final List<AudioTrack> queue = GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().getQueue();
        final String title, desc;
        if (queue.isEmpty()) {
            title = "播放清單有0首歌 :";
            desc = "播放清單為空";
        } else {
            title = "播放清單有" + queue.size() + "首歌 :";
            desc = queue.stream()
                    .filter(Objects::nonNull)
                    .map(audioTrack -> CommonUtil.descStartWithDiamondFormat("◆ " + audioTrack.getInfo().title))
                    .collect(Collectors.joining(StringUtils.LF));
        }
        CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, StringUtils.EMPTY);
    }

    @help(example = "skip", description = "跳過這首歌曲")
    protected void skip(final MessageChannel messageChannel, final Message message, final Member member) {
        final VoiceChannel voiceChannel = getVoiceChannel(member).orElse(null);
        if (Objects.nonNull(voiceChannel) && isChannelContainBot(voiceChannel)) {
            GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler().skip();
        }
    }


    @help(example = "pause", description = "暫停/恢復播放歌曲")
    protected void pause(final MessageChannel messageChannel, final Message message, final Member member) {
        final VoiceChannel voiceChannel = getVoiceChannel(member).orElse(null);
        if (Objects.nonNull(voiceChannel) && isChannelContainBot(voiceChannel)) {
            final boolean isPaused = GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().isPaused();
            GuildAudioManager.of(voiceChannel.getGuildId()).getPlayer().setPaused(!isPaused);
        }
    }

    @help(example = "clear", description = "清空播放清單")
    protected void clear(final MessageChannel messageChannel, final Message message, final Member member) {
        final VoiceChannel voiceChannel = getVoiceChannel(member).orElse(null);
        if (Objects.nonNull(voiceChannel) && isChannelContainBot(voiceChannel)) {
            GuildAudioManager.of(voiceChannel.getGuildId()).getQueue().clear();
        }
    }

    private boolean isChannelContainBot(final VoiceChannel voiceChannel) {
        return Boolean.TRUE.equals(voiceChannel.isMemberConnected(CommonUtil.BOT.getSelfId()).block());
    }

    private Optional<VoiceChannel> getVoiceChannel(final Member member) {
        final AtomicReference<VoiceChannel> voiceChannel = new AtomicReference<>();
        member.getVoiceState().subscribe(voiceState -> voiceState.getChannel().subscribe(voiceChannel::set));
        return Optional.ofNullable(voiceChannel.get());
    }

    private String timeFormat(final long milliseconds) {
        final long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        return hours == 0 ? minutes + ":" + seconds : hours + ":" + minutes + ":" + seconds;
    }
}
