package dev.ximarelli.whatsappdailygroupscheduler.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TargetGroupRequest(
        @JsonProperty("targetGroupJid") @JsonAlias({"targetGroupId", "groupJid", "chatId", "jid", "number", "phone", "phoneNumber", "targetNumber"}) String targetGroupJid
) {
}
