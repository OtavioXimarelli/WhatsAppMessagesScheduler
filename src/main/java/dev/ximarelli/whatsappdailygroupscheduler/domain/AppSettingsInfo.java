package dev.ximarelli.whatsappdailygroupscheduler.domain;

import java.util.Collections;
import java.util.List;

public record AppSettingsInfo(
        String targetGroupJid,
        String evolutionApiUrl,
        String evolutionInstanceName,
        String cronTime,
        List<GroupDto> groups
) {
    public AppSettingsInfo(String targetGroupJid, String evolutionApiUrl, String evolutionInstanceName, String cronTime) {
        this(targetGroupJid, evolutionApiUrl, evolutionInstanceName, cronTime, Collections.emptyList());
    }
}

