package dev.ximarelli.whatsappdailygroupscheduler.domain;

import dev.ximarelli.whatsappdailygroupscheduler.enums.MessageType;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "week_day", unique = true, nullable = false)
    private Integer weekDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public MessageEntity() {
        this.isActive = true;
    }

    public MessageEntity(Integer weekDay, MessageType messageType, String textContent, Boolean isActive) {
        this.weekDay = weekDay;
        this.messageType = messageType;
        this.textContent = textContent;
        this.isActive = isActive != null ? isActive : true;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getWeekDay() {
        return weekDay;
    }

    public void setWeekDay(Integer weekDay) {
        this.weekDay = weekDay;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEntity that = (MessageEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "id=" + id +
                ", weekDay=" + weekDay +
                ", messageType=" + messageType +
                ", isActive=" + isActive +
                '}';
    }
}
