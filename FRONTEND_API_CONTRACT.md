# WhatsApp Daily Group Scheduler — Frontend API Contract

This document serves as the official integration contract between the **Frontend** (`https://daily.ximarelli.dev`) and the **Spring Boot Backend REST API**.

---

## 1. Overview & Base Configuration

* **Base URL Path:** `/api/messages`
* **Content-Type:** `application/json`
* **CORS Policy:** Fully configured globally with `allowCredentials: true` and `allowedOriginPatterns: "*"` (explicitly allowing requests from `https://daily.ximarelli.dev` and local development ports `http://localhost:3000` / `http://localhost:5173`).
* **Authentication/Headers:** No bearer tokens or authentication headers are required by the backend (`Spring Security` is removed). When fetching with credentials, ensure `withCredentials: true` is passed in Axios or `credentials: 'include'` in Fetch API.

---

## 2. TypeScript Domain Models & Interfaces

Copy these TypeScript interfaces directly into your frontend application (e.g., `src/types/api.ts`):

```typescript
/**
 * Day of week representation:
 * 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday
 */
export type WeekDay = 1 | 2 | 3 | 4 | 5 | 6 | 7;

export type MessageType = 'TEXT' | 'AUDIO';

export interface MessageDto {
  /** Unique UUID of the message (can be null when creating a new message) */
  id?: string | null;
  /** Day of week (1 to 7) */
  weekDay: WeekDay | number;
  /** Type of message broadcasted */
  messageType: MessageType;
  /** Text content to send via WhatsApp */
  textContent: string;
  /** Whether the message is active for scheduling */
  isActive: boolean;
}

export interface GroupDto {
  /** Unique UUID of the group/chat */
  id?: string | null;
  /** Friendly label or name for the group/chat (e.g., 'Engineering Team') */
  name?: string | null;
  /** Target WhatsApp group or chat JID (e.g., '120363043837472938@g.us' or '5511999999999@s.whatsapp.net') */
  groupJid: string;
  /** Whether this group ID is currently active/selected to receive broadcasts */
  isSelected?: boolean;
}

export interface AppSettingsInfo {
  /** Target WhatsApp group JID (e.g., '120363043837472938@g.us') */
  targetGroupJid: String;
  /** Evolution API server URL */
  evolutionApiUrl: string;
  /** Evolution API instance name */
  evolutionInstanceName: string;
  /** Current active cron expression (e.g., '0 0 5 * * *') */
  cronTime: string;
  /** List of all saved WhatsApp groups and chat targets */
  groups?: GroupDto[];
}

export interface TargetGroupRequest {
  /** The target group JID to set as active default */
  targetGroupJid: string;
}

export interface DirectMessageRequest {
  /** Target WhatsApp phone number or JID (e.g., '5511999999999' or '5511999999999@s.whatsapp.net') */
  number: string;
  /** Text message content to send immediately */
  message: string;
}

export interface CronRequest {
  /** Standard 6-field Spring cron expression (e.g., '0 30 8 * * *') */
  cronTime: string;
}

export interface TimeRequest {
  /** Simple time format 'HH:mm' (e.g., '08:30') */
  time: string;
}

export interface ManualTriggerRequest {
  /** Optional target day of week to trigger its scheduled message */
  weekDay?: number | null;
  /** Optional custom text message override to send immediately */
  message?: string | null;
  /** Optional target group JID override (defaults to configured targetGroupJid if omitted) */
  targetGroupId?: string | null;
}

export interface HealthResponse {
  status: 'UP' | 'DOWN';
  database: 'CONNECTED' | 'DISCONNECTED';
  error?: string;
}

export interface ApiErrorResponse {
  error: string;
}
```

---

## 3. Endpoints Reference Table

| HTTP Method | Endpoint | Description | Request Body | Response Status & Body |
| :--- | :--- | :--- | :--- | :--- |
| **GET** | `/api/messages` | List all scheduled messages | — | `200 OK` (`MessageDto[]`) |
| **GET** | `/api/messages/{weekDay}` | Get message by weekday `1-7` | — | `200 OK` (`MessageDto`) / `404 Not Found` |
| **POST** | `/api/messages` | Create or update a message | `MessageDto` | `200 OK` (`MessageDto`) / `400 Bad Request` |
| **DELETE** | `/api/messages/{id}` | Delete message by ID (`UUID`) | — | `204 No Content` |
| **POST** | `/api/messages/trigger` | Trigger today's daily message now | — | `200 OK` (No Content) |
| **POST** | `/api/messages/trigger/manual`| Manual broadcast trigger | `ManualTriggerRequest`| `200 OK` (No Content) / `400 Bad Request` |
| **POST** | `/api/messages/send` *(or `/send-direct`)* | Direct message to specific number/chat | `DirectMessageRequest` | `200 OK` (No Content) / `400 Bad Request` |
| **GET** | `/api/messages/settings` | Get app settings, cron schedule & groups list | — | `200 OK` (`AppSettingsInfo`) |
| **POST** | `/api/messages/settings/cron` | Update schedule via cron | `CronRequest` | `200 OK` (No Content) / `400 Bad Request` |
| **POST** | `/api/messages/settings/time` | Update schedule via `HH:mm` | `TimeRequest` | `200 OK` (No Content) / `400 Bad Request` |
| **POST** | `/api/messages/settings/target-group` | Update default target group JID | `TargetGroupRequest` | `200 OK` (No Content) / `400 Bad Request` |
| **GET** | `/api/messages/groups` *(or `/api/groups`)* | List all saved groups/chat targets | — | `200 OK` (`GroupDto[]`) |
| **GET** | `/api/messages/groups/{id}` | Get group/chat ID by UUID | — | `200 OK` (`GroupDto`) / `404 Not Found` |
| **POST** | `/api/messages/groups` *(or `/api/groups`)* | Create or update a group/chat target | `GroupDto` | `200 OK` (`GroupDto`) / `400 Bad Request` |
| **DELETE** | `/api/messages/groups/{id}` | Delete a group/chat target | — | `204 No Content` |
| **POST** | `/api/messages/groups/{id}/select` | Select/activate a group target to use | — (`?exclusive=true/false`) | `200 OK` (`GroupDto`) / `400 Bad Request` |
| **POST** | `/api/messages/groups/{id}/deselect` | Deselect a group target | — | `200 OK` (`GroupDto`) / `400 Bad Request` |
| **GET** | `/api/messages/health` | Check API & database health | — | `200 OK` / `503 Service Unavailable` |

---

## 4. Detailed Endpoint Specifications

### 4.1 Messages Management

#### `GET /api/messages`
Retrieves all stored daily messages.
* **Response `200 OK`:**
  ```json
  [
    {
      "id": "c1a3e878-5a21-4f11-9a70-8b1b8b64e0f1",
      "weekDay": 1,
      "messageType": "TEXT",
      "textContent": "Good morning team! Happy Monday! ☀️",
      "isActive": true
    },
    {
      "id": "e4f8a92b-81d3-4f90-a612-3c8166c3a9f9",
      "weekDay": 5,
      "messageType": "TEXT",
      "textContent": "Happy Friday everyone! Please submit your weekly logs. 🎉",
      "isActive": true
    }
  ]
  ```

---

#### `GET /api/messages/{weekDay}`
Retrieves the message configured for a specific day of the week.
* **Path Parameter:** `weekDay` (`1` = Monday ... `7` = Sunday).
* **Response `200 OK`:**
  ```json
  {
    "id": "c1a3e878-5a21-4f11-9a70-8b1b8b64e0f1",
    "weekDay": 1,
    "messageType": "TEXT",
    "textContent": "Good morning team! Happy Monday! ☀️",
    "isActive": true
  }
  ```
* **Response `404 Not Found`:** Returned when no message exists for that weekday.
  ```json
  {
    "error": "No message configured for day 3"
  }
  ```

---

#### `POST /api/messages`
Creates a new message or updates an existing message (upsert behavior). If a message with the same `weekDay` already exists, it updates that entry.
* **Request Body (`MessageDto`):**
  ```json
  {
    "id": null,
    "weekDay": 2,
    "messageType": "TEXT",
    "textContent": "Good morning! Happy Tuesday! 🚀",
    "isActive": true
  }
  ```
* **Response `200 OK`:** Returns the saved/updated `MessageDto` with assigned `id`.
  ```json
  {
    "id": "b73a219f-124b-4b10-9c88-123456789abc",
    "weekDay": 2,
    "messageType": "TEXT",
    "textContent": "Good morning! Happy Tuesday! 🚀",
    "isActive": true
  }
  ```
* **Response `400 Bad Request`:**
  ```json
  {
    "error": "Message content cannot be empty"
  }
  ```

---

#### `DELETE /api/messages/{id}`
Deletes a message by its UUID.
* **Path Parameter:** `id` (`string` / UUID).
* **Response `204 No Content`:** Empty body.

---

### 4.2 Trigger & Broadcast Operations

#### `POST /api/messages/trigger`
Immediately executes the daily scheduler trigger routine for the **current day of the week** according to the `America/Sao_Paulo` timezone. If there is an active message (`isActive: true`) for today's weekday, it is sent to the configured WhatsApp group via Evolution API.
* **Request Body:** None.
* **Response `200 OK`:** Empty body.

---

#### `POST /api/messages/trigger/manual`
Allows triggering a custom immediate broadcast or triggering a specific weekday's message on demand.
* **Request Body Options (`ManualTriggerRequest`):**
  * *Option A (Send custom text):*
    ```json
    {
      "message": "Immediate announcement: Server maintenance in 10 minutes!",
      "targetGroupId": null
    }
    ```
  * *Option B (Send existing weekday message on demand):*
    ```json
    {
      "weekDay": 5,
      "message": null,
      "targetGroupId": null
    }
    ```
  * *Note:* If `targetGroupId` (or `number`) is `null` or empty, it defaults to the `target-group-jid` configured in settings.
* **Response `200 OK`:** Empty body.
* **Response `400 Bad Request`:** Returned if both `message` and `weekDay` lookup fail or content is blank.
  ```json
  {
    "error": "Message content cannot be empty"
  }
  ```

---

#### `POST /api/messages/send` *(or `/api/messages/send-direct`)*
Sends a direct text message immediately to a specific WhatsApp phone number or individual chat without modifying schedules or groups.
* **Request Body (`DirectMessageRequest`):**
  ```json
  {
    "number": "5511999999999",
    "message": "Hello! This is a one-off direct message to your number."
  }
  ```
  *(Note: `number` can be a plain digits string like `"5511999999999"`, or a full JID like `"5511999999999@s.whatsapp.net"` or `"120363043837472938@g.us"`)*
* **Response `200 OK`:** Empty body.
* **Response `400 Bad Request`:**
  ```json
  {
    "error": "Recipient number cannot be empty"
  }
  ```

---

### 4.3 Settings & Schedule Configuration

#### `GET /api/messages/settings`
Retrieves application settings and the current scheduler cron expression.
* **Response `200 OK`:**
  ```json
  {
    "targetGroupJid": "120363043837472938@g.us",
    "evolutionApiUrl": "http://evolution-api:8080",
    "evolutionInstanceName": "my_whatsapp_instance",
    "cronTime": "0 0 5 * * *"
  }
  ```

---

#### `POST /api/messages/settings/cron`
Updates the daily schedule using a raw 6-field Spring cron expression. The timezone applied is always `America/Sao_Paulo`.
* **Request Body (`CronRequest`):**
  ```json
  {
    "cronTime": "0 30 7 * * *"
  }
  ```
  *(Format: `second minute hour day-of-month month day-of-week`)*
* **Response `200 OK`:** Empty body.
* **Response `400 Bad Request`:**
  ```json
  {
    "error": "Invalid cron expression: 0 60 25 * * *. Detail: ..."
  }
  ```

---

#### `POST /api/messages/settings/time`
**Recommended for Frontend UI:** Updates the daily schedule using a simple `HH:mm` (24-hour) string. The backend automatically converts this to `0 {mm} {HH} * * *` in `America/Sao_Paulo` timezone.
* **Request Body (`TimeRequest`):**
  ```json
  {
    "time": "08:30"
  }
  ```
* **Response `200 OK`:** Empty body.
* **Response `400 Bad Request`:**
  ```json
  {
    "error": "Invalid time format: 8:3. Must be HH:mm"
  }
  ```

---

### 4.4 Groups & Chat Targets Management

#### `GET /api/messages/groups` *(or `/api/groups`)*
Retrieves all configured WhatsApp group and chat targets.
* **Response `200 OK`:**
  ```json
  [
    {
      "id": "a1b2c3d4-e5f6-7a8b-9c0d-1234567890ab",
      "name": "Engineering Team",
      "groupJid": "120363043837472938@g.us",
      "isSelected": true
    },
    {
      "id": "f9e8d7c6-b5a4-3c2d-1e0f-9876543210ab",
      "name": "Management Chat",
      "groupJid": "5511999999999@s.whatsapp.net",
      "isSelected": false
    }
  ]
  ```

---

#### `POST /api/messages/groups` *(or `/api/groups`)*
Creates or updates a group/chat target.
* **Request Body (`GroupDto`):**
  ```json
  {
    "id": null,
    "name": "Engineering Team",
    "groupJid": "120363043837472938@g.us",
    "isSelected": true
  }
  ```
  *(Note: `chatId`, `jid`, or `targetGroupId` can also be passed as aliases for `groupJid`)*
* **Response `200 OK`:** Returns the saved `GroupDto`.

---

#### `POST /api/messages/groups/{id}/select`
Selects/activates a specific group target to receive scheduled or manual broadcasts.
* **Query Parameters:** `?exclusive=true` (default: `true`, which deselects all other groups and sets this group as the sole target; `false` allows selecting multiple concurrent groups).
* **Response `200 OK`:** Returns the updated `GroupDto`.

---

#### `POST /api/messages/settings/target-group`
Updates the primary default `targetGroupJid` directly in settings.
* **Request Body (`TargetGroupRequest`):**
  ```json
  {
    "targetGroupJid": "120363043837472938@g.us"
  }
  ```
* **Response `200 OK`:** Empty body.

---

### 4.5 System & Health

#### `GET /api/messages/health`
Checks application health and verifies if the PostgreSQL database connection is active.
* **Response `200 OK`:**
  ```json
  {
    "status": "UP",
    "database": "CONNECTED"
  }
  ```
* **Response `503 Service Unavailable`:**
  ```json
  {
    "status": "DOWN",
    "database": "DISCONNECTED",
    "error": "Connection refused"
  }
  ```

---

## 5. Ready-to-Use Axios Client (`apiClient.ts`)

Copy and drop this utility into your frontend project to interact with the API safely:

```typescript
import axios, { AxiosInstance, AxiosError } from 'axios';
import type {
  MessageDto,
  GroupDto,
  AppSettingsInfo,
  CronRequest,
  TimeRequest,
  TargetGroupRequest,
  DirectMessageRequest,
  ManualTriggerRequest,
  HealthResponse,
  ApiErrorResponse
} from './types/api'; // adjust import path

// Set base URL from env or fallback to your production/dev domain
const BASE_URL = import.meta.env.VITE_API_URL || 'https://daily.ximarelli.dev/api/messages';

const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // Enables cookies/credentials across CORS
  headers: {
    'Content-Type': 'application/json',
  },
});

export const WhatsAppSchedulerApi = {
  // --- Messages ---
  getAllMessages: async (): Promise<MessageDto[]> => {
    const { data } = await api.get<MessageDto[]>('');
    return data;
  },

  getMessageByDay: async (weekDay: number): Promise<MessageDto | null> => {
    try {
      const { data } = await api.get<MessageDto>(`/${weekDay}`);
      return data;
    } catch (error) {
      if ((error as AxiosError).response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  saveMessage: async (message: MessageDto): Promise<MessageDto> => {
    const { data } = await api.post<MessageDto>('', message);
    return data;
  },

  deleteMessage: async (id: string): Promise<void> => {
    await api.delete(`/${id}`);
  },

  // --- Triggers ---
  triggerDailyNow: async (): Promise<void> => {
    await api.post('/trigger');
  },

  triggerManual: async (request: ManualTriggerRequest): Promise<void> => {
    await api.post('/trigger/manual', request);
  },

  sendDirectMessage: async (request: DirectMessageRequest): Promise<void> => {
    await api.post('/send', request);
  },

  // --- Settings & Scheduling ---
  getSettings: async (): Promise<AppSettingsInfo> => {
    const { data } = await api.get<AppSettingsInfo>('/settings');
    return data;
  },

  updateCronTime: async (cronTime: string): Promise<void> => {
    await api.post('/settings/cron', { cronTime } as CronRequest);
  },

  updateScheduleTime: async (time: string): Promise<void> => {
    await api.post('/settings/time', { time } as TimeRequest);
  },

  updateTargetGroupJid: async (targetGroupJid: string): Promise<void> => {
    await api.post('/settings/target-group', { targetGroupJid } as TargetGroupRequest);
  },

  // --- Groups & Chat Targets ---
  getAllGroups: async (): Promise<GroupDto[]> => {
    const { data } = await api.get<GroupDto[]>('/groups');
    return data;
  },

  saveGroup: async (group: GroupDto): Promise<GroupDto> => {
    const { data } = await api.post<GroupDto>('/groups', group);
    return data;
  },

  deleteGroup: async (id: string): Promise<void> => {
    await api.delete(`/groups/${id}`);
  },

  selectGroup: async (id: string, exclusive = true): Promise<GroupDto> => {
    const { data } = await api.post<GroupDto>(`/groups/${id}/select?exclusive=${exclusive}`);
    return data;
  },

  // --- Health Check ---
  getHealth: async (): Promise<HealthResponse> => {
    const { data } = await api.get<HealthResponse>('/health');
    return data;
  },
};
```
