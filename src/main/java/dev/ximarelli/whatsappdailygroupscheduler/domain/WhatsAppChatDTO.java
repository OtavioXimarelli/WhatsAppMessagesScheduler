package dev.ximarelli.whatsappdailygroupscheduler.domain;

/**
 * Represents a chat or contact fetched from the WhatsApp instance via Evolution API.
 * Used by the frontend ChatPicker component for easy group/contact selection.
 */
public record WhatsAppChatDTO(
        String jid,              // WhatsApp JID: "120363...@g.us" (group) or "5511...@s.whatsapp.net" (contact)
        String name,             // Display name: group subject or contact pushName
        String type,             // "GROUP" or "CONTACT"
        String profilePicUrl,    // Profile picture URL (nullable)
        Integer participantsCount, // Number of participants (groups only, nullable)
        Boolean isFavorite       // Whether this chat is starred as favorite (merged from local GroupEntity)
) {
}
