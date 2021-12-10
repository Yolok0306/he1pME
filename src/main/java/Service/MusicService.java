package Service;

import MusicPlugin.LavaPlayerAudioProvider;
import MusicPlugin.TrackScheduler;
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
import discord4j.voice.AudioProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MusicService extends MainService {
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
    private final AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
    private final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer);
    private final AudioProvider audioProvider = new LavaPlayerAudioProvider(audioPlayer);

    public MusicService() {
        audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
    }

    protected void playMusic(final MessageCreateEvent event) {
        final String content = event.getMessage().getContent();
        final String[] array = content.split(" ");
        if (checkChannelContainBot(event) && array.length == 2) {
            final String musicSource = content.split(" ")[1];
            audioPlayerManager.loadItemOrdered(audioPlayerManager, musicSource, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(final AudioTrack audioTrack) {
                    trackScheduler.queue(audioTrack);
                }

                @Override
                public void playlistLoaded(final AudioPlaylist audioPlaylist) {
                    final AtomicReference<AudioTrack> firstTrack = new AtomicReference<>(audioPlaylist.getSelectedTrack());
                    firstTrack.set(audioPlaylist.getTracks().get(0));
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

    protected void stopMusic(final MessageCreateEvent event) {
        if (checkChannelContainBot(event)) {
            audioPlayer.stopTrack();
            trackScheduler.clearQueue();
        }
    }

    protected void joinBot(final MessageCreateEvent event) {
        if (!checkChannelContainBot(event)) {
            Optional.ofNullable(getVoiceChannel(event)).ifPresent(channel ->
                    channel.join(spec -> spec.setProvider(audioProvider)).block());
        }
    }

    protected void leaveBot(final MessageCreateEvent event) {
        if (checkChannelContainBot(event)) {
            Optional.ofNullable(getVoiceChannel(event)).ifPresent(channel ->
                    channel.sendDisconnectVoiceState().block());
        }
    }

    protected void getMusicInfo(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && Optional.ofNullable(audioPlayer.getPlayingTrack()).isPresent() &&
                audioPlayer.getPlayingTrack().isSeekable()) {
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            final AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
            final String title = "Title : " + audioTrackInfo.title;
            final String author = "\nAuthor : " + audioTrackInfo.author;
            final String time = "\nTime : " + timeFormat(audioTrackInfo.length);
            replyByHe1pMETemplate(messageChannel, title + author + time);
        }
    }

    protected void listQueue(final MessageCreateEvent event) {
        if (checkChannelContainBot(event)) {
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            if (trackScheduler.getQueue().isEmpty()) {
                replyByHe1pMETemplate(messageChannel, "queue is empty");
            } else {
                //TODO set embed with hyperLink
                final StringBuilder result = new StringBuilder("queue contains " + trackScheduler.getQueue().size() + " songs");
                for (final AudioTrack audioTrack : trackScheduler.getQueue())
                    result.append("\n").append(audioTrack.getInfo().title);
                replyByHe1pMETemplate(messageChannel, result.toString());
            }
        }
    }

    protected void skipMusic() {
        if (!trackScheduler.getQueue().isEmpty()) {
            trackScheduler.nextTrack();
        }
    }

    protected void pauseMusic() {
        final boolean control = audioPlayer.isPaused();
        audioPlayer.setPaused(!control);
    }

    protected void resumeMusic() {
        final boolean control = audioPlayer.isPaused();
        audioPlayer.setPaused(!control);
    }

    private Boolean checkChannelContainBot(final MessageCreateEvent event) {
        final AtomicReference<Boolean> result = new AtomicReference<>(false);
        Optional.ofNullable(getTokenFromDB("he1pME")).ifPresent(token ->
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
