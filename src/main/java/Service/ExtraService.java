package Service;

import Service.MusicService.LavaPlayerAudioProvider;
import Service.MusicService.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Color;
import discord4j.voice.AudioProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ExtraService extends MainService {
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
    private final AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
    private final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer);
    private final AudioProvider audioProvider = new LavaPlayerAudioProvider(audioPlayer);

    public ExtraService() {
        audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
    }

    protected void playMusic(final MessageCreateEvent event) {
        final String content = event.getMessage().getContent();
        final String[] array = content.split(" ");
        if (array.length > 1) {
            if(!checkChannelContainBot(event)){
                final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
                voiceChannel.ifPresent(channel -> channel.join(spec -> spec.setProvider(audioProvider)).block());
            }
            audioPlayerManager.loadItemOrdered(audioPlayerManager, array[1], new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    trackScheduler.queue(audioTrack);
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    AtomicReference<AudioTrack> firstTrack = new AtomicReference<>(audioPlaylist.getSelectedTrack());
                    firstTrack.set(audioPlaylist.getTracks().get(0));
                }

                @Override
                public void noMatches() {
                    Objects.requireNonNull(event.getMessage().getChannel().block())
                            .createMessage("Could not play: " + array[1]).block();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    Objects.requireNonNull(event.getMessage().getChannel().block())
                            .createMessage(exception.getMessage()).block();
                }
            });
        }
    }

    protected void stopMusic(final MessageCreateEvent event) {
        if(checkChannelContainBot(event)) {
            audioPlayer.stopTrack();
            trackScheduler.clearQueue();
            final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
            voiceChannel.ifPresent(channel -> channel.sendDisconnectVoiceState().block());
        }
    }

    protected void skipMusic(final MessageCreateEvent event) {
        if(audioPlayer.getPlayingTrack().getState() == AudioTrackState.PLAYING){
            trackScheduler.nextTrack();
        }
    }

    protected void listQueue(final MessageCreateEvent event) {
        if(checkChannelContainBot(event)){
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            if(trackScheduler.getQueue().isEmpty()){
                replyByHe1pMETemplate(messageChannel,"queue is empty");
            }else{
                //TODO set embed with hyperLink
                String result = "queue contains " + trackScheduler.getQueue().size() + " songs";
                for (AudioTrack audioTrack : trackScheduler.getQueue()) result += "\n" + audioTrack.getInfo().title;
                replyByHe1pMETemplate(messageChannel,result);
            }
        }
    }

    protected void getMusicInfo(final MessageCreateEvent event){
        if(checkChannelContainBot(event) && audioPlayer.getPlayingTrack().isSeekable()){
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
            String title = "Title : " + audioTrackInfo.title;
            String author = "\nAuthor : " + audioTrackInfo.author;
            String time = "\nTime : " + timeFormat(audioTrackInfo.length);
            replyByHe1pMETemplate(messageChannel,title + author + time);
        }
    }

    protected void pauseMusic(final MessageCreateEvent event) {
        audioPlayer.setPaused(true);
    }

    protected void resumeMusic(final MessageCreateEvent event) {
        audioPlayer.setPaused(false);
    }

    protected void replyMessageEmbed(final MessageCreateEvent event) {
        final MessageChannel channel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> id = getId("Yolok");
        final Optional<String> img = getURL(IMAGE, "BlueHead");
        if (id.isPresent() && img.isPresent()) {
            channel.createMessage("<@" + id.get() + "> 456").block();
            channel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 255)).setImage(img.get())).block();
        }
    }

    protected void warnXunByMessageEmbed(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getURL(IMAGE, "HorrorRushia");
        img.ifPresent(image -> replyByXunTemplate(messageChannel,"又再玩糞Game?", image));
    }

    protected void askXunByMessageEmbed(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getURL(IMAGE, "RainbowAqua");
        img.ifPresent(image -> replyByXunTemplate(messageChannel,"打LOL嗎?", image));
    }

    protected void concernXunByMessageEmbed(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> img = getURL(IMAGE, "MikasaConcern");
        img.ifPresent(image -> replyByXunTemplate(messageChannel,"主播人咧?", image));
    }

    protected void concernYueByMessageEmbed(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final Optional<String> id = getId("Yue");
        final Optional<String> img = getURL(IMAGE, "AngryAqua");
        if (id.isPresent() && img.isPresent()){
            replyByDefaultTemplate(messageChannel ,id.get(),"醒了嗎?" ,img.get());
        }
    }

    protected void getCurrentTime(final MessageCreateEvent event) {
        final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
        final String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        replyByHe1pMETemplate(messageChannel,time);
    }

    private Boolean checkChannelContainBot(final MessageCreateEvent event){
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        final Optional<VoiceChannel> voiceChannel = getVoiceChannel(event);
        voiceChannel.ifPresent(channel ->
                result.set(channel.isMemberConnected(Snowflake.of(getId("he1pME").get())).block())
        );
        return result.get();
    }

    private Optional<VoiceChannel> getVoiceChannel(final MessageCreateEvent event) {
        final Member member = event.getMember().orElse(null);
        if (Optional.ofNullable(member).isPresent()) {
            final VoiceState voiceState = member.getVoiceState().block();
            if (Optional.ofNullable(voiceState).isPresent()) {
                return Optional.ofNullable(voiceState.getChannel().block());
            }
        }
        return Optional.empty();
    }

    private String timeFormat(long time){
        time /= 1000;
        long hours = time / 3600;
        time -= (hours * 3600);
        String minutes = String.format("%02d" ,time / 60);
        String seconds = String.format("%02d" ,time % 60);
        if (hours == 0){
            return minutes + ":" + seconds;
        }
        return hours + ":" + minutes + ":" + seconds;
    }

    private void replyByXunTemplate(final MessageChannel messageChannel, final String msg, final String img){
        final Optional<String> id = getId("Xun");
        id.ifPresent(xun -> {
            messageChannel.createMessage("<@" + xun + "> " + msg).block();
            messageChannel.createEmbed(spec -> spec.setColor(Color.of(0, 255, 127)).setImage(img)).block();
        });
    }

    private void replyByDefaultTemplate(final MessageChannel messageChannel, final String id ,final String msg, final String img){
        messageChannel.createMessage("<@" + id + "> " + msg).block();
        messageChannel.createEmbed(spec -> spec.setColor(Color.BLUE).setImage(img)).block();
    }

    private void  replyByHe1pMETemplate(final MessageChannel messageChannel, final String msg){
        messageChannel.createEmbed(spec -> {
            spec.setTitle(msg);
            spec.setColor(Color.of(255,192,203));
        }).block();
    }
}
