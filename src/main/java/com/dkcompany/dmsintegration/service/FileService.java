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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.slf4j.Logger;

@Configuration
@EnableScheduling
public class FileService {
    private final List<Directory> directories;
    private final As4DkcClient as4DkcClient;
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

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
                // logger.info("No new files for " + directory.getBaseDirectory() + ".");
                return;
            }

            submitDocumentsForDirectory(directory, documents);
        });
    }

    @Scheduled(fixedDelay = 300000)
    public void retrieveNotifications() {
        // TODO: use logic from NotificationService

        // First step: push notification requests for each certificate
        CompletableFuture<Void>[] futures = directories.stream()
                .map(d -> CompletableFuture.supplyAsync(() -> pushNotificationRequests(d)))
                .toArray(CompletableFuture[]::new);
        Arrays.stream(futures).forEach(CompletableFuture::join);

        // Second step: pull notifications for each certificate
        CompletableFuture<Pair<Directory, List<As4ClientResponseDto>>>[] completableFutures = directories.stream()
                .map(d -> CompletableFuture.supplyAsync(() -> pullNotifications(d)))
                .toArray(CompletableFuture[]::new);

        Arrays.stream(completableFutures)
                .parallel()
                .map(CompletableFuture::join)
                .forEach(this::saveNotification);
    }

    private Void pushNotificationRequests(Directory directory) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime then = now.minusMinutes(5);
        String certificatePrefix = directory.getCertificatePrefix();

        String nowFormatted = now.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("Pushing notification request for %s (%s).%n", directory.getBaseDirectory(), nowFormatted);

        try {
            as4DkcClient.pushNotificationRequest(now, then, certificatePrefix);
            return null;
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Pair<Directory, List<As4ClientResponseDto>> pullNotifications(Directory directory) {
        String nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("Pulling notifications for %s (%s).%n", directory.getBaseDirectory(), nowFormatted);

        List<As4ClientResponseDto> dtos = new ArrayList<>();

        try {
            while (true) {
                As4ClientResponseDto dto = as4DkcClient.pullNotifications(directory.getCertificatePrefix());
                dtos.add(dto);

                if (dto.getReftoOriginalID() == null) {
                    break;
                }
            }

            return new Pair<Directory, List<As4ClientResponseDto>>(
                    directory,
                    dtos
            );
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveNotification (Pair<Directory, List<As4ClientResponseDto>> tuple) {
        Directory directory = tuple.getValue0();
        StringBuilder notifications = getStringBuilder(tuple);

        System.out.println(notifications);

        Path fileLocationPath = Paths.get(directory.getInDirectory().getAbsolutePath(), "notifications.txt");
        String fileLocation = fileLocationPath.toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileLocation, true))) {
            writer.write(notifications.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static StringBuilder getStringBuilder(Pair<Directory, List<As4ClientResponseDto>> tuple) {
        List<As4ClientResponseDto> dtos = tuple.getValue1();

        StringBuilder notifications = new StringBuilder();

        dtos.forEach(dto -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String firstAttachment = dto.getFirstAttachment();
            if (firstAttachment == null) {
                firstAttachment = "Empty.";
            }

            notifications.append("[NOTIFICATION ");
            notifications.append(time);
            notifications.append(']');
            notifications.append('\n');
            notifications.append(firstAttachment);
            notifications.append('\n');
        });
        return notifications;
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