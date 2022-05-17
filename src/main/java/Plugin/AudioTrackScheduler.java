package Plugin;

import Util.CommonUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.common.util.Snowflake;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class AudioTrackScheduler extends AudioEventAdapter {
    private final List<AudioTrack> queue;
    private final AudioPlayer player;
    private final Snowflake guild;

    public AudioTrackScheduler(final AudioPlayer player, final Snowflake guild) {
        // The queue may be modifed by different threads so guarantee memory safety
        // This does not, however, remove several race conditions currently present
        queue = Collections.synchronizedList(new LinkedList<>());
        this.player = player;
        this.guild = guild;
    }

    public List<AudioTrack> getQueue() {
        return queue;
    }

    public boolean play(final AudioTrack track) {
        return play(track, false);
    }

    public boolean play(final AudioTrack track, final boolean force) {
        final boolean playing = player.startTrack(track, !force);

        if (!playing) {
            queue.add(track);
        }

        return playing;
    }

    public void skip() {
        if (CollectionUtils.isNotEmpty(queue)) {
            play(queue.remove(0), true);
        }
    }

    @Override
    public void onTrackEnd(final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason endReason) {
        // Advance the player if the track completed naturally (FINISHED) or if the track cannot play (LOAD_FAILED)
        if (CollectionUtils.isEmpty(queue)) {
            CommonUtil.BOT.getGuildMembers(guild).subscribe(member ->
                    member.getVoiceState().subscribe(voiceState ->
                            voiceState.getChannel().subscribe(voiceChannel ->
                                    voiceChannel.sendDisconnectVoiceState().block())));
        } else if (endReason.mayStartNext) {
            skip();
        }
    }
}