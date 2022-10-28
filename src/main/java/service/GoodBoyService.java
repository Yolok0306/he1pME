package service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import util.CommonUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GoodBoyService {
    public static final Map<String, Set<String>> BAD_WORD_MAP = new HashMap<>();

    protected void checkContent(final MessageChannel messageChannel, final Message message, final Member member) {
        if (!BAD_WORD_MAP.containsKey(message.getGuild().getId())) {
            return;
        }

        final Set<String> badWordSet = BAD_WORD_MAP.get(message.getGuild().getId());
        final String content = message.getContentRaw();
        if (member.getUser().isBot() || notNeedToCheck(member) || !isBadWord(content, badWordSet)) {
            return;
        }

        message.delete().queue();

        final int punishmentTime = 3;
        member.timeoutFor(punishmentTime, TimeUnit.MINUTES).queue();
        final String title = "言論審查系統";
        final String desc = "◆ 不當言論 : " + content + StringUtils.LF + "◆ 懲處 : 禁言" + punishmentTime + "分鐘";
        CommonUtil.replyByHe1pMETemplate(messageChannel, member, title, desc, StringUtils.EMPTY);
    }

    private boolean notNeedToCheck(final Member member) {
        final EnumSet<Permission> permissionSet = member.getPermissions();
        return permissionSet.contains(Permission.ADMINISTRATOR) || permissionSet.contains(Permission.MODERATE_MEMBERS);
    }

    private boolean isBadWord(String content, final Set<String> badWordSet) {
        content = content.replaceAll("@everyone|@here", StringUtils.EMPTY);
        content = content.replaceAll("<@[!&]?\\d{18}>", StringUtils.EMPTY);
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
}
