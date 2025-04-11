package com.birmanBank.BirmanBankBackend.models;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private String address;
    private String city;
    private String postalCode;
    private String additionalInfo;
    private String province;
    private String country;
}