package org.yolok.he1pME.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TimerTaskService {

    @Autowired
    private YouTubeService youtubeService;

    @Autowired
    private TwitchService twitchService;

    @Scheduled(initialDelay = 5000, fixedDelayString = "${frequency}")
    public void doTask() {
        youtubeService.execute();
        twitchService.execute();
    }
}
