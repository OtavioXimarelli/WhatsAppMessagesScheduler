# Backend Changes: WhatsApp Chat Picker Integration

This document describes the backend changes needed to support the new ChatPicker feature, which fetches real WhatsApp chats/contacts from the Evolution API and presents them in an easy-to-use picker UI.

---

## 1. New DTO: `WhatsAppChatDto`

Create this new record class in your DTO package:

```java
package com.ximarelli.whatsapp.dto;

/**
 * Represents a chat or contact fetched from the WhatsApp instance via Evolution API.
 * Used by the frontend ChatPicker component for easy group/contact selection.
 */
public record WhatsAppChatDto(
    String jid,              // WhatsApp JID: "120363...@g.us" (group) or "5511...@s.whatsapp.net" (contact)
    String name,             // Display name: group subject or contact pushName
    String type,             // "GROUP" or "CONTACT"
    String profilePicUrl,    // Profile picture URL (nullable)
    Integer participantsCount, // Number of participants (groups only, nullable)
    Boolean isFavorite       // Whether this chat is starred as favorite (merged from local GroupDto)
) {}
```

---

## 2. Update `GroupDto` — Add `isFavorite` field

```java
public record GroupDto(
    UUID id,
    String name,
    String groupJid,
    Boolean isSelected,
    Boolean isFavorite   // NEW FIELD — whether this group is starred for quick access
) {}
```

**Migration:** Add `is_favorite` column to the `groups` table (boolean, default false):
```sql
ALTER TABLE groups ADD COLUMN is_favorite BOOLEAN DEFAULT FALSE;
```

---

## 3. New Service: `EvolutionApiProxyService`

This service makes HTTP calls to the Evolution API to fetch real WhatsApp data.

```java
package com.ximarelli.whatsapp.service;

import com.ximarelli.whatsapp.dto.WhatsAppChatDto;
import com.ximarelli.whatsapp.dto.GroupDto;
import com.ximarelli.whatsapp.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvolutionApiProxyService {

    @Value("${evolution.api.url}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key}")
    private String evolutionApiKey;

    @Value("${evolution.instance.name}")
    private String instanceName;

    private final GroupRepository groupRepository;
    private final RestTemplate restTemplate;

    public EvolutionApiProxyService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetch all WhatsApp chats (groups + contacts) from Evolution API.
     * Calls: POST /chat/findChats/{instanceName}
     */
    public List<WhatsAppChatDto> fetchAllChats() {
        String url = evolutionApiUrl + "/chat/findChats/" + instanceName;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", evolutionApiKey);
        
        // Empty body or query filters
        Map<String, Object> body = new HashMap<>();
        body.put("take", 200); // Limit results
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<List<Map>> response = restTemplate.exchange(
                url, HttpMethod.POST, request, List.class
            );
            
            List<Map> chats = response.getBody();
            if (chats == null) return Collections.emptyList();
            
            // Get favorites from local DB
            Set<String> favoriteJids = getFavoriteJids();
            
            return chats.stream()
                .map(chat -> mapToWhatsAppChatDto(chat, favoriteJids))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            // Log error and return empty list
            System.err.println("Failed to fetch chats from Evolution API: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch only WhatsApp groups.
     */
    public List<WhatsAppChatDto> fetchGroupsOnly() {
        return fetchAllChats().stream()
            .filter(c -> "GROUP".equals(c.type()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch only WhatsApp contacts.
     */
    public List<WhatsAppChatDto> fetchContactsOnly() {
        return fetchAllChats().stream()
            .filter(c -> "CONTACT".equals(c.type()))
            .collect(Collectors.toList());
    }

    /**
     * Search chats by name (client-side filter for simplicity).
     */
    public List<WhatsAppChatDto> searchChats(String query) {
        if (query == null || query.trim().isEmpty()) {
            return fetchAllChats();
        }
        
        String lowerQuery = query.toLowerCase();
        return fetchAllChats().stream()
            .filter(c -> 
                (c.name() != null && c.name().toLowerCase().contains(lowerQuery)) ||
                c.jid().toLowerCase().contains(lowerQuery)
            )
            .collect(Collectors.toList());
    }

    // --- Helper Methods ---

    private WhatsAppChatDto mapToWhatsAppChatDto(Map chat, Set<String> favoriteJids) {
        try {
            String jid = (String) chat.get("id"); // or "remoteJid" depending on Evolution API response
            if (jid == null) return null;
            
            // Determine type from JID
            String type = jid.contains("@g.us") ? "GROUP" : "CONTACT";
            
            // Extract name (group subject or contact pushName)
            String name = (String) chat.getOrDefault("name", 
                        chat.getOrDefault("subject", 
                        chat.getOrDefault("pushName", jid)));
            
            // Profile picture URL (if available in response)
            String profilePicUrl = (String) chat.get("profilePicUrl");
            
            // Participants count (groups only)
            Integer participantsCount = null;
            if ("GROUP".equals(type) && chat.containsKey("participants")) {
                Object participants = chat.get("participants");
                if (participants instanceof List) {
                    participantsCount = ((List<?>) participants).size();
                }
            }
            
            Boolean isFavorite = favoriteJids.contains(jid);
            
            return new WhatsAppChatDto(jid, name, type, profilePicUrl, participantsCount, isFavorite);
        } catch (Exception e) {
            System.err.println("Failed to map chat: " + e.getMessage());
            return null;
        }
    }

    private Set<String> getFavoriteJids() {
        return groupRepository.findAll().stream()
            .filter(g -> Boolean.TRUE.equals(g.getIsFavorite()))
            .map(g -> g.getGroupJid())
            .collect(Collectors.toSet());
    }
}
```

---

## 4. New Controller Endpoints

Add these endpoints to your `MessageController`:

```java
// ==========================================
// 6. WHATSAPP CHATS (Live from Evolution API)
// ==========================================

@Operation(summary = "Fetch all WhatsApp chats from Evolution API")
@GetMapping("/whatsapp-chats")
public ResponseEntity<List<WhatsAppChatDto>> getAllWhatsAppChats() {
    List<WhatsAppChatDto> chats = evolutionApiProxyService.fetchAllChats();
    return ResponseEntity.ok(chats);
}

@Operation(summary = "Fetch only WhatsApp groups from Evolution API")
@GetMapping("/whatsapp-chats/groups")
public ResponseEntity<List<WhatsAppChatDto>> getWhatsAppGroups() {
    List<WhatsAppChatDto> groups = evolutionApiProxyService.fetchGroupsOnly();
    return ResponseEntity.ok(groups);
}

@Operation(summary = "Fetch only WhatsApp contacts from Evolution API")
@GetMapping("/whatsapp-chats/contacts")
public ResponseEntity<List<WhatsAppChatDto>> getWhatsAppContacts() {
    List<WhatsAppChatDto> contacts = evolutionApiProxyService.fetchContactsOnly();
    return ResponseEntity.ok(contacts);
}

@Operation(summary = "Search WhatsApp chats by name")
@GetMapping("/whatsapp-chats/search")
public ResponseEntity<List<WhatsAppChatDto>> searchWhatsAppChats(
        @RequestParam(required = false, defaultValue = "") String q) {
    List<WhatsAppChatDto> results = evolutionApiProxyService.searchChats(q);
    return ResponseEntity.ok(results);
}
```

---

## 5. Update Group Entity & Repository

Add `isFavorite` field to your `Group` entity:

```java
@Entity
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    
    @Column(unique = true)
    private String groupJid;
    
    private Boolean isSelected;
    
    @Column(columnDefinition = "boolean default false")
    private Boolean isFavorite;  // NEW FIELD
    
    // Getters and setters...
}
```

---

## 6. Update Group CRUD Endpoints

Update the existing group endpoints to handle `isFavorite`:

```java
@PostMapping("/groups")
public ResponseEntity<GroupDto> saveGroup(@RequestBody GroupDto request) {
    // Convert DTO to entity, save, convert back to DTO
    Group entity = groupMapper.toEntity(request);
    Group saved = groupRepository.save(entity);
    return ResponseEntity.ok(groupMapper.toDto(saved));
}

@PostMapping("/groups/{id}/favorite")
@ResponseStatus(HttpStatus.OK)
public ResponseEntity<GroupDto> toggleFavorite(@PathVariable UUID id) {
    Group group = groupRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    
    group.setIsFavorite(!Boolean.TRUE.equals(group.getIsFavorite()));
    Group updated = groupRepository.save(group);
    
    return ResponseEntity.ok(groupMapper.toDto(updated));
}
```

---

## 7. Frontend API Contract Update

Add these new endpoints to your `FRONTEND_API_CONTRACT.md`:

### 6. WhatsApp Chats (Live from Evolution API)

#### `GET /api/messages/whatsapp-chats`
Fetches all WhatsApp chats (groups + contacts) from the connected Evolution API instance.
* **Response `200 OK`:**
  ```json
  [
    {
      "jid": "120363043837472938@g.us",
      "name": "Engineering Team",
      "type": "GROUP",
      "profilePicUrl": "https://...",
      "participantsCount": 15,
      "isFavorite": true
    },
    {
      "jid": "5511999999999@s.whatsapp.net",
      "name": "John Doe",
      "type": "CONTACT",
      "profilePicUrl": null,
      "participantsCount": null,
      "isFavorite": false
    }
  ]
  ```

#### `GET /api/messages/whatsapp-chats/groups`
Fetches only WhatsApp groups.

#### `GET /api/messages/whatsapp-chats/contacts`
Fetches only WhatsApp contacts.

#### `GET /api/messages/whatsapp-chats/search?q={query}`
Searches WhatsApp chats by name or JID.
* **Query Parameter:** `q` (search term)
* **Response `200 OK`:** Same format as `/whatsapp-chats`

---

## 8. Configuration

Make sure your `application.yml` or `application.properties` has:

```yaml
evolution:
  api:
    url: ${EVOLUTION_API_URL:http://localhost:8080}
    key: ${EVOLUTION_API_KEY:your-api-key-here}
  instance:
    name: ${EVOLUTION_INSTANCE_NAME:my-whatsapp-instance}
```

---

## 9. Testing

After implementing these changes, test the new endpoints:

```bash
# Fetch all chats
curl -X GET "http://localhost:8080/api/messages/whatsapp-chats" \
  -H "Content-Type: application/json"

# Fetch only groups
curl -X GET "http://localhost:8080/api/messages/whatsapp-chats/groups" \
  -H "Content-Type: application/json"

# Search chats
curl -X GET "http://localhost:8080/api/messages/whatsapp-chats/search?q=engineering" \
  -H "Content-Type: application/json"
```

---

## 10. Summary of Changes

| File | Change |
|---|---|
| `WhatsAppChatDto.java` | **NEW** — DTO for WhatsApp chats |
| `GroupDto.java` | Add `isFavorite` field |
| `Group.java` (entity) | Add `isFavorite` column |
| `EvolutionApiProxyService.java` | **NEW** — Service to call Evolution API |
| `MessageController.java` | Add 4 new endpoints for `/whatsapp-chats` |
| `application.yml` | Add `evolution.api.url`, `evolution.api.key`, `evolution.instance.name` |
| Database | Add `is_favorite` column to `groups` table |

---

## 11. Next Steps (Optional Enhancements)

1. **Profile Pictures:** Fetch profile pictures from Evolution API using `/chat/fetchProfilePicture/{instanceName}`
2. **Caching:** Cache the chat list for 5-10 minutes to avoid hitting Evolution API too frequently
3. **Pagination:** Add pagination support if you have hundreds of chats
4. **Webhook Updates:** Set up Evolution API webhooks to receive real-time updates when new chats are added

---

With these backend changes, the frontend ChatPicker will work seamlessly and allow users to easily select WhatsApp groups and contacts without manually typing JIDs! 🎉
