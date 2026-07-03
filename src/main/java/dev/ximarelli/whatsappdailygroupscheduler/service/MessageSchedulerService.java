package dev.ximarelli.whatsappdailygroupscheduler.service;

import dev.ximarelli.whatsappdailygroupscheduler.client.EvolutionClient;
import dev.ximarelli.whatsappdailygroupscheduler.domain.AppSettingsInfo;
import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.SettingEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.ManualTriggerRequest;
import dev.ximarelli.whatsappdailygroupscheduler.enums.MessageType;
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
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Service
public class MessageSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(MessageSchedulerService.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String DEFAULT_CRON = "0 0 5 * * *";
    private static final String CRON_SETTING_KEY = "daily_message_cron";

    private final TaskScheduler taskScheduler;
    private final SettingRepository settingRepository;
    private final MessageRepository repository;
    private final EvolutionClient evolutionClient;
    
    private final String targetGroupJid;
    private final String evolutionApiUrl;
    private final String evolutionInstanceName;

    private volatile ScheduledFuture<?> scheduledTask;

    public MessageSchedulerService(
            TaskScheduler taskScheduler,
            SettingRepository settingRepository,
            MessageRepository repository,
            EvolutionClient evolutionClient,
            @Value("${app.whatsapp.target-group-jid}") String targetGroupJid,
            @Value("${evolution.api.url}") String evolutionApiUrl,
            @Value("${evolution.instance.name}") String evolutionInstanceName
    ) {
        this.taskScheduler = taskScheduler;
        this.settingRepository = settingRepository;
        this.repository = repository;
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

    public AppSettingsInfo getAppSettingsInfo() {
        return new AppSettingsInfo(
                targetGroupJid,
                evolutionApiUrl,
                evolutionInstanceName,
                getCronTime()
        );
    }

    public void executeDailyMessageTrigger() {
        log.info("Executing daily message trigger");

        int currentDay = LocalDate.now(BRAZIL_ZONE).getDayOfWeek().getValue();
        Optional<MessageEntity> dailyMessage = repository.findByWeekDayAndIsActiveTrue(currentDay);

        if (dailyMessage.isPresent()) {
            MessageEntity message = dailyMessage.get();
            if (message.getMessageType() == MessageType.TEXT) {
                evolutionClient.sendTextMessage(targetGroupJid, message.getTextContent());
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
                : targetGroupJid;

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
            evolutionClient.sendTextMessage(groupJid, textMessage);
        } else {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
    }
}