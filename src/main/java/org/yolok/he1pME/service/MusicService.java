package org.yolok.he1pME.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicService {

    private final AudioPlayerManager audioPlayerManager;

    private final Map<String, AudioTrackScheduler> audioManagerMap = new HashMap<>();

    private static final String NOT_IN_CHANNEL_CONTENT = "You cannot execute this command because you are not in any voice channel or Bot is not in your voice channel";

//    @He1pME(instruction = "play", description = "播放音樂",
//            options = {
//                    @He1pME.Option(name = "music-url", description = "music url")
//            }, example = "play [music-url]")
    public void play(SlashCommandInteractionEvent event) {
        try {
            Member member = Objects.requireNonNull(event.getMember());
            Guild guild = Objects.requireNonNull(event.getGuild());
            AudioChannelUnion audioChannel = getAudioChannel(member);
            if (audioChannel == null) {
                event.reply("You cannot execute this instruction because you are not in any voice channel").setEphemeral(true).queue();
                return;
            }

            AudioManager audioManager = guild.getAudioManager();
            if (isMemberAndBotNotInSameChannel(member)) {
                audioManager.openAudioConnection(audioChannel);
            }

            AudioTrackScheduler scheduler = audioManagerMap.compute(guild.getId(), (key, value) -> {
                if (value == null) {
                    AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
                    audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
                    value = new AudioTrackScheduler(audioPlayer, audioManager);
                    audioPlayer.addListener(new AudioEventListener(value));
                } else {
                    audioManager.setSendingHandler(new AudioPlayerSendHandler(value.getPlayer()));
                }
                return value;
            });

            String musicUrl = Objects.requireNonNull(event.getOption("music-url")).getAsString();
            audioPlayerManager.loadItem(musicUrl, new ResultHandler(event, scheduler));
            event.reply("/play `" + musicUrl + "` completed").queue();
        } catch (Exception e) {
            log.error("MusicService play failed: ", e);
        }
    }

    @He1pME(instruction = "stop", description = "停止播放音樂", example = "stop")
    public void stop(SlashCommandInteractionEvent event) {
        if (validateChannelStatus(event)) {
            Guild guild = Objects.requireNonNull(event.getGuild());
            AudioTrackScheduler scheduler = audioManagerMap.get(guild.getId());
            if (scheduler != null) {
                scheduler.getPlayer().stopTrack();
                scheduler.getQueue().clear();
            }
            guild.getAudioManager().closeAudioConnection();
            event.reply("/stop completed").queue();
        }
    }

    @He1pME(instruction = "np", description = "顯示歌曲的播放資訊", example = "np")
    public void np(SlashCommandInteractionEvent event) {
        if (validateChannelStatus(event) && event.getGuild() != null && event.getMember() != null) {
            AudioPlayer player = audioManagerMap.get(event.getGuild().getId()).getPlayer();
            if (player.getPlayingTrack() == null) {
                event.reply("Currently not playing any track").setEphemeral(true).queue();
                return;
            }

            AudioTrackInfo info = player.getPlayingTrack().getInfo();
            String title = "播放資訊";
            String desc = CommonUtil.descFormat("Title : " + info.title) + StringUtils.LF +
                    CommonUtil.descFormat("Author : " + info.author) + StringUtils.LF +
                    CommonUtil.descFormat("Time : " + timeFormat(info.length));
            MessageEmbed embed = CommonUtil.getHe1pMessageEmbed(event.getMember(), title, desc, null);
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }
    }

    @He1pME(instruction = "list", description = "顯示播放清單", example = "list")
    public void list(SlashCommandInteractionEvent event) {
        if (validateChannelStatus(event) && event.getGuild() != null && event.getMember() != null) {
            BlockingQueue<AudioTrack> queue = audioManagerMap.get(event.getGuild().getId()).getQueue();
            String title, desc;
            if (queue.isEmpty()) {
                title = "播放清單有0首歌 :";
                desc = "播放清單為空";
            } else {
                title = String.format("播放清單有%d首歌 :", queue.size());
                desc = queue.stream()
                        .map(track -> CommonUtil.descStartWithDiamondFormat("◆ " + track.getInfo().title))
                        .collect(Collectors.joining(StringUtils.LF));
            }
            MessageEmbed embed = CommonUtil.getHe1pMessageEmbed(event.getMember(), title, desc, null);
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }
    }

    @He1pME(instruction = "skip", description = "跳過這首歌曲", example = "skip")
    public void skip(SlashCommandInteractionEvent event) {
        if (validateChannelStatus(event) && event.getGuild() != null) {
            AudioTrackScheduler scheduler = audioManagerMap.get(event.getGuild().getId());
            scheduler.getPlayer().stopTrack();
            scheduler.nextTrack();
            event.reply("/skip completed").queue();
        }
    }

    @He1pME(instruction = "pause", description = "暫停/恢復播放歌曲", example = "pause")
    public void pause(SlashCommandInteractionEvent event) {
        if (validateChannelStatus(event) && event.getGuild() != null) {
            AudioPlayer player = audioManagerMap.get(event.getGuild().getId()).getPlayer();
            player.setPaused(!player.isPaused());
            event.reply("/pause completed").queue();
        }
    }

    @He1pME(instruction = "clear", description = "清空播放清單", example = "clear")
    public void clear(SlashCommandInteractionEvent event) {
        if (validateChannelStatus(event) && event.getGuild() != null) {
            BlockingQueue<AudioTrack> queue = audioManagerMap.get(event.getGuild().getId()).getQueue();
            queue.clear();
            event.reply("/clear completed").queue();
        }
    }

    private boolean validateChannelStatus(SlashCommandInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        if (!isMemberAndBotNotInSameChannel(member)) {
            return true;
        }

        event.reply(NOT_IN_CHANNEL_CONTENT).setEphemeral(true).queue();
        return false;
    }

    private boolean isMemberAndBotNotInSameChannel(Member member) {
        AudioChannelUnion memberChannel = member.getVoiceState() != null ? member.getVoiceState().getChannel() : null;
        if (memberChannel == null) {
            return true;
        }

        AudioChannelUnion botChannel = member.getGuild().getSelfMember().getVoiceState() != null ?
                member.getGuild().getSelfMember().getVoiceState().getChannel() : null;
        return botChannel == null || memberChannel.getIdLong() != botChannel.getIdLong();
    }

    @Nullable
    private AudioChannelUnion getAudioChannel(Member member) {
        if (member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            return null;
        }

        return member.getVoiceState().getChannel();
    }

    private String timeFormat(long milliseconds) {
        return TimeUnit.MILLISECONDS.toHours(milliseconds) == 0 ?
                DurationFormatUtils.formatDuration(milliseconds, "mm:ss", true) :
                DurationFormatUtils.formatDuration(milliseconds, "HH:mm:ss", true);
    }
}
