package Service;

import Util.CommonUtil;
import discord4j.core.GatewayDiscordClient;

import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final GatewayDiscordClient bot = CommonUtil.bot;
    private final GoodBoyService goodBoyService;

    public TimerTaskService(final GoodBoyService goodBoyService) {
        this.goodBoyService = goodBoyService;
    }

    @Override
    public void run() {
        goodBoyService.updateMap(bot);
    }
}
