package Service;

import MusicPlugin.GuildAudioManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import discord4j.core.GatewayDiscordClient;

import java.util.Objects;
import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    GatewayDiscordClient bot;
    GoodBoyService goodBoyService;

    public TimerTaskService(final GatewayDiscordClient bot, final GoodBoyService goodBoyService) {
        this.bot = bot;
        this.goodBoyService = goodBoyService;
    }

    @Override
    public void run() {
        goodBoyService.updateMap(bot);
        leaveChannelAutomatically();
    }

    private void leaveChannelAutomatically() {
        bot.getUserById(bot.getSelfId()).subscribe(user -> {
            bot.getGuilds().toStream().forEach(guild -> user.asMember(guild.getId()).subscribe(member -> {
                member.getVoiceState().subscribe(voiceState -> {
                    final AudioPlayer audioPlayer = GuildAudioManager.of(guild.getId()).getPlayer();
                    if (Objects.isNull(audioPlayer.getPlayingTrack()) && GuildAudioManager.of(guild.getId()).getQueue().isEmpty()) {
                        voiceState.getChannel().subscribe(voiceChannel -> voiceChannel.sendDisconnectVoiceState().block());
                    }
                });
            }));
        });
    }
}
