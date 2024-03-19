package com.dkcompany.dmsintegration.enums;

public enum DmsService {
    Export2("DMS.Export"),
    Import2("DMS.Import");

    public final String value;

    private DmsService(String value) {
        this.value = value;
    }

    public static DmsService findByValue(String value) {
        for (DmsService d : values()) {
            if (d.value.equalsIgnoreCase(value)) {
                return d;
            }
        }

        return null;
    }
}
