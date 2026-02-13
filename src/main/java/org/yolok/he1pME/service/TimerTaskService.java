package org.yolok.he1pME.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimerTaskService {

    private final YouTubeService youtubeService;

    private final TwitchService twitchService;

    @Scheduled(initialDelay = 5000, fixedDelayString = "${frequency}")
    public void doTask() {
        youtubeService.execute();
        twitchService.execute();
    }
}
