package Action;

import Annotation.help;
import Service.YoutubeService;
import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

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
        CommonUtil.getServerDataFromDB();

        CommonUtil.BAD_WORD_MAP.clear();
        CommonUtil.getBadWordFromDB();

        CommonUtil.TWITCH_NOTIFICATION_MAP.clear();
        CommonUtil.getTwitchNotificationFromDB();

        CommonUtil.YOUTUBE_NOTIFICATION_MAP.clear();
        CommonUtil.getYouTubeNotificationFromDB();

        CommonUtil.YT_PLAYLIST_ID_VIDEO_ID_MAP = reconstructYTPlaylistIdVideoIdMap();

        final String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now(ZoneId.systemDefault()));
        log.info("Reload Cache by " + member.getTag() + " at " + now);
    }

    private Map<String, String> reconstructYTPlaylistIdVideoIdMap() {
        final Map<String, String> resultMap = CommonUtil.YT_PLAYLIST_ID_VIDEO_ID_MAP.entrySet().stream()
                .filter(entry -> CommonUtil.YOUTUBE_NOTIFICATION_MAP.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final Set<String> playlistItemResponseSet = CommonUtil.YT_PLAYLIST_ID_VIDEO_ID_MAP.keySet().stream()
                .filter(key -> !resultMap.containsKey(key))
                .map(YoutubeService::callPlayListItemApi)
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
