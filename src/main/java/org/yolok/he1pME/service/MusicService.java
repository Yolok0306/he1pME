package org.yolok.he1pME.service;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.annotation.Help;
import org.yolok.he1pME.plugin.AudioTrackScheduler;
import org.yolok.he1pME.plugin.GuildAudioManager;
import org.yolok.he1pME.util.CommonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MusicService {
    private final Map<String, GuildAudioManager> audioManagerMap = new HashMap<>();
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

    @PostConstruct
    public void init() {
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    @Help(example = "play [musicURI]", description = "播放音樂")
    public void play(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty()) {
            return;
        }

        String regex = String.format("\\%splay\\p{Blank}*", CommonUtil.SIGN);
        String musicSource = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        if (StringUtils.isBlank(musicSource)) {
            return;
        }

        GuildAudioManager guildAudioManager;
        VoiceChannel voiceChannel = voiceChannelOpt.get();
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
            public void trackLoaded(AudioTrack track) {
                scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = Optional.ofNullable(playlist.getSelectedTrack())
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
            public void loadFailed(FriendlyException exception) {
                message.getChannel().sendMessage(exception.getMessage()).queue();
                if (scheduler.getPlayer().getPlayingTrack() == null && scheduler.getQueue().isEmpty()) {
                    message.getGuild().getAudioManager().closeAudioConnection();
                }
            }
        });
    }

    @Help(example = "stop", description = "停止播放音樂")
    public void stop(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(message.getGuild().getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.getQueue().clear();
            message.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    @Help(example = "np", description = "顯示歌曲的播放資訊")
    public void np(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            return;
        }

        AudioPlayer audioPlayer = audioManagerMap.get(message.getGuild().getId()).scheduler.getPlayer();
        if (audioPlayer.getPlayingTrack() != null && audioPlayer.getPlayingTrack().isSeekable()) {
            AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
            String title = "播放資訊";
            String desc = CommonUtil.descFormat("Title : " + audioTrackInfo.title) + StringUtils.LF +
                    CommonUtil.descFormat("Author : " + audioTrackInfo.author) + StringUtils.LF +
                    CommonUtil.descFormat("Time : " + timeFormat(audioTrackInfo.length));
            CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, StringUtils.EMPTY);
        }
    }

    @Help(example = "list", description = "顯示播放清單")
    public void list(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            return;
        }

        BlockingQueue<AudioTrack> queue = audioManagerMap.get(message.getGuild().getId()).scheduler.getQueue();
        String title, desc;
        if (queue.isEmpty()) {
            title = "播放清單有0首歌 :";
            desc = "播放清單為空";
        } else {
            title = String.format("播放清單有%d首歌 :", queue.size());
            desc = queue.parallelStream()
                    .map(audioTrack -> CommonUtil.descStartWithDiamondFormat("◆ " + audioTrack.getInfo().title))
                    .collect(Collectors.joining(StringUtils.LF));
        }
        CommonUtil.replyByHe1pMETemplate(message.getChannel(), member, title, desc, StringUtils.EMPTY);
    }

    @Help(example = "skip", description = "跳過這首歌曲")
    public void skip(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(message.getGuild().getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.nextTrack();
        }
    }

    @Help(example = "pause", description = "暫停/恢復播放歌曲")
    public void pause(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            AudioPlayer audioPlayer = audioManagerMap.get(message.getGuild().getId()).scheduler.getPlayer();
            audioPlayer.setPaused(!audioPlayer.isPaused());
        }
    }

    @Help(example = "clear", description = "清空播放清單")
    public void clear(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            BlockingQueue<AudioTrack> queue = audioManagerMap.get(message.getGuild().getId()).scheduler.getQueue();
            queue.clear();
        }
    }

    private Optional<VoiceChannel> getVoiceChannel(Member member) {
        return member.getVoiceState() == null || member.getVoiceState().getChannel() == null ?
                Optional.empty() : Optional.of(member.getVoiceState().getChannel().asVoiceChannel());
    }

    private boolean isChannelContainBot(VoiceChannel voiceChannel) {
        return voiceChannel.getMembers().parallelStream()
                .map(ISnowflake::getId)
                .anyMatch(id -> StringUtils.equals(id, voiceChannel.getJDA().getSelfUser().getId()));
    }

    private String timeFormat(long milliseconds) {
        return TimeUnit.MILLISECONDS.toHours(milliseconds) == 0 ?
                DurationFormatUtils.formatDuration(milliseconds, "mm:ss", true) :
                DurationFormatUtils.formatDuration(milliseconds, "HH:mm:ss", true);
    }
}