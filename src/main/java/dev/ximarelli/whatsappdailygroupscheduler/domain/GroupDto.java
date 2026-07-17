package dev.ximarelli.whatsappdailygroupscheduler.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record GroupDto(
        UUID id,
        String name,
        @JsonProperty("groupJid") @JsonAlias({"chatId", "jid", "targetGroupId", "targetGroupJid", "number", "phone", "phoneNumber", "targetNumber"}) String groupJid,
        @JsonProperty("isSelected") @JsonAlias({"active", "selected", "isActive"}) boolean isSelected
) {
    public static GroupDto fromEntity(GroupEntity entity) {
        return new GroupDto(
                entity.getId(),
                entity.getName(),
                entity.getGroupJid(),
                entity.isSelected()
        );
    }

    public GroupEntity toEntity() {
        GroupEntity entity = new GroupEntity();
        entity.setId(this.id());
        entity.setName(this.name());
        entity.setGroupJid(this.groupJid());
        entity.setSelected(this.isSelected());
        return entity;
    }
}
