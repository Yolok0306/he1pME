package Service;

import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class GoodBoyService {
    private final int punishmentTime = 3;

    protected void checkContent(final MessageChannel messageChannel, final Message message, final Member member) {
        if (message.getGuildId().isEmpty() || !CommonUtil.BAD_WORD_MAP.containsKey(message.getGuildId().get().asString())) {
            return;
        }

        final Set<String> badWordSet = CommonUtil.BAD_WORD_MAP.get(message.getGuildId().get().asString());
        final String content = message.getContent();
        if (member.isBot() || notNeedToCheck(member) || !isBadWord(content, badWordSet)) {
            return;
        }

        message.delete().block();
        final int statusCode = callTimeOutApi(member.getGuildId().asString(), member.getId().asString());
        final String title = "言論審查系統";
        final String desc = 299 >= statusCode && statusCode >= 200 ?
                "◆ 不當言論 : " + content + StringUtils.LF + "◆ 懲處 : 禁言" + punishmentTime + "分鐘" :
                "◆ 不當言論 : " + content + StringUtils.LF + "◆ 懲處 : Time Out API失效了沒辦法禁言" +
                        StringUtils.LF + "◆ Status Code : " + statusCode;
        CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, StringUtils.EMPTY);
    }

    private boolean notNeedToCheck(final Member member) {
        final PermissionSet permissionSet = Optional.ofNullable(member.getBasePermissions().block()).orElse(PermissionSet.none());
        return permissionSet.contains(Permission.ADMINISTRATOR) || permissionSet.contains(Permission.MODERATE_MEMBERS);
    }

    private boolean isBadWord(String content, final Set<String> badWordSet) {
        content = content.replaceAll("@everyone|@here", StringUtils.EMPTY);
        content = content.replaceAll("<@[!&]\\d{18}>", StringUtils.EMPTY);
        content = content.replaceAll("<#\\d{18}>", StringUtils.EMPTY);
        content = fullWidthToHalfWidth(content);
        content = content.replaceAll("\\p{Punct}", StringUtils.EMPTY);
        content = content.replaceAll("\\p{Blank}", StringUtils.EMPTY);

        if (StringUtils.isBlank(content)) {
            return false;
        }

        for (final String badWord : badWordSet) {
            if (badWord.length() == 1 && StringUtils.containsOnly(content.toLowerCase(), badWord.toLowerCase())) {
                return true;
            } else if (badWord.length() > 1 && StringUtils.contains(content.toLowerCase(), badWord.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String fullWidthToHalfWidth(String content) {
        for (final char c : content.toCharArray()) {
            content = content.replace("　", StringUtils.EMPTY);
            if ((int) c >= 65281 && (int) c <= 65374) {
                content = content.replace(c, (char) (((int) c) - 65248));
            }
        }
        return content;
    }

    private int callTimeOutApi(final String guildId, final String memberId) {
        final ZonedDateTime futureTime = ZonedDateTime.now(ZoneId.of("UTC")).plusMinutes(punishmentTime);
        final String body = "{\"communication_disabled_until\" : \"" + futureTime.toLocalDateTime() + "\"}";
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMillis(1000)).build();
        try {
            final URI uri = new URI(CommonUtil.DISCORD_API_BASE_URI + "/guilds/" + guildId + "/members/" + memberId);
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .header("Authorization", CommonUtil.DISCORD_API_TOKEN_TYPE + StringUtils.SPACE + CommonUtil.DISCORD_API_TOKEN)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info(httpResponse.statusCode() + StringUtils.SPACE + httpResponse.body());
            return httpResponse.statusCode();
        } catch (final URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return StringUtils.INDEX_NOT_FOUND;
    }
}
