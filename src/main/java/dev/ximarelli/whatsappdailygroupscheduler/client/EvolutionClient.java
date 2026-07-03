package dev.ximarelli.whatsappdailygroupscheduler.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Service
public class EvolutionClient {

    private static final Logger log = LoggerFactory.getLogger(EvolutionClient.class);

    private final RestClient restClient;
    private final String instanceName;

    public EvolutionClient(
            @Value("${evolution.api.url}") String apiUrl,
            @Value("${evolution.api.key}") String apiKey,
            @Value("${evolution.instance.name}") String instanceName) {

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalArgumentException("evolution.api.url environment variable is not set or is empty");
        }

        String formattedApiUrl = apiUrl.trim();
        if (!formattedApiUrl.startsWith("http://") && !formattedApiUrl.startsWith("https://")) {
            formattedApiUrl = "http://" + formattedApiUrl;
        }
        if (formattedApiUrl.endsWith("/")) {
            formattedApiUrl = formattedApiUrl.substring(0, formattedApiUrl.length() - 1);
        }

        this.instanceName = instanceName;
        this.restClient = RestClient.builder()
                .baseUrl(formattedApiUrl)
                .defaultHeader("apikey", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendTextMessage(String groupJid, String textMessage) {
        Map<String, String> payload = new HashMap<>();
        payload.put("number", groupJid);
        payload.put("text", textMessage);

        try {
            restClient.post()
                    .uri("/message/sendText/{instanceName}", instanceName)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Message sent successfully to {}", groupJid);
        } catch (RestClientException e) {
            log.error("Failed to send message to {}: {}", groupJid, e.getMessage(), e);
            throw e;
        }
    }
}
