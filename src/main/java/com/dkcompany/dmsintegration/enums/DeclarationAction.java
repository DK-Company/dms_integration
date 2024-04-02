package com.dkcompany.dmsintegration.enums;

public enum DeclarationAction {
    // This list contains all types present on Told's website.
    Submit("Submit"), Amend("Amend"), Invalidate("Invalidate"),
    AmmendGoodspresented("Ammend.Goodspresented");



    public final String value;

    private DeclarationAction(String value) {
        this.value = value;
    }

    public static DeclarationAction findByValue(String value) {
        for (DeclarationAction d : values()) {
            if (d.value.equalsIgnoreCase(value)) {
                return d;
            }
        }

        return null;
    }
}