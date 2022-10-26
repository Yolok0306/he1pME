package action;

import annotation.help;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;
import service.GoodBoyService;
import service.TwitchService;
import service.YouTubeService;
import util.CommonUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
        YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP = reconstructYTPlaylistIdVideoIdMap();

        final String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(ZoneId.systemDefault()));
        log.info("Reload Cache by " + member.getUser().getAsTag() + " at " + now);
    }

    private Map<String, String> reconstructYTPlaylistIdVideoIdMap() {
        final Map<String, String> resultMap = YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP.entrySet().stream()
                .filter(entry -> YouTubeService.YOUTUBE_NOTIFICATION_MAP.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Set<String> playlistItemResponseSet = YouTubeService.YT_PLAYLIST_ID_VIDEO_ID_MAP.keySet().stream()
                .filter(key -> !resultMap.containsKey(key))
                .map(YouTubeService::callPlayListItemApi)
                .collect(Collectors.toSet());
        final Map<String, String> newYTPlaylistIdVideoIdMap = playlistItemResponseSet.stream()
                .map(JSONObject::new)
                .map(playlistJsonObject -> playlistJsonObject.getJSONArray("items"))
                .filter(playlistItemJsonArray -> !playlistItemJsonArray.isEmpty())
                .map(playlistItemJsonArray -> playlistItemJsonArray.getJSONObject(0).getJSONObject("snippet"))
                .filter(snippetJsonObject -> !CommonUtil.checkStartTime(snippetJsonObject.getString("publishedAt"), null))
                .collect(Collectors.toMap(snippetJsonObject -> snippetJsonObject.getString("playlistId"),
                        snippetJsonObject -> snippetJsonObject.getJSONObject("resourceId").getString("videoId"),
                        (existing, replacement) -> existing, HashMap::new));
        resultMap.putAll(newYTPlaylistIdVideoIdMap);
        return resultMap;
    }
}
