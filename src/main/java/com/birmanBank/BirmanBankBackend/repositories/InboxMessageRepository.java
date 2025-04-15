package com.birmanBank.BirmanBankBackend.repositories;

import com.birmanBank.BirmanBankBackend.models.InboxMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InboxMessageRepository extends MongoRepository<InboxMessage, String> {
    List<InboxMessage> findByRecipientId(String recipientId); // find messages by recipient
}