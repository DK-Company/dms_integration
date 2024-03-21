package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.Document;
import com.google.common.io.Files;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class Directory {
    @Getter
    private final File baseDirectory;
    private final File outDirectory;
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
        inDirectory = new File(baseDirectory, "in");
        successDirectory = new File(baseDirectory, "success");
        errorDirectory = new File(baseDirectory, "error");
        properties = _properties;

        if (!baseDirectory.exists()) baseDirectory.mkdirs();
        if (!outDirectory.exists()) outDirectory.mkdir();
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
            throw new RuntimeException(e);
        }
    }

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
            throw new RuntimeException(e);
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
                .map(f -> {
                    return new Document(
                            f,
                            getProcedureTypeFromFile(f),
                            getDmsServiceFromFile(f)
                    );
                })
                .toList();
    }

    private static ProcedureType getProcedureTypeFromFile(File file) {
        String fileName = file.getName();
        String prefix = fileName.split("_")[0];
        return ProcedureType.findByValue(prefix);
    }

    private static DmsService getDmsServiceFromFile(File file) {
        String fileName = file.getName();
        String prefix = fileName.split("_")[1];
        return DmsService.findByValue(prefix);
    }
}
