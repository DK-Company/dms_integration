package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.Application;
import com.dkcompany.dmsintegration.enums.DeclarationAction;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

@Configuration
@EnableScheduling
public class FileService {
    private final List<Directory> directories;
    private final As4DkcClient as4DkcClient;
    private final NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public FileService(
            As4DkcClient as4DkcClient,
            @Value("${configPath:null}") String configPath,
            NotificationService notificationService) {
        this.as4DkcClient = as4DkcClient;
        this.notificationService = notificationService;
        this.directories = new ArrayList<>();

        addDirectories(configPath);
    }

    private void addDirectories(String configPath) {
        List<Path> configFiles = getConfigFiles(configPath);
        configFiles.forEach(c -> {
            Properties properties = loadProperties(c.toString());
            directories.add(new Directory(properties));
            logger.info("Added " + properties.getProperty("alias") + "to directories");
        });


        this.directories.forEach(d -> {
            String certificatePrefix = d.getCertificatePrefix();
            this.as4DkcClient.addCertificate(d.getProperties());
            logger.info("Added certificate " + certificatePrefix + " to " + d.getBaseDirectory());
        });
    }

    public static List<Path> getConfigFiles(String directoryPath) {
        List<Path> configFiles = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath), "*.{config}")) {
            for (Path path : directoryStream) {
                configFiles.add(path);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return configFiles;
    }

    public static Properties loadProperties(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            properties.load(fileInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

    @Scheduled(fixedDelay = 10000)
    public void submitDeclarations() {
        directories.forEach(directory -> {
            List<Document> documents = directory.listFiles();

            if (documents == null || documents.isEmpty()) {
                return;
            }

            submitDocumentsForDirectory(directory, documents);
        });
    }

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void retrieveNotifications() {
        // First step: push notification requests concurrently for each certificate
        var futures = directories.stream()
                .map(d -> CompletableFuture.supplyAsync(() -> {
                    return notificationService.pushNotificationRequests(d);
                }))
                .toList();
        futures.forEach(CompletableFuture::join);

        // Second step: pull notifications concurrently for each certificate
        var completableFutures = directories.stream()
                .map(d -> CompletableFuture.supplyAsync(() -> {
                    return notificationService.pullNotifications(d);
                }))
                .toList();

        // Third step: save notifications to files in directories
        completableFutures.parallelStream()
                .map(CompletableFuture::join)
                .forEach(notificationService::saveNotifications);
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
            DeclarationAction declarationAction = document.declarationAction();

            logger.info("Uploading file: " + file.getAbsolutePath());

            try {
                As4ClientResponseDto response = as4DkcClient.submitDeclaration(
                        file.getAbsolutePath(),
                        procedureType,
                        dmsService,
                        declarationAction,
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