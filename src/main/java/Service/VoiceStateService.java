package Service;

import Plugin.GuildAudioManager;
import Util.CommonUtil;
import discord4j.core.event.domain.VoiceStateUpdateEvent;

import java.util.Objects;

public class VoiceStateService {
    public void receiveEvent(final VoiceStateUpdateEvent event) {
        if (event.isLeaveEvent() && Objects.equals(event.getCurrent().getUserId(), CommonUtil.BOT.getSelfId())) {
            GuildAudioManager.of(event.getCurrent().getGuildId()).getPlayer().stopTrack();
            GuildAudioManager.of(event.getCurrent().getGuildId()).getQueue().clear();
        }
    }
}
