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

public class Directory {
    @Getter
    private final File baseDirectory;
    private final File outDirectory;
    private final File successDirectory;
    private final File errorDirectory;
    @Getter
    private final String certificatePrefix;

    public Directory(String baseDirectoryString) {
        baseDirectory = new File(baseDirectoryString);
        outDirectory = new File(baseDirectory, "out");
        successDirectory = new File(baseDirectory, "success");
        errorDirectory = new File(baseDirectory, "error");

        if (!baseDirectory.exists()) baseDirectory.mkdirs();
        if (!outDirectory.exists()) outDirectory.mkdir();
        if (!successDirectory.exists()) successDirectory.mkdir();
        if (!errorDirectory.exists()) errorDirectory.mkdir();

        this.certificatePrefix = getCertificatePrefixFromConfigFile(baseDirectory);
    }

    /**
     * Expects a file called certificate.config in the root of the
     * base directory. Reads the first line of the file and returns
     * the value.
     * @param baseDirectory file object of the base directory
     * @return value of the certificate prefix
     */
    private String getCertificatePrefixFromConfigFile(File baseDirectory) {
        var file = new File(baseDirectory, "certificate.config");
        try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();

            if (firstLine == null) {
                throw new RuntimeException("Could not set certificate prefix for: " + baseDirectory);
            }

            return firstLine;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
