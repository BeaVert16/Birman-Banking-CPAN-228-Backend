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
    private String messageId;
    private String recipientId;
    private String senderId;
    private String subject;
    private String body;
    private LocalDateTime timestamp;
    private String status;
    private LocalDateTime updatedAt; 
    
    private String targetClientId; 
}