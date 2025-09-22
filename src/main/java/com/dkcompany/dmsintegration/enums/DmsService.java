//DKC/001/220925/TOP Added/Corrected enums

package com.dkcompany.dmsintegration.enums;

// Valid DMS Services
public enum DmsService {
    Shared("DMS.Shared"),
    Export("DMS.Export"),
    Import("DMS.Import"),
    Export2("DMS.Export2"),
    Import2("DMS.Import2");

    public final String value;

    private DmsService(String value) {
        this.value = value;
    }

    // Check DMS Service and return the short-service name
    public static DmsService findByValue(String value) {
        for (DmsService d : values()) {
            if (d.value.equalsIgnoreCase(value)) {
                return d;
            }
        }
        // Return null for invalid services
        return null;
    }
}
