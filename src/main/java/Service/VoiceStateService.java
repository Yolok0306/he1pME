package Service;

import MusicPlugin.GuildAudioManager;
import Util.CommonUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;

import java.util.Objects;

public class VoiceStateService {
    private final Snowflake he1pME = Snowflake.of(CommonUtil.getIdFromDB("he1pME").orElse(""));

    public void receiveEvent(final VoiceStateUpdateEvent event) {
        if (event.isLeaveEvent() && Objects.equals(event.getCurrent().getUserId(), he1pME)) {
            GuildAudioManager.of(event.getCurrent().getGuildId()).getPlayer().stopTrack();
            GuildAudioManager.of(event.getCurrent().getGuildId()).getQueue().clear();
        }
    }
}
