package dev.ximarelli.whatsappdailygroupscheduler.repository;

import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    Optional<MessageEntity> findByWeekDayAndIsActiveTrue(Integer weekDay);

}
