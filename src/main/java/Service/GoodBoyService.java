package Service;

import Util.CommonUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GoodBoyService {
    private final int punishmentTime = 3;

    protected void checkContent(final Member member, final Message message, final String content, final MessageChannel messageChannel) {
        if (CollectionUtils.isEmpty(CommonUtil.BAD_WORD_SET)) {
            return;
        }

        if (member.isBot() || notNeedToCheck(member) || !isBadWord(content)) {
            return;
        }

        message.delete().block();
        final int statusCode = callTimeOutApi(member.getGuildId().asString(), member.getId().asString());
        final String title = "言論審查系統";
        final String desc = 299 >= statusCode && statusCode >= 200 ?
                "◆ 不當言論 : " + content + StringUtils.LF + "◆ 懲處 : 禁言" + punishmentTime + "分鐘" :
                "◆ 不當言論 : " + content + StringUtils.LF + "◆ 懲處 : Time Out API失效了沒辦法禁言" +
                        StringUtils.LF + "◆ Status Code : " + statusCode;
        CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, null);
    }

    private boolean notNeedToCheck(final Member member) {
        final PermissionSet permissionSet = Optional.ofNullable(member.getBasePermissions().block()).orElse(PermissionSet.none());
        return permissionSet.contains(Permission.ADMINISTRATOR) || permissionSet.contains(Permission.MODERATE_MEMBERS);
    }

    private boolean isBadWord(String content) {
        content = content.replaceAll("@everyone|@here", StringUtils.EMPTY);
        content = content.replaceAll("<@[!&]\\d{18}>", StringUtils.EMPTY);
        content = content.replaceAll("<#\\d{18}>", StringUtils.EMPTY);
        content = fullWidthToHalfWidth(content);
        content = content.replaceAll("\\p{Punct}", StringUtils.EMPTY);
        content = content.replaceAll("\\p{Blank}", StringUtils.EMPTY);

        if (StringUtils.isBlank(content)) {
            return false;
        }

        for (final String badWord : CommonUtil.BAD_WORD_SET) {
            if (badWord.length() == 1 && StringUtils.containsOnly(content, badWord)) {
                return true;
            } else if (badWord.length() > 1 && StringUtils.contains(content, badWord)) {
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
        final Date futureTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(punishmentTime));
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssXXX");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String body = "{\"communication_disabled_until\" : \"" + simpleDateFormat.format(futureTime) + "\"}";
        try {
            final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(CommonUtil.BASE_URI + "/guilds/" + guildId + "/members/" + memberId))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bot " + CommonUtil.TOKEN)
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
