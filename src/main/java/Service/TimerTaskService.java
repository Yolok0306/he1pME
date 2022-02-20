package Service;

import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    GoodBoyService goodBoyService;

    public TimerTaskService(final GoodBoyService goodBoyService) {
        this.goodBoyService = goodBoyService;
    }

    @Override
    public void run() {
        goodBoyService.updateMap();
    }
}
