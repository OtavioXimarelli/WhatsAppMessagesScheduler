package dev.ximarelli.whatsappdailygroupscheduler.Controller;

import dev.ximarelli.whatsappdailygroupscheduler.Domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.Service.MessageSchedulerService;
import dev.ximarelli.whatsappdailygroupscheduler.Service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service;
    private final MessageSchedulerService schedulerService;

    public MessageController(MessageService service, MessageSchedulerService schedulerService) {
        this.service = service;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public ResponseEntity<List<MessageEntity>> getAllMessages() {
        return ResponseEntity.ok(service.getAllMessages());
    }

    @GetMapping("/{weekDay}")
    public ResponseEntity<MessageEntity> getByDay(@PathVariable Integer weekDay) {
        return service.getByWeekDay(weekDay)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MessageEntity> saveMessage(@RequestBody MessageEntity message) {
        try {
            MessageEntity saveMessage = service.saveOrUpdateMessages(message);
            return ResponseEntity.ok(saveMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID id) {
        service.deleteMessageById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trigger")
    public ResponseEntity<Void> triggerDailyMessage() {
        schedulerService.executeDailyMessageTrigger();
        return ResponseEntity.ok().build();
    }
}
