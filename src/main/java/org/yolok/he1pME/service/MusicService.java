package org.yolok.he1pME.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.plugin.AudioEventListener;
import org.yolok.he1pME.plugin.AudioPlayerSendHandler;
import org.yolok.he1pME.plugin.AudioTrackScheduler;
import org.yolok.he1pME.plugin.ResultHandler;
import org.yolok.he1pME.util.CommonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MusicService {

    @Autowired
    private AudioPlayerManager audioPlayerManager;

    private Map<String, AudioTrackScheduler> audioManagerMap;

    private final String content = "You cannot execute this command because you are not in any voice channel or Bot is not in your voice channel";

    @PostConstruct
    public void init() {
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
        audioManagerMap = new HashMap<>();
    }

    @He1pME(instruction = "play", description = "播放音樂",
            options = {
                    @He1pME.Option(name = "music-url", description = "music url")
            }, example = "play [music-url]")
    public void play(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Guild guild = Objects.requireNonNull(event.getGuild());
        VoiceChannel voiceChannel = getVoiceChannel(member);
        if (voiceChannel == null) {
            event.reply("You cannot execute this instruction because you are not in any voice channel").setEphemeral(true).queue();
            return;
        }

        if (isMemberAndBotNotInSameChannel(member)) {
            AudioManager getAudioManager = guild.getAudioManager();
            getAudioManager.openAudioConnection(voiceChannel);
            audioManagerMap.compute(guild.getId(), (key, value) -> {
                if (value == null) {
                    AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
                    getAudioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
                    value = new AudioTrackScheduler(audioPlayer, getAudioManager);
                    audioPlayer.addListener(new AudioEventListener(value));
                } else {
                    value.getPlayer().stopTrack();
                    value.getQueue().clear();
                }
                return value;
            });
        }


        AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(guild.getId());
        String musicUrl = Objects.requireNonNull(event.getOption("music-url")).getAsString();
        audioPlayerManager.loadItem(musicUrl, new ResultHandler(event, audioTrackScheduler));
        event.reply("/play `" + musicUrl + "` completed").queue();
    }

    @He1pME(instruction = "stop", description = "停止播放音樂", example = "stop")
    public void stop(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        if (isMemberAndBotNotInSameChannel(member)) {
            event.reply(content).setEphemeral(true).queue();
            return;
        }


        Guild guild = Objects.requireNonNull(event.getGuild());
        AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(guild.getId());
        audioTrackScheduler.getPlayer().stopTrack();
        audioTrackScheduler.getQueue().clear();
        guild.getAudioManager().closeAudioConnection();
        event.reply("/stop completed").queue();
    }

    @He1pME(instruction = "np", description = "顯示歌曲的播放資訊", example = "np")
    public void np(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        if (isMemberAndBotNotInSameChannel(member)) {
            event.reply(content).setEphemeral(true).queue();
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        AudioPlayer audioPlayer = audioManagerMap.get(guild.getId()).getPlayer();
        AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
        String title = "播放資訊";
        String desc = CommonUtil.descFormat("Title : " + audioTrackInfo.title) + StringUtils.LF +
                CommonUtil.descFormat("Author : " + audioTrackInfo.author) + StringUtils.LF +
                CommonUtil.descFormat("Time : " + timeFormat(audioTrackInfo.length));
        MessageEmbed he1pMEMessageEmbed = CommonUtil.getHe1pMessageEmbed(member, title, desc, null);
        event.replyEmbeds(he1pMEMessageEmbed).queue();
    }

    @He1pME(instruction = "list", description = "顯示播放清單", example = "list")
    public void list(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        if (isMemberAndBotNotInSameChannel(member)) {
            event.reply(content).setEphemeral(true).queue();
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        BlockingQueue<AudioTrack> queue = audioManagerMap.get(guild.getId()).getQueue();
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
        MessageEmbed he1pMEMessageEmbed = CommonUtil.getHe1pMessageEmbed(member, title, desc, null);
        event.replyEmbeds(he1pMEMessageEmbed).queue();
    }

    @He1pME(instruction = "skip", description = "跳過這首歌曲", example = "skip")
    public void skip(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        if (isMemberAndBotNotInSameChannel(member)) {
            event.reply(content).setEphemeral(true).queue();
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        AudioTrackScheduler audioTrackScheduler = audioManagerMap.get(guild.getId());
        audioTrackScheduler.getPlayer().stopTrack();
        audioTrackScheduler.nextTrack();
        event.reply("/skip completed").queue();
    }

    @He1pME(instruction = "pause", description = "暫停/恢復播放歌曲", example = "pause")
    public void pause(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        if (isMemberAndBotNotInSameChannel(member)) {
            event.reply(content).setEphemeral(true).queue();
            return;
        }

        Guild guild = Objects.requireNonNull(event.getGuild());
        AudioPlayer audioPlayer = audioManagerMap.get(guild.getId()).getPlayer();
        audioPlayer.setPaused(!audioPlayer.isPaused());
        event.reply("/pause completed").queue();
    }

    @He1pME(instruction = "clear", description = "清空播放清單", example = "clear")
    public void clear(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        Guild guild = Objects.requireNonNull(event.getGuild());
        if (isMemberAndBotNotInSameChannel(member)) {
            event.reply(content).setEphemeral(true).queue();
            return;
        }

        BlockingQueue<AudioTrack> queue = audioManagerMap.get(guild.getId()).getQueue();
        queue.clear();
        event.reply("/clear completed").queue();
    }

    private boolean isMemberAndBotNotInSameChannel(Member member) {
        String memberId = member.getId();
        String botId = member.getJDA().getSelfUser().getId();
        VoiceChannel voiceChannel = getVoiceChannel(member);
        if (voiceChannel == null) {
            return true;
        }

        return voiceChannel.getMembers().parallelStream()
                .map(ISnowflake::getId)
                .filter(id -> StringUtils.equals(id, memberId) || StringUtils.equals(id, botId))
                .count() != 2;
    }

    @Nullable
    private VoiceChannel getVoiceChannel(Member member) {
        if (member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            return null;
        }

        return member.getVoiceState().getChannel().asVoiceChannel();
    }

    private String timeFormat(long milliseconds) {
        return TimeUnit.MILLISECONDS.toHours(milliseconds) == 0 ?
                DurationFormatUtils.formatDuration(milliseconds, "mm:ss", true) :
                DurationFormatUtils.formatDuration(milliseconds, "HH:mm:ss", true);
    }
}