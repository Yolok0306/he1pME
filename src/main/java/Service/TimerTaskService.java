package Service;

import java.util.TimerTask;

public class TimerTaskService extends TimerTask {
    private final TwitchService twitchService = new TwitchService();

    @Override
    public void run() {
        twitchService.execute();
    }
}
