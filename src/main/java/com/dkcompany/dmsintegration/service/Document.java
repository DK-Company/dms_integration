package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import lombok.Getter;

import java.io.File;

public class Document {
    @Getter
    private final File file;
    @Getter
    private final ProcedureType procedureType;
    @Getter
    private final DmsService dmsService;

    public Document(File file, ProcedureType procedureType, DmsService dmsService) {
        this.file = file;
        this.procedureType = procedureType;
        this.dmsService = dmsService;
    }
}
