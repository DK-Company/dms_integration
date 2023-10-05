package com.dkcompany.dmsintegration.enums;

public enum DmsService {
    Export2("DMS.Export2"),
    Import2("DMS.Import2");

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
