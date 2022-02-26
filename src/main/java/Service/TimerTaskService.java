package Service;

import discord4j.core.GatewayDiscordClient;

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
    }
}
