package dev.ximarelli.whatsappdailygroupscheduler.Service;


import dev.ximarelli.whatsappdailygroupscheduler.Client.EvolutionClient;
import dev.ximarelli.whatsappdailygroupscheduler.Domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.Enum.MessageType;
import dev.ximarelli.whatsappdailygroupscheduler.Repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class MessageSchedulerService {

    private final MessageRepository repository;
    private final EvolutionClient evolutionClient;
    private final String targetGroupJid;

    public MessageSchedulerService(
            MessageRepository repository,
            EvolutionClient evolutionClient,
            @Value("${app.whatsapp.target-group-jid}") String targetGroupJid
    ) {

        this.repository = repository;
        this.evolutionClient = evolutionClient;
        this.targetGroupJid = targetGroupJid;
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "America/Sao_Paulo")
    public void executeDailyMessageTrigger() {
        System.out.println("executing cron job");

        int currentDay = LocalDate.now().getDayOfWeek().getValue();
        Optional<MessageEntity> dailyMessage = repository.findByWeekDayAndIsActiveTrue(currentDay);

        if (dailyMessage.isPresent()) {
            MessageEntity message = dailyMessage.get();
            if (message.getMessageType() == MessageType.TEXT) {
                evolutionClient.sendTextMessage(targetGroupJid, message.getTextContent());
            } else {
                System.out.println("Unsupported message type for the current day: " + message.getMessageType());
            }

        } else {
            System.out.println("No active message found for the current day: " + currentDay);
        }


    }
}