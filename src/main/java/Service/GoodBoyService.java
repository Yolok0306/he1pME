package Service;

import Util.CommonUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class GoodBoyService {
    private final Set<String> badWordSet = new HashSet<>();
    private final Map<String, Integer> badBoyMap = new HashMap<>();

    public void addBadWordSet(final String badWord) {
        badWordSet.add(badWord);
    }

    protected void checkContent(final MessageCreateEvent event, final String content) {
        if (badWordSet.isEmpty()) {
            return;
        }

        event.getMember().ifPresent(member -> {
            if (member.isBot() || notNeedToCheck(member) || !isBadWord(content)) {
                return;
            }

            event.getMessage().delete().block();
            member.addRole(CommonUtil.muteRole, "Bad Boy").block();
            final String title = "言論審查系統";
            final String desc = "◆ 不當言論 : " + content + "\n◆ 懲處 : 禁言3分鐘";
            CommonUtil.replyByHe1pMETemplate(event, title, desc, null);
            badBoyMap.put(member.getGuildId() + "-" + member.getId(), 3);
        });
    }

    private boolean notNeedToCheck(final Member member) {
        final PermissionSet permissionSet = Optional.ofNullable(member.getBasePermissions().block()).orElse(PermissionSet.none());
        return permissionSet.contains(Permission.ADMINISTRATOR) || permissionSet.contains(Permission.MANAGE_ROLES);
    }

    private boolean isBadWord(String content) {
        content = content.replaceAll("@everyone|@here", "");
        content = content.replaceAll("<@[!&]\\d{18}>", "");
        content = content.replaceAll("<#\\d{18}>", "");
        content = fullWidthToHalfWidth(content);
        content = content.replaceAll("\\p{Punct}", "");
        content = content.replaceAll("\\p{Blank}", "");

        if (StringUtils.isBlank(content)) {
            return false;
        }

        for (final String badWord : badWordSet) {
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
            content = content.replace("　", "");
            if ((int) c >= 65281 && (int) c <= 65374) {
                content = content.replace(c, (char) (((int) c) - 65248));
            }
        }
        return content;
    }

    protected void updateMap(final GatewayDiscordClient bot) {
        if (badBoyMap.isEmpty()) {
            return;
        }

        for (final Map.Entry<String, Integer> entry : badBoyMap.entrySet()) {
            if (entry.getValue() > 1) {
                badBoyMap.replace(entry.getKey(), entry.getValue() - 1);
            } else {
                final String guildId = entry.getKey().split("-")[0];
                final String memberId = entry.getKey().split("-")[1];
                bot.getGuilds().toStream()
                        .filter(guild -> StringUtils.equals(guild.getId().toString(), guildId))
                        .findFirst().flatMap(guild -> guild.getMembers().toStream()
                                .filter(member -> StringUtils.equals(member.getId().toString(), memberId))
                                .findFirst()).ifPresent(member -> member.removeRole(CommonUtil.muteRole).block());
                badBoyMap.remove(entry.getKey());
            }
        }
    }
}
