package com.birmanBank.BirmanBankBackend.services;

import com.birmanBank.BirmanBankBackend.models.InboxMessage;
import com.birmanBank.BirmanBankBackend.repositories.InboxMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageService {

    private final InboxMessageRepository inboxMessageRepository;

    public MessageService(InboxMessageRepository inboxMessageRepository) {
        this.inboxMessageRepository = inboxMessageRepository;
    }

    public void sendMessage(String recipientId, String subject, String body) {
        InboxMessage message = InboxMessage.builder()
                .recipientId(recipientId)
                .senderId(null) // System message
                .subject(subject)
                .body(body)
                .timestamp(LocalDateTime.now())
                .build();
        inboxMessageRepository.save(message);
    }
}