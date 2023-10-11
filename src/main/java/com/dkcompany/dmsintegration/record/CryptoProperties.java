package com.dkcompany.dmsintegration.record;

public record CryptoProperties(
        String file,
        String password,
        String type,
        String alias,
        String privatePassword
) {}
