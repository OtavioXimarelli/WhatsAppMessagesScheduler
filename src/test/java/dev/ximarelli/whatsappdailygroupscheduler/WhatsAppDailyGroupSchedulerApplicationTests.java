package dev.ximarelli.whatsappdailygroupscheduler;

import tools.jackson.databind.ObjectMapper;
import dev.ximarelli.whatsappdailygroupscheduler.client.EvolutionClient;
import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.MessageDto;
import dev.ximarelli.whatsappdailygroupscheduler.enums.MessageType;
import dev.ximarelli.whatsappdailygroupscheduler.repository.MessageRepository;
import dev.ximarelli.whatsappdailygroupscheduler.repository.SettingRepository;
import dev.ximarelli.whatsappdailygroupscheduler.service.MessageSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WhatsAppDailyGroupSchedulerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private MessageSchedulerService schedulerService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EvolutionClient evolutionClient;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        settingRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertNotNull(schedulerService);
    }

    @Test
    void testCreateAndGetMessage() throws Exception {
        MessageDto message = new MessageDto(null, 3, MessageType.TEXT, "Hello Wednesday!", true);

        // POST message
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekDay").value(3))
                .andExpect(jsonPath("$.textContent").value("Hello Wednesday!"))
                .andExpect(jsonPath("$.messageType").value("TEXT"));

        // GET message by weekDay
        mockMvc.perform(get("/api/messages/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekDay").value(3))
                .andExpect(jsonPath("$.textContent").value("Hello Wednesday!"));
    }

    @Test
    void testDeleteMessage() throws Exception {
        MessageEntity message = new MessageEntity(4, MessageType.TEXT, "Hello Thursday!", true);
        MessageEntity saved = messageRepository.save(message);

        mockMvc.perform(delete("/api/messages/" + saved.getId()))
                .andExpect(status().isNoContent());

        Optional<MessageEntity> deleted = messageRepository.findById(saved.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    void testDeleteNonExistentMessage() throws Exception {
        mockMvc.perform(delete("/api/messages/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testSchedulerTriggersCorrectly() {
        java.time.ZoneId brazilZone = java.time.ZoneId.of("America/Sao_Paulo");
        int currentDay = LocalDate.now(brazilZone).getDayOfWeek().getValue();
        String expectedMessage = "Good morning group! Today is " + LocalDate.now(brazilZone).getDayOfWeek();

        MessageEntity msg = new MessageEntity(currentDay, MessageType.TEXT, expectedMessage, true);
        messageRepository.save(msg);

        schedulerService.executeDailyMessageTrigger();

        verify(evolutionClient, times(1)).sendTextMessage(any(), eq(expectedMessage));
    }

    @Test
    void testGetSettings() throws Exception {
        mockMvc.perform(get("/api/messages/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetGroupJid").value("dummygroup"))
                .andExpect(jsonPath("$.evolutionApiUrl").value("http://localhost:8080"))
                .andExpect(jsonPath("$.evolutionInstanceName").value("dummyinstance"))
                .andExpect(jsonPath("$.cronTime").value("0 0 5 * * *"));
    }

    @Test
    void testUpdateCronTime() throws Exception {
        String newCron = "0 30 6 * * *";
        mockMvc.perform(post("/api/messages/settings/cron")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cronTime\":\"" + newCron + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/messages/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronTime").value(newCron));
    }

    @Test
    void testUpdateScheduleTimeFromTimeStr() throws Exception {
        String timeStr = "08:30";
        mockMvc.perform(post("/api/messages/settings/time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"time\":\"" + timeStr + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/messages/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronTime").value("0 30 8 * * *"));
    }

    @Test
    void testTriggerManualMessage() throws Exception {
        String testMessage = "Manual test message";
        String customGroupJid = "customgroup@g.us";

        mockMvc.perform(post("/api/messages/trigger/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekDay\":null,\"message\":\"" + testMessage + "\",\"targetGroupId\":\"" + customGroupJid + "\"}"))
                .andExpect(status().isOk());

        verify(evolutionClient, times(1)).sendTextMessage(eq(customGroupJid), eq(testMessage));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/messages/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("CONNECTED"));
    }
}
