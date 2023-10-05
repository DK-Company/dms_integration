package com.dkcompany.dmsintegration.enums;

public enum ProcedureType {
    B1("B1");

    public final String value;

    private ProcedureType(String value) {
        this.value = value;
    }

    public static ProcedureType findByValue(String value) {
        for (ProcedureType p : values()) {
            if (p.value.equalsIgnoreCase(value)) {
                return p;
            }
        }

        return null;
    }
}
