package com.dkcompany.dmsintegration.record;

import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import java.io.File;

public record Document(
        File file,
        ProcedureType procedureType,
        DmsService dmsService
) { }