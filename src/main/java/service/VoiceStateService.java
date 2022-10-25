package service;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import org.apache.commons.lang3.StringUtils;
import plugin.AudioTrackScheduler;
import util.CommonUtil;

public class VoiceStateService {
    public void execute(final GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() != null && StringUtils.equals(event.getMember().getId(), event.getJDA().getSelfUser().getId())) {
            final AudioTrackScheduler audioTrackScheduler = CommonUtil.getGuildAudioPlayer(event.getGuild(), null).scheduler;
            audioTrackScheduler.getPlayer().stopTrack();
            audioTrackScheduler.getQueue().clear();
        }
    }
}
