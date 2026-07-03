package dev.ximarelli.whatsappdailygroupscheduler.service;

import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.repository.MessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class MessageService {
    private final MessageRepository repository;

    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }

    public List<MessageEntity> getAllMessages() {
        return repository.findAll();
    }

    public Optional<MessageEntity> getByWeekDay(Integer weekDay) {
        return repository.findByWeekDayAndIsActiveTrue(weekDay);
    }

    @Transactional
    public MessageEntity saveOrUpdateMessages(MessageEntity messageEntity) {

        if (messageEntity.getWeekDay() == null || messageEntity.getWeekDay() < 1 || messageEntity.getWeekDay() > 7) {
            throw new IllegalArgumentException("Week day must be between 1 (Monday) and 7 (Sunday)");
        }

        Optional<MessageEntity> existingMessage = repository.findByWeekDay(messageEntity.getWeekDay());

        if (existingMessage.isPresent()) {
            MessageEntity messageToUpdate = existingMessage.get();
            messageToUpdate.setMessageType(messageEntity.getMessageType());
            messageToUpdate.setTextContent(messageEntity.getTextContent());
            messageToUpdate.setActive(messageEntity.isActive());
            return repository.save(messageToUpdate);
        } else {
            return repository.save(messageEntity);
        }
    }

    @Transactional
    public void deleteMessageById(UUID id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Message not found with id " + id);
        }
        repository.deleteById(id);
    }
}
