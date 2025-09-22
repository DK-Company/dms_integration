package com.dkcompany.dmsintegration.enums;

// Enums for supported procedure types.
public enum ProcedureType {
    // This list contains all types present on Told's website.
    A3("A3"), H1("H1"), G4("G4"), CC507C("CC507C"), B1("B1"), B2("B2"),
    B3("B3"), B4("B4"), C1("C1"), C2("C2"), C2EIDR("C2EIDR"), G2("G2"),
    G3("G3"), G4G3("G4G3"), G5("G5"), H2("H2"), H3("H3"), H4("H4"),
    H5("H5"), H6("H6"), L1("L1"), L2("L2"), L2PN("L2PN"), L2EIDR("L2EIDR"),
    LE007("LE007"), LE015("LE015"), LE026("LE026"), LE034("LE034"), CC547C("CC547C"),
    MSE("MSE"), MSI("MSI"), A1("A1"), A2("A2"), H7("H7"), CC590C("CC590C");

    public final String value;

    private ProcedureType(String value) {
        this.value = value;
    }

    // Get procedure type even of the Case is wrong
    public static ProcedureType findByValue(String value) {
        for (ProcedureType p : values()) {
            if (p.value.equalsIgnoreCase(value)) {
                return p; // Return the correct procedure type
            }
        }
        // Return null if the procedure type is not valid
        return null;
    }
}
