package dev.ximarelli.whatsappdailygroupscheduler.Repositories;

import dev.ximarelli.whatsappdailygroupscheduler.Domain.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    Optional<MessageEntity> findByWeekDayAndIsActiveTrue(Integer weekDay);

}
