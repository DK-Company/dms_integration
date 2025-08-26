package com.dkcompany.dmsintegration.as4client;

import java.io.Serializable;

public class As4UserInformation implements Serializable {
    private static final long serialVersionUID = 1;
    private final As4UserInformationType type;

    private String cvr = null;
    private String id;

    public As4UserInformation(final As4UserInformationType type, final String id, final String cvrOptional) {
        this.type = type;
        this.id = id;
        if (cvrOptional != null) {
            cvr = cvrOptional;
        }
    }

    public As4UserInformationType getType() {
        return type;
    }

    public String getCvr() {
        return cvr;
    }

    public void setCvr(String cvr) {
        this.cvr = cvr;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}