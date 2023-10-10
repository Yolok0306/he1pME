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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.annotation.He1pME;
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

    @He1pME(instruction = "play", description = "播放音樂",
            options = {
                    @He1pME.Option(name = "music-url", description = "music url")
            }, example = "play [music-url]")
    public void play(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty()) {
            event.reply("You cannot execute this instruction").setEphemeral(true).queue();
            return;
        }

        String musicUrl = Objects.requireNonNull(event.getOption("music-url")).getAsString();
        GuildAudioManager guildAudioManager;
        Guild guild = Objects.requireNonNull(event.getGuild());
        VoiceChannel voiceChannel = voiceChannelOpt.get();
        if (isChannelContainBot(voiceChannel)) {
            guildAudioManager = audioManagerMap.get(guild.getId());
        } else if (audioManagerMap.containsKey(guild.getId())) {
            guildAudioManager = audioManagerMap.get(guild.getId());
            guildAudioManager.scheduler.getPlayer().stopTrack();
            guildAudioManager.scheduler.getQueue().clear();
            guild.getAudioManager().openAudioConnection(voiceChannel);
        } else {
            guildAudioManager = new GuildAudioManager(audioPlayerManager, guild);
            audioManagerMap.put(guild.getId(), guildAudioManager);
            guild.getAudioManager().setSendingHandler(guildAudioManager.getSendHandler());
            guild.getAudioManager().openAudioConnection(voiceChannel);
        }

        audioPlayerManager.loadItem(musicUrl, new AudioLoadResultHandler() {
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
                event.getChannel().sendMessage("Could not play: " + musicUrl).queue();
                if (scheduler.getPlayer().getPlayingTrack() == null && scheduler.getQueue().isEmpty()) {
                    guild.getAudioManager().closeAudioConnection();
                }
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getChannel().sendMessage(exception.getMessage()).queue();
                if (scheduler.getPlayer().getPlayingTrack() == null && scheduler.getQueue().isEmpty()) {
                    guild.getAudioManager().closeAudioConnection();
                }
            }
        });

        event.reply("Add `" + musicUrl + "` completed").queue();
    }

    @He1pME(instruction = "stop", description = "停止播放音樂", example = "stop")
    public void stop(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Guild guild = Objects.requireNonNull(event.getGuild());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(guild.getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.getQueue().clear();
            guild.getAudioManager().closeAudioConnection();
        }
        event.reply("Completed").queue();
    }

    @He1pME(instruction = "np", description = "顯示歌曲的播放資訊", example = "np")
    public void np(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            event.reply("You cannot execute this instruction").setEphemeral(true).queue();
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        AudioPlayer audioPlayer = audioManagerMap.get(guild.getId()).scheduler.getPlayer();
        if (audioPlayer.getPlayingTrack() == null || !audioPlayer.getPlayingTrack().isSeekable()) {
            event.reply("You cannot execute this instruction").setEphemeral(true).queue();
            return;
        }

        AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
        String title = "播放資訊";
        String desc = CommonUtil.descFormat("Title : " + audioTrackInfo.title) + StringUtils.LF +
                CommonUtil.descFormat("Author : " + audioTrackInfo.author) + StringUtils.LF +
                CommonUtil.descFormat("Time : " + timeFormat(audioTrackInfo.length));
        CommonUtil.replyByHe1pMETemplate(event, member, title, desc, null);
    }

    @He1pME(instruction = "list", description = "顯示播放清單", example = "list")
    public void list(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            event.reply("You cannot execute this instruction").setEphemeral(true).queue();
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        BlockingQueue<AudioTrack> queue = audioManagerMap.get(guild.getId()).scheduler.getQueue();
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
        CommonUtil.replyByHe1pMETemplate(event, member, title, desc, null);
    }

    @He1pME(instruction = "skip", description = "跳過這首歌曲", example = "skip")
    public void skip(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Guild guild = Objects.requireNonNull(event.getGuild());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(guild.getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.nextTrack();
        }
        event.reply("Completed").queue();
    }

    @He1pME(instruction = "pause", description = "暫停/恢復播放歌曲", example = "pause")
    public void pause(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Guild guild = Objects.requireNonNull(event.getGuild());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            AudioPlayer audioPlayer = audioManagerMap.get(guild.getId()).scheduler.getPlayer();
            audioPlayer.setPaused(!audioPlayer.isPaused());
        }
        event.reply("Completed").queue();
    }

    @He1pME(instruction = "clear", description = "清空播放清單", example = "clear")
    public void clear(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Guild guild = Objects.requireNonNull(event.getGuild());
        Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            BlockingQueue<AudioTrack> queue = audioManagerMap.get(guild.getId()).scheduler.getQueue();
            queue.clear();
        }
        event.reply("Completed").queue();
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