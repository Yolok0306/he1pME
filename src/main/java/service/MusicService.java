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
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
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
    private static final Map<String, GuildAudioManager> AUDIO_MANAGER_MAP;
    private static final AudioPlayerManager AUDIO_PLAYER_MANAGER;

    static {
        AUDIO_MANAGER_MAP = new HashMap<>();
        AUDIO_PLAYER_MANAGER = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(AUDIO_PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(AUDIO_PLAYER_MANAGER);
    }

    @help(example = "play [musicURI]", description = "播放音樂")
    protected void play(final MessageChannel messageChannel, final Message message, final Member member) {
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty()) {
            return;
        }

        final String regex = "\\" + CommonUtil.SIGN + "play\\p{Blank}*";
        final String musicSource = message.getContentRaw().replaceAll(regex, StringUtils.EMPTY);
        if (StringUtils.isBlank(musicSource)) {
            return;
        }

        final VoiceChannel voiceChannel = voiceChannelOpt.get();
        final GuildAudioManager guildAudioManager;
        if (isChannelContainBot(voiceChannel)) {
            guildAudioManager = AUDIO_MANAGER_MAP.get(message.getGuild().getId());
        } else if (AUDIO_MANAGER_MAP.containsKey(message.getGuild().getId())) {
            guildAudioManager = AUDIO_MANAGER_MAP.get(message.getGuild().getId());
            guildAudioManager.scheduler.getPlayer().stopTrack();
            guildAudioManager.scheduler.getQueue().clear();
            message.getGuild().getAudioManager().openAudioConnection(voiceChannel);
        } else {
            guildAudioManager = new GuildAudioManager(AUDIO_PLAYER_MANAGER, message.getGuild());
            AUDIO_MANAGER_MAP.put(message.getGuild().getId(), guildAudioManager);
            message.getGuild().getAudioManager().setSendingHandler(guildAudioManager.getSendHandler());
            message.getGuild().getAudioManager().openAudioConnection(voiceChannel);
        }

        AUDIO_PLAYER_MANAGER.loadItem(musicSource, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(final AudioTrack track) {
                guildAudioManager.scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(final AudioPlaylist playlist) {
                final AudioTrack firstTrack = Optional.ofNullable(playlist.getSelectedTrack())
                        .orElse(playlist.getTracks().get(0));
                guildAudioManager.scheduler.queue(firstTrack);
            }

            @Override
            public void noMatches() {
                messageChannel.sendMessage("Could not play: " + musicSource).queue();
            }

            @Override
            public void loadFailed(final FriendlyException exception) {
                messageChannel.sendMessage(exception.getMessage()).queue();
            }
        });
    }

    @help(example = "stop", description = "停止播放音樂")
    protected void stop(final MessageChannel messageChannel, final Message message, final Member member) {
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final AudioTrackScheduler audioTrackScheduler = AUDIO_MANAGER_MAP.get(message.getGuild().getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.getQueue().clear();
            message.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    @help(example = "np", description = "顯示歌曲的播放資訊")
    protected void np(final MessageChannel messageChannel, final Message message, final Member member) {
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            return;
        }

        final AudioPlayer audioPlayer = AUDIO_MANAGER_MAP.get(message.getGuild().getId()).scheduler.getPlayer();
        if (audioPlayer.getPlayingTrack() != null && audioPlayer.getPlayingTrack().isSeekable()) {
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
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isEmpty() || !isChannelContainBot(voiceChannelOpt.get())) {
            return;
        }

        final BlockingQueue<AudioTrack> queue = AUDIO_MANAGER_MAP.get(message.getGuild().getId()).scheduler.getQueue();
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
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final AudioTrackScheduler audioTrackScheduler = AUDIO_MANAGER_MAP.get(message.getGuild().getId()).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.nextTrack();
        }
    }

    @help(example = "pause", description = "暫停/恢復播放歌曲")
    protected void pause(final MessageChannel messageChannel, final Message message, final Member member) {
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final AudioPlayer audioPlayer = AUDIO_MANAGER_MAP.get(message.getGuild().getId()).scheduler.getPlayer();
            audioPlayer.setPaused(!audioPlayer.isPaused());
        }
    }

    @help(example = "clear", description = "清空播放清單")
    protected void clear(final MessageChannel messageChannel, final Message message, final Member member) {
        final Optional<VoiceChannel> voiceChannelOpt = getVoiceChannel(member);
        if (voiceChannelOpt.isPresent() && isChannelContainBot(voiceChannelOpt.get())) {
            final BlockingQueue<AudioTrack> queue = AUDIO_MANAGER_MAP.get(message.getGuild().getId()).scheduler.getQueue();
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
        final long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        return hours == 0 ? minutes + ":" + seconds : hours + ":" + minutes + ":" + seconds;
    }
}
