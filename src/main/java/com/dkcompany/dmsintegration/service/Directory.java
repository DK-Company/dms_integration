package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.util.DmsService;
import com.dkcompany.dmsintegration.util.ProcedureType;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Directory {
    private final File outDirectory;
    @Getter
    private final File tempDirectory;

    public Directory(String baseDirectory) {
        outDirectory = new File(baseDirectory, "out");
        tempDirectory = new File(baseDirectory, "temp");

        assert(outDirectory.isDirectory());
        assert(tempDirectory.isDirectory());
    }

    public void moveFiles() {
        List<File> files = listFiles();
        copyFiles(files);
    }

    private List<File> listFiles() {
        File[] files = outDirectory.listFiles();

        if (files == null) {
            return null;
        }

        return Arrays.stream(files)
                .toList()
                .stream()
                .filter(f -> FilenameUtils.getExtension(f.toString()).equalsIgnoreCase("xml"))
                .toList();
    }

    public List<Document> listTempFiles() {
        File[] files = tempDirectory.listFiles();

        if (files == null) {
            return  null;
        }

        return Arrays.stream(files)
                .map(file -> new Document(file, ProcedureType.B1, DmsService.Export2))
                .toList();
    }

    private void copyFiles(List<File> files) {
        if (files == null) {
            return;
        }

        files.forEach(file -> {
            try {
                FileUtils.copyFileToDirectory(file, tempDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
