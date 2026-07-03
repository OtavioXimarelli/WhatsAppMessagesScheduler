package dev.ximarelli.whatsappdailygroupscheduler.domain;

public record AppSettingsInfo(
        String targetGroupJid,
        String evolutionApiUrl,
        String evolutionInstanceName,
        String cronTime
) {
}
