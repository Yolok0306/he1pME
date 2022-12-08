package action;

import annotation.help;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import service.GoodBoyService;
import service.TwitchService;
import service.YouTubeService;
import util.CommonUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@help(example = "reload", description = "重新載入資料庫資料")
public class ReloadAction implements Action {
    @Override
    public String getInstruction() {
        return "reload";
    }

    @Override
    public void execute(final Message message) {
        GoodBoyService.BAD_WORD_MAP.clear();
        TwitchService.TWITCH_NOTIFICATION_MAP.clear();
        YouTubeService.YOUTUBE_NOTIFICATION_MAP.clear();
        CommonUtil.loadAllDataFromDB();

        adjustTwitchCache();
        adjustYoutubeCache();
        message.delete().queue();

        final Member member = Objects.requireNonNull(message.getMember());
        final String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(ZoneId.systemDefault()));
        log.info("Reload Cache by {} at {}!", member.getUser().getAsTag(), now);
    }

    private void adjustTwitchCache() {
        if (TwitchService.TWITCH_NOTIFICATION_MAP.isEmpty()) {
            TwitchService.TWITCH_CACHE.clear();
            return;
        }

        final Map<String, String> existingDataMap = TwitchService.TWITCH_CACHE.entrySet().parallelStream()
                .filter(entry -> TwitchService.TWITCH_NOTIFICATION_MAP.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Set<String> newDataSet = TwitchService.TWITCH_NOTIFICATION_MAP.keySet().parallelStream()
                .filter(key -> !existingDataMap.containsKey(key))
                .collect(Collectors.toSet());
        TwitchService.TWITCH_CACHE.clear();
        TwitchService.TWITCH_CACHE.putAll(existingDataMap);
        TwitchService.addDataToTwitchCache(newDataSet);
    }

    private void adjustYoutubeCache() {
        if (YouTubeService.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            YouTubeService.YOUTUBE_CACHE.clear();
            return;
        }

        final Map<String, String> existingDataMap = YouTubeService.YOUTUBE_CACHE.entrySet().parallelStream()
                .filter(entry -> YouTubeService.YOUTUBE_NOTIFICATION_MAP.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Set<String> newDataSet = YouTubeService.filterAndGetPlayListItemResponseSet(existingDataMap.keySet());
        YouTubeService.YOUTUBE_CACHE.clear();
        YouTubeService.YOUTUBE_CACHE.putAll(existingDataMap);
        YouTubeService.addDataToYoutubeCache(newDataSet);
    }
}
