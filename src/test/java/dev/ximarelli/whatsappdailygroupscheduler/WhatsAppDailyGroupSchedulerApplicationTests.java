package dev.ximarelli.whatsappdailygroupscheduler;

import tools.jackson.databind.ObjectMapper;
import dev.ximarelli.whatsappdailygroupscheduler.Client.EvolutionClient;
import dev.ximarelli.whatsappdailygroupscheduler.Domain.MessageEntity;
import dev.ximarelli.whatsappdailygroupscheduler.Enum.MessageType;
import dev.ximarelli.whatsappdailygroupscheduler.Repositories.MessageRepository;
import dev.ximarelli.whatsappdailygroupscheduler.Service.MessageSchedulerService;
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
    private MessageSchedulerService schedulerService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EvolutionClient evolutionClient;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertNotNull(schedulerService);
    }

    @Test
    void testCreateAndGetMessage() throws Exception {
        MessageEntity message = new MessageEntity(3, MessageType.TEXT, "Hello Wednesday!", true);

        // POST message
        mockMvc.perform(post("/api/messages")

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekDay").value(3))
                .andExpect(jsonPath("$.textContent").value("Hello Wednesday!"))
                .andExpect(jsonPath("$.messageType").value("TEXT"));

        // GET message by weekDay
        mockMvc.perform(get("/api/messages/3")
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekDay").value(3))
                .andExpect(jsonPath("$.textContent").value("Hello Wednesday!"));
    }

    @Test
    void testDeleteMessage() throws Exception {
        MessageEntity message = new MessageEntity(4, MessageType.TEXT, "Hello Thursday!", true);
        MessageEntity saved = messageRepository.save(message);

        mockMvc.perform(delete("/api/messages/" + saved.getId())
)
                .andExpect(status().isNoContent());

        Optional<MessageEntity> deleted = messageRepository.findById(saved.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    void testSchedulerTriggersCorrectly() {
        // Find current day of week (1 to 7)
        int currentDay = LocalDate.now().getDayOfWeek().getValue();
        String expectedMessage = "Good morning group! Today is " + LocalDate.now().getDayOfWeek();

        // Create and save message for current day
        MessageEntity msg = new MessageEntity(currentDay, MessageType.TEXT, expectedMessage, true);
        messageRepository.save(msg);

        // Trigger scheduler trigger method manually
        schedulerService.executeDailyMessageTrigger();

        // Verify that EvolutionClient was called to send the text message
        verify(evolutionClient, times(1)).sendTextMessage(any(), eq(expectedMessage));
    }
}
