package action;

import annotation.help;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONException;
import service.GoodBoyService;
import service.TwitchService;
import service.YouTubeService;
import util.CommonUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
    public void execute(final MessageChannel messageChannel, final Message message, final Member member) {
        GoodBoyService.BAD_WORD_MAP.clear();
        TwitchService.TWITCH_NOTIFICATION_MAP.clear();
        YouTubeService.YOUTUBE_NOTIFICATION_MAP.clear();
        CommonUtil.loadAllDataFromDB();

        adjustTwitchChannelSet();
        adjustYTPlaylistIdVideoIdMap();
        message.delete().queue();

        final String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(ZoneId.systemDefault()));
        log.info("Reload Cache by {} at {}!", member.getUser().getAsTag(), now);
    }

    private void adjustTwitchChannelSet() {
        if (TwitchService.TWITCH_NOTIFICATION_MAP.isEmpty()) {
            TwitchService.TWITCH_CHANNEL_SET.clear();
            return;
        }

        TwitchService.TWITCH_CHANNEL_SET.clear();
        try {
            TwitchService.addDataToTwitchChannelSet();
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }

    private void adjustYTPlaylistIdVideoIdMap() {
        if (YouTubeService.YOUTUBE_NOTIFICATION_MAP.isEmpty()) {
            YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP.clear();
            return;
        }

        final Map<String, String> existingDataMap = YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP.entrySet().stream()
                .filter(entry -> YouTubeService.YOUTUBE_NOTIFICATION_MAP.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Set<String> playlistItemResponseSet = YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP.keySet().stream()
                .filter(key -> !existingDataMap.containsKey(key))
                .map(YouTubeService::callPlayListItemApi)
                .collect(Collectors.toSet());
        YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP.clear();
        YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP.putAll(existingDataMap);
        try {
            YouTubeService.addDataToYTPlaylistIdVideoIdMap(playlistItemResponseSet);
        } catch (final JSONException exception) {
            exception.printStackTrace();
        }
    }
}
