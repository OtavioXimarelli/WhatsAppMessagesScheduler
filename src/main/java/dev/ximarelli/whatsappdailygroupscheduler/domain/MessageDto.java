package dev.ximarelli.whatsappdailygroupscheduler.domain;

import dev.ximarelli.whatsappdailygroupscheduler.enums.MessageType;
import java.util.UUID;

public record MessageDto(
        UUID id,
        Integer weekDay,
        MessageType messageType,
        String textContent,
        boolean isActive
) {
    public static MessageDto fromEntity(MessageEntity entity) {
        return new MessageDto(
                entity.getId(),
                entity.getWeekDay(),
                entity.getMessageType(),
                entity.getTextContent(),
                entity.isActive()
        );
    }

    public MessageEntity toEntity() {
        MessageEntity entity = new MessageEntity();
        entity.setId(this.id());
        entity.setWeekDay(this.weekDay());
        entity.setMessageType(this.messageType());
        entity.setTextContent(this.textContent());
        entity.setActive(this.isActive());
        return entity;
    }
}
