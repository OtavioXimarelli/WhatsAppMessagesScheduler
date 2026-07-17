package dev.ximarelli.whatsappdailygroupscheduler.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DirectMessageRequest(
        @JsonProperty("number")
        @JsonAlias({"phoneNumber", "phone", "recipient", "targetNumber", "targetGroupId", "targetGroupJid", "jid"})
        String number,

        @JsonProperty("message")
        @JsonAlias({"text", "textContent"})
        String message
) {
}
