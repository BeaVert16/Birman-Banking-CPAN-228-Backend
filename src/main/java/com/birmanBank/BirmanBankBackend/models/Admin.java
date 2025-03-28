package com.birmanBank.BirmanBankBackend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.List;
import lombok.Data;

@Data
@Document(collection = "admins")
public class Admin {
    @Id
    private String id;
    @DBRef
    private Account account;
    private List<String> permissions;
}