package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.enums.DeclarationAction;
import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.Document;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

//DKC/001 Added support for notificationRequests

public class Directory {
    @Getter
    private final File baseDirectory;
    private final File outDirectory;
    @Getter
    private final File notificationRequestDirectory; //DKC/001
    private final File notificationRequestHandledDirectory; //DKC/001
    @Getter
    private final File inDirectory;
    private final File successDirectory;
    private final File errorDirectory;
    @Getter
    private final String certificatePrefix;
    @Getter
    private final Properties properties;

    public Directory(Properties _properties) {
        baseDirectory = new File(_properties.getProperty("directoryPath"));
        outDirectory = new File(baseDirectory, "out");
        notificationRequestDirectory = new File(baseDirectory, "notificationRequests"); //DKC/001
        notificationRequestHandledDirectory = new File(baseDirectory, "notificationRequests/handled"); //DKC/001
        inDirectory = new File(baseDirectory, "in");
        successDirectory = new File(baseDirectory, "success");
        errorDirectory = new File(baseDirectory, "error");
        properties = _properties;

        if (!baseDirectory.exists()) baseDirectory.mkdirs();
        if (!outDirectory.exists()) outDirectory.mkdir();
        if (!notificationRequestDirectory.exists()) notificationRequestDirectory.mkdir(); //DKC/001
        if (!notificationRequestHandledDirectory.exists()) notificationRequestHandledDirectory.mkdir(); //DKC/001
        if (!inDirectory.exists()) inDirectory.mkdir();
        if (!successDirectory.exists()) successDirectory.mkdir();
        if (!errorDirectory.exists()) errorDirectory.mkdir();

        this.certificatePrefix = properties.getProperty("certificatePrefix");
    }

    public void moveToSuccess(File file) {
        try {
            FileUtils.copyFileToDirectory(file, successDirectory);
            file.delete();
        } catch (IOException e) {
            System.out.printf("Error happened when trying to move file to Success folder. Inner exception: " + e.getMessage());
        }
    }

    //DKC/001/START
    public void moveToHandled(String fileName) {
        try {
            File file = new File(fileName);
            if(file.exists()) {
                File handledDirectory = new File( file.getParent()+"/handled");
                if (!handledDirectory.exists()) handledDirectory.mkdirs();
                //System.out.printf("\n moving filename : " + file.toString() + "\n");
                //System.out.printf("\n move to : " + handledDirectory.toString() + "\n");
                FileUtils.copyFileToDirectory(file, handledDirectory);
                file.delete();
            }
        } catch (IOException e) {
            System.out.printf("Error happened when trying to move file to the handled folder. Inner exception: " + e.getMessage());
        }

    }
    //DKC/001/STOP

    public void moveToError(File file, String errorMessage) {
        try {
            FileUtils.copyFileToDirectory(file, errorDirectory);

            // Create file that contains error message
            String fileNameWithoutExtension = FilenameUtils.removeExtension(file.getName());
            String errorFileName = fileNameWithoutExtension + ".error.xml";
            String errorFilePath = new File(errorDirectory, errorFileName).toString();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(errorFilePath, true))) {
                writer.write(errorMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            file.delete();
        } catch (IOException e) {
            System.out.printf("Error happened when trying to move file to Error folder. Inner exception: " + e.getMessage());
        }
    }

    public List<Document> listFiles() {
        File[] files = outDirectory.listFiles();

        if (files == null) {
            return null;
        }

        return Arrays.stream(files)
                .toList()
                .stream()
                .filter(f -> FilenameUtils.getExtension(f.toString()).equalsIgnoreCase("xml"))
                .filter(f -> getProcedureTypeFromFile(f) != null)
                .filter(f -> getDmsServiceFromFile(f) != null)
                .filter(f -> getDeclarationActionFromFile(f) != null)
                .map(f -> {
                    return new Document(
                            f,
                            getProcedureTypeFromFile(f),
                            getDmsServiceFromFile(f),
                            getDeclarationActionFromFile(f)
                    );
                })
                .toList();
    }

    // Get the procedure-type from the filename (First part of the filename ex. DMS.Import)
    private static ProcedureType getProcedureTypeFromFile(File file) {
        String fileName = file.getName();
        String prefix = fileName.split("_")[0];
        ProcedureType procedureType = ProcedureType.findByValue(prefix);
        try {
            if (procedureType == null) {
                throw new Exception("Invalid Procedure Type");
            }
            return procedureType;
        } catch (Exception e){
            System.out.printf("Directory creation failed with the error: " + e.getMessage());
            return null;
        }
    }

    private static DmsService getDmsServiceFromFile(File file) {
        String fileName = file.getName();
        String prefix = fileName.split("_")[1];
        DmsService service = DmsService.findByValue(prefix);
        try {
            if (service == null) {
                throw new Exception("Invalid Dms Service");
            }
            return service;
        } catch (Exception e){
            System.out.printf("Directory creation failed with the error: " + e.getMessage());
            return null;
        }
    }

    private static DeclarationAction getDeclarationActionFromFile(File file) {
        String fileName = file.getName();
        String prefix = fileName.split("_")[2];
        DeclarationAction action = DeclarationAction.findByValue(prefix);
        try {
            if (action == null) {
                throw new Exception("Invalid Declaration Action");
            }
            return action;
        } catch (Exception e){
            System.out.printf("Directory creation failed with the error: " + e.getMessage());
            return null;
        }
    }
}
