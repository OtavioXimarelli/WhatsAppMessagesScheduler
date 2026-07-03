package dev.ximarelli.whatsappdailygroupscheduler.domain;

public record ManualTriggerRequest(
        Integer weekDay,
        String message,
        String targetGroupId) {
}
