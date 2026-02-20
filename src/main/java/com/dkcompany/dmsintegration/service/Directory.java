package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.enums.DeclarationAction;
import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.Document;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/*

    Directory-handler. Each CRV has a directory-structure and a config file.
    The configuration for each Directory is stored in the Directory-list, and
    is used for handeling the directories.

    A directory is set up for eacn config file one for each CRV we handle.

 */

//DKC/001 Added support for notificationRequests
//DKC/002/090226/TOP Added support for batches
//                   Changed to public functions getProcedureTypeFromFile+getDmsServiceFromFile

public class Directory {
    @Getter
    private final File baseDirectory;
    private final File outDirectory;
    @Getter
    private final File notificationRequestDirectory; //DKC/001
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
        File notificationRequestHandledDirectory = new File(baseDirectory, "notificationRequests/handled");//DKC/001
        inDirectory = new File(baseDirectory, "in");
        successDirectory = new File(baseDirectory, "success");
        errorDirectory = new File(baseDirectory, "error");
        properties = _properties;

        try {
            Files.createDirectories(baseDirectory.toPath());
            Files.createDirectories(outDirectory.toPath());
            Files.createDirectories(notificationRequestDirectory.toPath());          // DKC/001
            Files.createDirectories(notificationRequestHandledDirectory.toPath());   // DKC/001
            Files.createDirectories(inDirectory.toPath());
            Files.createDirectories(successDirectory.toPath());
            Files.createDirectories(errorDirectory.toPath());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to initialize directory structure under: "
                            + baseDirectory.getAbsolutePath(),  e
            );
        }

        this.certificatePrefix = properties.getProperty("certificatePrefix");
    }

    // Move a file/folder to the success folder
    public void moveToSuccess(File source) {
        try {
            // Ensure success directory exists
            if (!successDirectory.exists()) {
                if (!successDirectory.mkdirs()) {
                    throw new IOException("Failed to create success directory: "
                            + successDirectory.getAbsolutePath());
                }
            }

            Path sourcePath = source.toPath();
            Path targetPath = successDirectory.toPath().resolve(source.getName());

            Files.move(
                    sourcePath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {
            System.out.printf(
                    "Error when trying to move to Success folder. Inner exception: %s%n",
                    e.getMessage()
            );
        }
    }

    //DKC/001/START
    // Move file to handled folder
    public void moveToHandled(String fileName) {
        try {
            File file = new File(fileName);
            if(file.exists()) {
                File handledDirectory = new File( file.getParent()+"/handled");
                if (!handledDirectory.exists()) Files.createDirectories(handledDirectory.toPath());
                FileUtils.copyFileToDirectory(file, handledDirectory);
                Files.delete(file.toPath());
            }
        } catch (IOException e) {
            System.out.printf("Error happened when trying to move file to the handled folder. Inner exception: " + e.getMessage());
        }

    }
    //DKC/001/STOP

    // Move file to error folder
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
            Files.delete(file.toPath());
        } catch (IOException e) {
            System.out.printf("Error happened when trying to move file to Error folder. Inner exception: " + e.getMessage());
        }
    }

    // Get a list of valid declaration files in the out-folder
    public List<Document> listDeclarationFiles() {
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
                .map(f -> new Document(
                        f,
                        getProcedureTypeFromFile(f),
                        getDmsServiceFromFile(f),
                        getDeclarationActionFromFile(f)
                ))
                .toList();
    }

    //DKC/002/START

    // Find batch folders in the "out" folder, that are ready to be handled.
    public List<File> listBatches() {
        // Get content of the outfolder
        File[] files = outDirectory.listFiles();
        if (files == null) {
            return null;
        }
        // Return a list of batchfolders, that are "ready" to be handled
        return Arrays.stream(files)
                .toList()
                .stream()
                .filter(File::isDirectory)
                .filter(f -> new File(f, "ready.txt").exists())    // Must have a ready.txt file
                .filter(f -> !new File(f, "handled.txt").exists()) // Must NOT have a handled.txt
                .filter(f -> !new File(f, "error.txt").exists())   // Must NOT have a error.txt
                .filter(f -> !new File(f, "error.xml").exists())   // Must not have a error.xml
                .toList();
    }

    // Get a list of xml files in the batch folder
    public List<Document> listBatchFiles(File batchFolder) {
        File[] files = batchFolder.listFiles();

        if (files == null) {
            return null;
        }
        // Return all declaration files wher the filename that are correct formated.
        return Arrays.stream(files)
                .toList()
                .stream()
                .filter(File::isFile)
                .filter(f -> FilenameUtils.getExtension(f.toString()).equalsIgnoreCase("xml"))
                .filter(f -> getProcedureTypeFromFile(f) != null)
                .filter(f -> getDmsServiceFromFile(f) != null)
                .filter(f -> getDeclarationActionFromFile(f) != null)
                .map(f -> new Document(
                        f,
                        getProcedureTypeFromFile(f),
                        getDmsServiceFromFile(f),
                        getDeclarationActionFromFile(f)
                ))
                .toList();
    }


    //DKC/002/STOP

    // Get the procedure-type from the filename (First part of the filename ex. DMS.Import)
    public static ProcedureType getProcedureTypeFromFile(File file) {
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

    public static DmsService getDmsServiceFromFile(File file) {
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
