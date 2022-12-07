package service;

import annotation.help;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import plugin.AudioTrackScheduler;
import plugin.GuildAudioManager;
import util.CommonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MusicService {
    private final Map<String, GuildAudioManager> audioManagerMap = new HashMap<>();
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

    MusicService() {
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    @help(example = "play [musicURI]", description = "播放音樂")
    protected void play(final Message message) {
        final Member member = Objects.requireNonNull(message.getMember());
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty()) {
            return;
        }

        final String regex = String.format("\\%splay\\p{Blank}*", CommonUtil.SIGN);
        final String musicSource = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        if (StringUtils.isBlank(musicSource)) {
            return;
        }

        final GuildAudioManager guildAudioManager;
        final VoiceChannel voiceChannel = voiceChannelOpt.get();
        if (isChannelContainBot(voiceChannel)) {
            guildAudioManager = audioManagerMap.get(message.getGuild().getId());
        } else if (audioManagerMap.containsKey(message.getGuild().getId())) {
            guildAudioManager = audioManagerMap.get(message.getGuild().getId());
            guildAudioManager.scheduler.getPlayer().stopTrack();
            guildAudioManager.scheduler.getQueue().clear();
            message.getGuild().getAudioManager().openAudioConnection(voiceChannel);
        } else {
            guildAudioManager = new GuildAudioManager(audioPlayerManager, message.getGuild());
            audioManagerMap.put(message.getGuild().getId(), guildAudioManager);
            message.getGuild().getAudioManager().setSendingHandler(guildAudioManager.getSendHandler());
            message.getGuild().getAudioManager().openAudioConnection(voiceChannel);
        }

        audioPlayerManager.loadItem(musicSource, new AudioLoadResultHandler() {
            final AudioTrackScheduler scheduler = guildAudioManager.scheduler;

            @Override
            public void trackLoaded(final AudioTrack track) {
                scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(final AudioPlaylist playlist) {
                final AudioTrack firstTrack = Optional.ofNullable(playlist.getSelectedTrack())
                        .orElse(playlist.getTracks().get(0));
                scheduler.queue(firstTrack);
            }

            @Override
            public void noMatches() {
                message.getChannel().sendMessage("Could not play: " + musicSource).queue();
                if (scheduler.getPlayer().getPlayingTrack() == null && scheduler.getQueue().isEmpty()) {
                    message.getGuild().getAudioManager().closeAudioConnection();
                }
            }

            @Override
            public void loadFailed(final FriendlyException exception) {
                message.getChannel().sendMessage(exception.getMessage()).queue();
                if (scheduler.getPlayer().getPlayingTrack() == null && scheduler.getQueue().isEmpty()) {
                    message.getGuild().getAudioManager().closeAudioConnection();
                }
            }
        });
    }

    @help(example = "stop", description = "停止播放音樂")
    protected void stop(final Message message) {
        final Member member = Objects.requireNonNull(message.getMember());
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(message.getGuild().getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.getQueue().clear();
            message.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    @help(example = "np", description = "顯示歌曲的播放資訊")
    protected void np(final Message message) {
        final Member member = Objects.requireNonNull(message.getMember());
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            return;
        }

        final AudioPlayer audioPlayer = audioManagerMap.get(message.getGuild().getId()).scheduler.getPlayer();
        if (audioPlayer.getPlayingTrack() != null && audioPlayer.getPlayingTrack().isSeekable()) {
            final AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
            final String title = "播放資訊";
            final String desc = CommonUtil.descFormat("Title : " + audioTrackInfo.title) + StringUtils.LF +
                    CommonUtil.descFormat("Author : " + audioTrackInfo.author) + StringUtils.LF +
                    CommonUtil.descFormat("Time : " + timeFormat(audioTrackInfo.length));
            CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, StringUtils.EMPTY);
        }
    }

    @help(example = "list", description = "顯示播放清單")
    protected void list(final Message message) {
        final Member member = Objects.requireNonNull(message.getMember());
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            return;
        }

        final BlockingQueue<AudioTrack> queue = audioManagerMap.get(message.getGuild().getId()).scheduler.getQueue();
        final String title, desc;
        if (queue.isEmpty()) {
            title = "播放清單有0首歌 :";
            desc = "播放清單為空";
        } else {
            title = String.format("播放清單有%d首歌 :", queue.size());
            desc = queue.stream()
                    .filter(Objects::nonNull)
                    .map(audioTrack -> CommonUtil.descStartWithDiamondFormat("◆ " + audioTrack.getInfo().title))
                    .collect(Collectors.joining(StringUtils.LF));
        }
        CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, StringUtils.EMPTY);
    }

    @help(example = "skip", description = "跳過這首歌曲")
    protected void skip(final Message message) {
        final Member member = Objects.requireNonNull(message.getMember());
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(message.getGuild().getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.nextTrack();
        }
    }

    @help(example = "pause", description = "暫停/恢復播放歌曲")
    protected void pause(final Message message) {
        final Member member = Objects.requireNonNull(message.getMember());
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final AudioPlayer audioPlayer = audioManagerMap.get(message.getGuild().getId()).scheduler.getPlayer();
            audioPlayer.setPaused(!audioPlayer.isPaused());
        }
    }

    @help(example = "clear", description = "清空播放清單")
    protected void clear(final Message message) {
        final Member member = Objects.requireNonNull(message.getMember());
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final BlockingQueue<AudioTrack> queue = audioManagerMap.get(message.getGuild().getId()).scheduler.getQueue();
            queue.clear();
        }
    }

    private Optional<VoiceChannel> getVoiceChannel(final Member member) {
        return member.getVoiceState() == null || member.getVoiceState().getChannel() == null ?
                Optional.empty() : Optional.of(member.getVoiceState().getChannel().asVoiceChannel());
    }

    private boolean isChannelContainBot(final VoiceChannel voiceChannel) {
        return voiceChannel.getMembers().stream()
                .map(ISnowflake::getId)
                .anyMatch(id -> StringUtils.equals(id, voiceChannel.getJDA().getSelfUser().getId()));
    }

    private String timeFormat(final long milliseconds) {
        return TimeUnit.MILLISECONDS.toHours(milliseconds) == 0 ?
                DurationFormatUtils.formatDuration(milliseconds, "mm:ss", true) :
                DurationFormatUtils.formatDuration(milliseconds, "HH:mm:ss", true);
    }
}
