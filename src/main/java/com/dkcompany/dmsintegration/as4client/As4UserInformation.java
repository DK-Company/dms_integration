package com.dkcompany.dmsintegration.as4client;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class As4UserInformation implements Serializable {
    private final As4UserInformationType type;

    private String cvr = null;
    private final String id;

    public As4UserInformation(final As4UserInformationType type, final String id, final String cvrOptional) {
        this.type = type;
        this.id = id;
        if (cvrOptional != null) {
            cvr = cvrOptional;
        }
    }

}