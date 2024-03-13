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
            @Value("${directoryPaths:null}") String directoryPaths,
            NotificationService notificationService) {
        this.as4DkcClient = as4DkcClient;
        this.notificationService = notificationService;
        this.directories = new ArrayList<>();

        addDirectories(directoryPaths);
    }

    private void addDirectories(String directoryPaths) {
        if (directoryPaths.equals("null")) {
        String rootPackageName = YourApplication.class.getPackageName(); // get the package name of the project
        String rootPackagePath = rootPackageName.replace(".", "/"); // change to directory format
        String basePath = Paths.get(".").toAbsolutePath().normalize().toString(); // get the absolute path
        System.out.println(basePath + "/" + rootPackagePath);
        directories.add(new Directory(basePath + "/" + rootPackagePath)); // send absolute path in constructor to set inner basePath
            //directories.add(new Directory("C:\\Files\\directory3"));
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