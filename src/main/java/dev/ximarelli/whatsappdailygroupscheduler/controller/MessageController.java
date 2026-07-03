package dev.ximarelli.whatsappdailygroupscheduler.controller;

import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageDto;
import dev.ximarelli.whatsappdailygroupscheduler.domain.AppSettingsInfo;
import dev.ximarelli.whatsappdailygroupscheduler.domain.CronRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.ManualTriggerRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.TimeRequest;
import dev.ximarelli.whatsappdailygroupscheduler.service.MessageSchedulerService;
import dev.ximarelli.whatsappdailygroupscheduler.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service;
    private final MessageSchedulerService schedulerService;

    public MessageController(MessageService service, MessageSchedulerService schedulerService) {
        this.service = service;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public ResponseEntity<List<MessageDto>> getAllMessages() {
        List<MessageDto> dtos = service.getAllMessages().stream()
                .map(MessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{weekDay}")
    public ResponseEntity<MessageDto> getByDay(@PathVariable Integer weekDay) {
        return service.getByWeekDay(weekDay)
                .map(MessageDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> saveMessage(@RequestBody MessageDto messageDto) {
        try {
            MessageEntity entityToSave = messageDto.toEntity();
            MessageEntity savedEntity = service.saveOrUpdateMessages(entityToSave);
            return ResponseEntity.ok(MessageDto.fromEntity(savedEntity));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

    @GetMapping("/settings")
    public ResponseEntity<AppSettingsInfo> getSettings() {
        AppSettingsInfo info = schedulerService.getAppSettingsInfo();
        return ResponseEntity.ok(info);
    }

    @PostMapping("/settings/cron")
    public ResponseEntity<?> updateCronTime(@RequestBody CronRequest request) {
        try {
            schedulerService.updateScheduleTime(request.cronTime());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/settings/time")
    public ResponseEntity<?> updateScheduleTimeFromTimeStr(@RequestBody TimeRequest request) {
        try {
            schedulerService.updateScheduleTimeFromTimeStr(request.time());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trigger/manual")
    public ResponseEntity<?> triggerManualMessage(@RequestBody ManualTriggerRequest request) {
        try {
            schedulerService.executeManualMessageTrigger(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        try {
            service.getAllMessages();
            return ResponseEntity.ok(Map.of("status", "UP", "database", "CONNECTED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "database", "DISCONNECTED", "error", e.getMessage()));
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
