package Action;

import Service.MusicService;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;

public class GetMusicInfoAction extends MusicService implements Action {
    @Override
    public String getAction() {
        return "getMusicInfo";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        if (checkChannelContainBot(event) && audioPlayer.getPlayingTrack().isSeekable()) {
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            final AudioTrackInfo audioTrackInfo = audioPlayer.getPlayingTrack().getInfo();
            final String title = "Title : " + audioTrackInfo.title;
            final String author = "\nAuthor : " + audioTrackInfo.author;
            final String time = "\nTime : " + timeFormat(audioTrackInfo.length);
            replyByHe1pMETemplate(messageChannel, title + author + time);
        }
    }

    private String timeFormat(long time) {
        time /= 1000;
        final long hours = time / 3600;
        time -= (hours * 3600);
        final String minutes = String.format("%02d", time / 60);
        final String seconds = String.format("%02d", time % 60);
        if (hours == 0) {
            return minutes + ":" + seconds;
        }
        return hours + ":" + minutes + ":" + seconds;
    }
}
