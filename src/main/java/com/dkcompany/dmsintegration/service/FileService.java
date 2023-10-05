package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.util.*;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import dk.toldst.eutk.as4client.utilities.Tools;
import org.javatuples.Pair;
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

@Configuration
@EnableScheduling
public class FileService {
    private final List<Directory> directories;
    private final As4DkcClient as4DkcClient;

    public FileService(
            As4DkcClient as4DkcClient,
            @Value("${directoryPaths:null}") String directoryPaths
    ) {
        this.as4DkcClient = as4DkcClient;
        directories = new ArrayList<>();
        if (directoryPaths.equals("null")) {
            String path = "C:\\Files\\directory3";
            boolean directoryExists = Files.exists(Paths.get(path));
            if (directoryExists) {
                directories.add(new Directory(path));
            } else {
                System.out.println(path + " could not be added because it was not a directory.");
            }
        } else {
            List<String> paths = Arrays
                    .stream(directoryPaths.split(";"))
                    .filter(p -> {
                        if (Files.exists(Paths.get(p))) {
                            return true;
                        } else {
                            System.out.println(p + " could not be added because it was not a directory.");
                            return false;
                        }
                    })
                    .toList();
            paths.forEach(path -> {
                System.out.println("Added " + path + " to directories.");
                directories.add(new Directory(path));
            });
            System.out.println("directories length: " + directories.size());

        }
    }

    @Scheduled(fixedDelay = 10000)
    public void submitDeclarations() {
        List<Pair<Directory, List<Document>>> directoryPairs = getDocumentsForSubmission();

        directoryPairs.forEach(pair -> {
            Directory directory = pair.getValue0();
            List<Document> documents = pair.getValue1();

            if (documents == null || documents.isEmpty()) {
                System.out.println("No new files in out directories.");
                return;
            }

            documents.forEach(document -> {
                File file = document.getFile();
                ProcedureType procedureType = document.getProcedureType();
                DmsService dmsService = document.getDmsService();

                System.out.println("Uploading file: " + file.getAbsolutePath());

                try {
                    As4ClientResponseDto response = as4DkcClient.submitDeclaration(
                            file.getAbsolutePath(),
                            procedureType,
                            dmsService);

                    System.out.println("Upload response: " + response.getFirstAttachment());

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
        });
    }

    private List<Pair<Directory, List<Document>>> getDocumentsForSubmission() {
        return directories
                .stream()
                .map(directory -> new Pair<>(directory, directory.listFiles()))
                .toList();
    }
}
