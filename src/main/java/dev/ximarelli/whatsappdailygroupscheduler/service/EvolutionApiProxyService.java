package dev.ximarelli.whatsappdailygroupscheduler.service;

import dev.ximarelli.whatsappdailygroupscheduler.domain.WhatsAppChatDTO;
import dev.ximarelli.whatsappdailygroupscheduler.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Proxies the Evolution API to fetch live WhatsApp chats (groups + contacts)
 * for the frontend ChatPicker, merging in "favorite" flags stored locally.
 */
@Service
public class EvolutionApiProxyService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiProxyService.class);

    private final GroupRepository groupRepository;
    private final RestClient restClient;
    private final String instanceName;

    public EvolutionApiProxyService(
            GroupRepository groupRepository,
            @Value("${evolution.api.url}") String apiUrl,
            @Value("${evolution.api.key}") String apiKey,
            @Value("${evolution.instance.name}") String instanceName) {

        this.groupRepository = groupRepository;
        this.instanceName = instanceName;
        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(apiUrl))
                .defaultHeader("apikey", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Fetch all WhatsApp chats (groups + contacts) from Evolution API.
     * Calls: POST /chat/findChats/{instanceName}
     */
    public List<WhatsAppChatDTO> fetchAllChats() {
        Map<String, Object> body = new HashMap<>();
        body.put("take", 200); // Limit results

        try {
            Object raw = restClient.post()
                    .uri("/chat/findChats/{instanceName}", instanceName)
                    .body(body)
                    .retrieve()
                    .body(Object.class);

            List<Map<String, Object>> chats = extractChatList(raw);
            if (chats.isEmpty()) return Collections.emptyList();

            Set<String> favoriteJids = getFavoriteJids();

            return chats.stream()
                    .map(chat -> mapToWhatsAppChatDTO(chat, favoriteJids))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch chats from Evolution API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetch only WhatsApp groups.
     */
    public List<WhatsAppChatDTO> fetchGroupsOnly() {
        return fetchAllChats().stream()
                .filter(c -> "GROUP".equals(c.type()))
                .collect(Collectors.toList());
    }

    /**
     * Fetch only WhatsApp contacts.
     */
    public List<WhatsAppChatDTO> fetchContactsOnly() {
        return fetchAllChats().stream()
                .filter(c -> "CONTACT".equals(c.type()))
                .collect(Collectors.toList());
    }

    /**
     * Search chats by name or JID (client-side filter for simplicity).
     */
    public List<WhatsAppChatDTO> searchChats(String query) {
        if (query == null || query.trim().isEmpty()) {
            return fetchAllChats();
        }

        String lowerQuery = query.toLowerCase();
        return fetchAllChats().stream()
                .filter(c ->
                        (c.name() != null && c.name().toLowerCase().contains(lowerQuery)) ||
                        c.jid().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // --- Helper Methods ---

    /**
     * The Evolution API may return the chat list directly or wrapped in
     * {"status": ..., "response": [...] | {"data": [...]}} depending on version.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractChatList(Object raw) {
        if (raw instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        if (raw instanceof Map<?, ?> map) {
            Object response = map.get("response");
            if (response instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
            if (response instanceof Map<?, ?> responseMap) {
                Object data = responseMap.get("data");
                if (data instanceof List<?> list) {
                    return (List<Map<String, Object>>) list;
                }
            }
            Object data = map.get("data");
            if (data instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
        }
        log.warn("Unexpected Evolution API findChats response shape: {}", raw == null ? "null" : raw.getClass());
        return Collections.emptyList();
    }

    private WhatsAppChatDTO mapToWhatsAppChatDTO(Map<String, Object> chat, Set<String> favoriteJids) {
        try {
            String jid = firstStringValue(chat.get("id"), chat.get("remoteJid"), chat.get("_serialized"));
            if (jid == null || jid.isBlank()) return null;

            // Determine type from JID
            String type = jid.contains("@g.us") ? "GROUP" : "CONTACT";

            // Extract name (group subject or contact pushName)
            String name = firstStringValue(chat.get("name"), chat.get("subject"));
            if (name == null) {
                name = pushNameFromSender(chat.get("sender"));
            }
            if (name == null) {
                name = firstStringValue(chat.get("pushName"));
            }
            if (name == null) {
                name = jid;
            }

            // Profile picture URL (if available in response)
            String profilePicUrl = firstStringValue(chat.get("profilePicUrl"));

            // Participants count (groups only)
            Integer participantsCount = null;
            if ("GROUP".equals(type) && chat.get("participants") instanceof List<?> participants) {
                participantsCount = participants.size();
            }

            boolean isFavorite = favoriteJids.contains(jid);

            return new WhatsAppChatDTO(jid, name, type, profilePicUrl, participantsCount, isFavorite);
        } catch (Exception e) {
            log.warn("Failed to map chat: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getFavoriteJids() {
        return groupRepository.findAll().stream()
                .filter(g -> g.isFavorite())
                .map(g -> g.getGroupJid())
                .collect(Collectors.toSet());
    }

    private static String pushNameFromSender(Object sender) {
        if (sender instanceof Map<?, ?> senderMap) {
            if (senderMap.get("name") instanceof String s && !s.isBlank()) return s;
            if (senderMap.get("pushName") instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    private static String firstStringValue(Object... values) {
        for (Object v : values) {
            if (v instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    private static String normalizeBaseUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalArgumentException("evolution.api.url is not set or is empty");
        }
        String url = apiUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
