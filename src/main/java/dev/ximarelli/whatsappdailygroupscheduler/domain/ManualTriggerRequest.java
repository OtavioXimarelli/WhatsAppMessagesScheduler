package dev.ximarelli.whatsappdailygroupscheduler.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ManualTriggerRequest(
        Integer weekDay,
        String message,
        @JsonProperty("targetGroupId")
        @JsonAlias({"targetGroupJid", "targetNumber", "number", "phone", "phoneNumber", "recipient", "chatId", "jid"})
        String targetGroupId
) {
}
