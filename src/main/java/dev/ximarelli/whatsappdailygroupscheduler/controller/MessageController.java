package dev.ximarelli.whatsappdailygroupscheduler.controller;

import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageDto;
import dev.ximarelli.whatsappdailygroupscheduler.domain.AppSettingsInfo;
import dev.ximarelli.whatsappdailygroupscheduler.domain.CronRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.DirectMessageRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.ManualTriggerRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.TargetGroupRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.TimeRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.WhatsAppChatDTO;
import dev.ximarelli.whatsappdailygroupscheduler.service.EvolutionApiProxyService;
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
@CrossOrigin(originPatterns = "*", allowCredentials = "true", allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service;
    private final MessageSchedulerService schedulerService;
    private final EvolutionApiProxyService evolutionApiProxyService;

    public MessageController(MessageService service,
                             MessageSchedulerService schedulerService,
                             EvolutionApiProxyService evolutionApiProxyService) {
        this.service = service;
        this.schedulerService = schedulerService;
        this.evolutionApiProxyService = evolutionApiProxyService;
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

    @PostMapping({"/settings/target-group", "/settings/targetGroup"})
    public ResponseEntity<?> updateTargetGroup(@RequestBody TargetGroupRequest request) {
        try {
            schedulerService.updateTargetGroupJid(request.targetGroupJid());
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

    @PostMapping({"/send", "/send-direct"})
    public ResponseEntity<?> sendDirectMessage(@RequestBody DirectMessageRequest request) {
        try {
            schedulerService.sendDirectMessage(request);
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

    // ==========================================
    // WHATSAPP CHATS (Live from Evolution API)
    // ==========================================

    /**
     * Fetch all WhatsApp chats (groups + contacts) from Evolution API.
     */
    @GetMapping("/whatsapp-chats")
    public ResponseEntity<List<WhatsAppChatDTO>> getAllWhatsAppChats() {
        List<WhatsAppChatDTO> chats = evolutionApiProxyService.fetchAllChats();
        return ResponseEntity.ok(chats);
    }

    /**
     * Fetch only WhatsApp groups from Evolution API.
     */
    @GetMapping("/whatsapp-chats/groups")
    public ResponseEntity<List<WhatsAppChatDTO>> getWhatsAppGroups() {
        List<WhatsAppChatDTO> groups = evolutionApiProxyService.fetchGroupsOnly();
        return ResponseEntity.ok(groups);
    }

    /**
     * Fetch only WhatsApp contacts from Evolution API.
     */
    @GetMapping("/whatsapp-chats/contacts")
    public ResponseEntity<List<WhatsAppChatDTO>> getWhatsAppContacts() {
        List<WhatsAppChatDTO> contacts = evolutionApiProxyService.fetchContactsOnly();
        return ResponseEntity.ok(contacts);
    }

    /**
     * Search WhatsApp chats by name or JID.
     */
    @GetMapping("/whatsapp-chats/search")
    public ResponseEntity<List<WhatsAppChatDTO>> searchWhatsAppChats(
            @RequestParam(required = false, defaultValue = "") String q) {
        List<WhatsAppChatDTO> results = evolutionApiProxyService.searchChats(q);
        return ResponseEntity.ok(results);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
