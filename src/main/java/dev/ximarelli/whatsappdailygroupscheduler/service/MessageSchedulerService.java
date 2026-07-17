package dev.ximarelli.whatsappdailygroupscheduler.service;

import dev.ximarelli.whatsappdailygroupscheduler.client.EvolutionClient;
import dev.ximarelli.whatsappdailygroupscheduler.domain.AppSettingsInfo;
import dev.ximarelli.whatsappdailygroupscheduler.domain.GroupDto;
import dev.ximarelli.whatsappdailygroupscheduler.domain.GroupEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.DirectMessageRequest;
import dev.ximarelli.whatsappdailygroupscheduler.domain.SettingEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.ManualTriggerRequest;
import dev.ximarelli.whatsappdailygroupscheduler.enums.MessageType;
import dev.ximarelli.whatsappdailygroupscheduler.repository.GroupRepository;
import dev.ximarelli.whatsappdailygroupscheduler.repository.MessageRepository;
import dev.ximarelli.whatsappdailygroupscheduler.repository.SettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class MessageSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(MessageSchedulerService.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String DEFAULT_CRON = "0 0 5 * * *";
    private static final String CRON_SETTING_KEY = "daily_message_cron";

    private final TaskScheduler taskScheduler;
    private final SettingRepository settingRepository;
    private final MessageRepository repository;
    private final GroupRepository groupRepository;
    private final EvolutionClient evolutionClient;
    
    private final String targetGroupJid;
    private final String evolutionApiUrl;
    private final String evolutionInstanceName;

    private volatile ScheduledFuture<?> scheduledTask;

    public MessageSchedulerService(
            TaskScheduler taskScheduler,
            SettingRepository settingRepository,
            MessageRepository repository,
            GroupRepository groupRepository,
            EvolutionClient evolutionClient,
            @Value("${app.whatsapp.target-group-jid}") String targetGroupJid,
            @Value("${evolution.api.url}") String evolutionApiUrl,
            @Value("${evolution.instance.name}") String evolutionInstanceName
    ) {
        this.taskScheduler = taskScheduler;
        this.settingRepository = settingRepository;
        this.repository = repository;
        this.groupRepository = groupRepository;
        this.evolutionClient = evolutionClient;
        this.targetGroupJid = targetGroupJid;
        this.evolutionApiUrl = evolutionApiUrl;
        this.evolutionInstanceName = evolutionInstanceName;
    }

    @PostConstruct
    public void init() {
        scheduleTask();
    }

    public void updateScheduleTime(String cronTime) {
        try {
            new CronTrigger(cronTime, BRAZIL_ZONE);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronTime + ". Detail: " + e.getMessage());
        }

        saveCronSetting(cronTime);
        scheduleTask();
    }

    public void updateScheduleTimeFromTimeStr(String timeStr) {
        LocalTime parsedTime;
        try {
            parsedTime = LocalTime.parse(timeStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + timeStr + ". Must be HH:mm");
        }

        String cronTime = String.format("0 %d %d * * *", parsedTime.getMinute(), parsedTime.getHour());

        saveCronSetting(cronTime);
        scheduleTask();
    }

    private void saveCronSetting(String cronTime) {
        SettingEntity setting = settingRepository.findById(CRON_SETTING_KEY)
                .orElse(new SettingEntity(CRON_SETTING_KEY, cronTime));
        setting.setSettingValue(cronTime);
        settingRepository.save(setting);
    }

    private synchronized void scheduleTask() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }

        String cronTime = getCronTime();

        scheduledTask = taskScheduler.schedule(
                this::executeDailyMessageTrigger,
                new CronTrigger(cronTime, BRAZIL_ZONE)
        );

        log.info("Scheduled daily message trigger with cron '{}' in timezone {}", cronTime, BRAZIL_ZONE);
    }

    public String getCronTime() {
        return settingRepository.findById(CRON_SETTING_KEY)
                .map(SettingEntity::getSettingValue)
                .orElse(DEFAULT_CRON);
    }

    public String getTargetGroupJid() {
        return settingRepository.findById("target_group_jid")
                .map(SettingEntity::getSettingValue)
                .orElse(targetGroupJid);
    }

    public void updateTargetGroupJid(String newTargetGroupJid) {
        if (newTargetGroupJid == null || newTargetGroupJid.isBlank()) {
            throw new IllegalArgumentException("Target Group JID cannot be empty");
        }
        SettingEntity setting = settingRepository.findById("target_group_jid")
                .orElse(new SettingEntity("target_group_jid", newTargetGroupJid));
        setting.setSettingValue(newTargetGroupJid);
        settingRepository.save(setting);
    }

    public AppSettingsInfo getAppSettingsInfo() {
        List<GroupDto> groupDtos = groupRepository.findAll().stream()
                .map(GroupDto::fromEntity)
                .collect(Collectors.toList());
        return new AppSettingsInfo(
                getTargetGroupJid(),
                evolutionApiUrl,
                evolutionInstanceName,
                getCronTime(),
                groupDtos
        );
    }

    public void executeDailyMessageTrigger() {
        log.info("Executing daily message trigger");

        int currentDay = LocalDate.now(BRAZIL_ZONE).getDayOfWeek().getValue();
        Optional<MessageEntity> dailyMessage = repository.findByWeekDayAndIsActiveTrue(currentDay);

        if (dailyMessage.isPresent()) {
            MessageEntity message = dailyMessage.get();
            if (message.getMessageType() == MessageType.TEXT) {
                List<GroupEntity> selectedGroups = groupRepository.findByIsSelectedTrue();
                if (!selectedGroups.isEmpty()) {
                    for (GroupEntity group : selectedGroups) {
                        log.info("Sending scheduled message to selected group: {}", group.getGroupJid());
                        evolutionClient.sendTextMessage(group.getGroupJid(), message.getTextContent());
                    }
                } else {
                    String jid = getTargetGroupJid();
                    log.info("Sending scheduled message to default target group: {}", jid);
                    evolutionClient.sendTextMessage(jid, message.getTextContent());
                }
            } else {
                log.warn("Unsupported message type for day {}: {}", currentDay, message.getMessageType());
            }

        } else {
            log.info("No active message found for day {}", currentDay);
        }
    }

    public void executeManualMessageTrigger(ManualTriggerRequest request) {
        String groupJid = request.targetGroupId() != null && !request.targetGroupId().isBlank()
                ? request.targetGroupId()
                : null;

        String textMessage = request.message();
        if (textMessage == null || textMessage.isBlank()) {
            if (request.weekDay() != null) {
                Optional<MessageEntity> dailyMessage = repository.findByWeekDayAndIsActiveTrue(request.weekDay());
                if (dailyMessage.isPresent()) {
                    textMessage = dailyMessage.get().getTextContent();
                }
            }
        }

        if (textMessage != null && !textMessage.isBlank()) {
            if (groupJid != null) {
                evolutionClient.sendTextMessage(groupJid, textMessage);
            } else {
                List<GroupEntity> selectedGroups = groupRepository.findByIsSelectedTrue();
                if (!selectedGroups.isEmpty()) {
                    for (GroupEntity group : selectedGroups) {
                        evolutionClient.sendTextMessage(group.getGroupJid(), textMessage);
                    }
                } else {
                    evolutionClient.sendTextMessage(getTargetGroupJid(), textMessage);
                }
            }
        } else {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
    }

    public void sendDirectMessage(DirectMessageRequest request) {
        if (request.number() == null || request.number().isBlank()) {
            throw new IllegalArgumentException("Recipient number cannot be empty");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        evolutionClient.sendTextMessage(request.number(), request.message());
    }
}