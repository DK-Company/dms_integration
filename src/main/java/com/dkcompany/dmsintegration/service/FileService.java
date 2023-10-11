package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.Document;
import com.dkcompany.dmsintegration.util.*;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import dk.toldst.eutk.as4client.utilities.Tools;
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

        addDirectories(directoryPaths);
    }

    private void addDirectories(String directoryPaths) {
        if (directoryPaths.equals("null")) {
            directories.add(new Directory("C:\\Files\\directory2"));
            directories.add(new Directory("C:\\Files\\directory3"));
        } else {
            List<String> paths = Arrays
                    .stream(directoryPaths.split(";"))
                    .filter(p -> Files.exists(Paths.get(p)))
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
        directories.forEach(directory -> {
            List<Document> documents = directory.listFiles();

            if (documents == null || documents.isEmpty()) {
                logger.info("No new files in for " + directory.getBaseDirectory() + ".");
                return;
            }

            submitDocumentsForDirectory(directory, documents);
        });
    }

    private void submitDocumentsForDirectory(
            Directory directory,
            List<Document> documents
    ) {
        String certificatePrefix = directory.getCertificatePrefix();

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
}
