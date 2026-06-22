package dev.ximarelli.whatsappdailygroupscheduler.Client;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class EvolutionClient {

    private final RestClient restClient;
    private final String instanceName;

    public EvolutionClient(
            @Value("${evolution.api.url}") String apiUrl,
            @Value("${evolution.api.key}") String apiKey,
            @Value("${evolution.instance.name}") String instanceName) {

        this.instanceName = instanceName;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("apikey", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendTextMessage(String groupJid, String textMessage) {
        Map<String, String> payload = new HashMap<>();
        payload.put("number", groupJid);
        payload.put("text", textMessage);

        restClient.post()
                .uri("/message/sendText/{instanceName}", instanceName)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        System.out.println("Message sent successfully to " + groupJid);
    }


}
