package com.birmanBank.BirmanBankBackend.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "inbox_messages")
public class InboxMessage {
    @Id
    private String messageId; // Unique identifier for the message
    private String recipientId; // User ID of the recipient (admin)
    private String senderId; // User ID of the sender (null for system messages)
    private String subject; // Subject of the message
    private String body; // Body of the message
    private LocalDateTime timestamp; // Timestamp of when the message was created
    private String status; // Status of the message (e.g., ACCEPTED, DENIED)
    private LocalDateTime updatedAt; // Timestamp of the last update
    
    private String targetClientId; // ID of the client to be activated
}