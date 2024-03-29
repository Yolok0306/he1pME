package org.yolok.he1pME.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yolok.he1pME.entity.BadWord;
import org.yolok.he1pME.repository.BadWordRepository;
import org.yolok.he1pME.util.CommonUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoodBoyService {

    @Autowired
    private BadWordRepository badWordRepository;

    private Map<String, Set<String>> badWordMap;

    @PostConstruct
    public void initBadWordMap() {
        List<BadWord> badWordList = badWordRepository.findAll();
        badWordMap = CollectionUtils.isEmpty(badWordList) ?
                Collections.emptyMap() :
                badWordList.parallelStream().collect(Collectors.groupingBy(
                        BadWord::getGuildId,
                        Collectors.mapping(BadWord::getWord, Collectors.toSet())
                ));
    }

    public void checkContent(Message message) {
        if (!badWordMap.containsKey(message.getGuild().getId())) {
            return;
        }

        Set<String> badWordSet = badWordMap.get(message.getGuild().getId());
        Member member = Objects.requireNonNull(message.getMember());
        String content = message.getContentRaw();
        if (member.getUser().isBot() || notNeedToCheck(member) || !isBadWord(content, badWordSet)) {
            return;
        }

        message.delete().queue();

        int punishmentTime = 3;
        member.timeoutFor(punishmentTime, TimeUnit.MINUTES).queue();
        String title = "言論審查系統";
        String desc = String.format("◆ 不當言論 : %s\n◆ 懲處 : 禁言%d分鐘", content, punishmentTime);
        MessageEmbed he1pMEMessageEmbed = CommonUtil.getHe1pMessageEmbed(member, title, desc, null);
        message.getChannel().sendMessageEmbeds(he1pMEMessageEmbed).queue();
    }

    private boolean notNeedToCheck(Member member) {
        EnumSet<Permission> permissionSet = member.getPermissions();
        return permissionSet.contains(Permission.ADMINISTRATOR) || permissionSet.contains(Permission.MODERATE_MEMBERS);
    }

    private boolean isBadWord(String content, Set<String> badWordSet) {
        content = content.replaceAll("@everyone|@here", StringUtils.EMPTY);
        content = content.replaceAll("<@[!&]?\\d{18}>", StringUtils.EMPTY);
        content = content.replaceAll("<#\\d{18}>", StringUtils.EMPTY);
        content = fullWidthToHalfWidth(content);
        content = content.replaceAll("\\p{Punct}", StringUtils.EMPTY);
        content = content.replaceAll("\\p{Blank}", StringUtils.EMPTY);

        if (StringUtils.isBlank(content)) {
            return false;
        }

        for (String badWord : badWordSet) {
            if (badWord.length() == 1 && StringUtils.containsOnly(content.toLowerCase(), badWord.toLowerCase())) {
                return true;
            } else if (badWord.length() > 1 && StringUtils.contains(content.toLowerCase(), badWord.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String fullWidthToHalfWidth(String content) {
        for (char c : content.toCharArray()) {
            content = content.replace("　", StringUtils.EMPTY);
            if ((int) c >= 65281 && (int) c <= 65374) {
                content = content.replace(c, (char) (((int) c) - 65248));
            }
        }
        return content;
    }
}