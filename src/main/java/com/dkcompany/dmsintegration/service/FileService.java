package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.Document;
import com.dkcompany.dmsintegration.util.*;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import dk.toldst.eutk.as4client.utilities.Tools;
import org.javatuples.Pair;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;

@Configuration
@EnableScheduling
public class FileService {
    private final List<Directory> directories;
    private final As4DkcClient as4DkcClient;
    private final Logger logger = LoggerFactory.getLogger(FileService.class);

    public FileService(
            As4DkcClient as4DkcClient,
            @Value("${directoryPaths:null}") String directoryPaths
    ) {
        this.as4DkcClient = as4DkcClient;
        this.directories = new ArrayList<>();
        if (directoryPaths.equals("null")) {
            String path2 = "C:\\Files\\directory2";
            String path3 = "C:\\Files\\directory3";

            if (Files.exists(Paths.get(path2))) {
                directories.add(new Directory(path2));
            } else {
                logger.error(path2 + " could not be added because it was not a directory.");
            }

            if (Files.exists(Paths.get(path3))) {
                directories.add(new Directory(path3));
            } else {
                logger.error(path3 + " could not be added because it was not a directory.");
            }
        } else {
            List<String> paths = Arrays
                    .stream(directoryPaths.split(";"))
                    .filter(p -> {
                        if (Files.exists(Paths.get(p))) {
                            return true;
                        } else {
                            logger.error(p + " could not be added because it was not a directory.");
                            return false;
                        }
                    })
                    .toList();
            paths.forEach(path -> {
                directories.add(new Directory(path));
                logger.info("Added " + path + " to directories.");
            });
        }

        this.directories.forEach(d -> {
            String certificatePrefix = d.getCertificatePrefix();
            this.as4DkcClient.addCertificate(certificatePrefix);
            logger.info("Added certificate " + certificatePrefix + " to " + d.getBaseDirectory());
        });
    }

    @Scheduled(fixedDelay = 10000)
    public void submitDeclarations() {
        List<Pair<Directory, List<Document>>> directoryPairs = getDocumentsForSubmission();
        submitDirectories(directoryPairs);
    }

    private void submitDirectories(List<Pair<Directory, List<Document>>> directoryPairs) {
        directoryPairs.forEach(pair -> {
            Directory directory = pair.getValue0();
            List<Document> documents = pair.getValue1();
            String certificatePrefix = directory.getCertificatePrefix();

            if (documents == null || documents.isEmpty()) {
                logger.info("No new files in for " + directory.getBaseDirectory() + ".");
                return;
            }

            submitDocuments(documents, certificatePrefix, directory);
        });
    }

    private void submitDocuments(List<Document> documents, String certificatePrefix, Directory directory) {
        documents.forEach(document -> {
            File file = document.file();
            ProcedureType procedureType = document.procedureType();
            DmsService dmsService = document.dmsService();

            logger.info("Uploading file: " + file.getAbsolutePath());

            try {
                As4ClientResponseDto response = as4DkcClient.submitDeclaration(
                        file.getAbsolutePath(),
                        procedureType,
                        dmsService,
                        certificatePrefix
                );

                logger.info("Upload response: " + response.getFirstAttachment());

                var attachment = response.getFirstAttachment();
                String responseStatus = Tools.getStatus(attachment).getCode();
                if (responseStatus.equals("OK")) {
                    directory.moveToSuccess(file);
                } else {
                    directory.moveToError(file, attachment);
                }
            } catch (AS4Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<Pair<Directory, List<Document>>> getDocumentsForSubmission() {
        return directories
                .stream()
                .map(directory -> new Pair<>(directory, directory.listFiles()))
                .toList();
    }
}
