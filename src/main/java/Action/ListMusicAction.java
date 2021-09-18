package Action;

import Service.MusicService;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Objects;

public class ListMusicAction extends MusicService implements Action {

    @Override
    public String getAction() {
        return "listQueue";
    }

    @Override
    public void execute(final MessageCreateEvent event) {
        if (checkChannelContainBot(event)) {
            final MessageChannel messageChannel = Objects.requireNonNull(event.getMessage().getChannel().block());
            if (trackScheduler.getQueue().isEmpty()) {
                replyByHe1pMETemplate(messageChannel, "queue is empty");
            } else {
                //TODO set embed with hyperLink
                String result = "queue contains " + trackScheduler.getQueue().size() + " songs";
                for (final AudioTrack audioTrack : trackScheduler.getQueue())
                    result += "\n" + audioTrack.getInfo().title;
                replyByHe1pMETemplate(messageChannel, result);
            }
        }
    }
}
