package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.util.As4DkcClient;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

//@Service
@Configuration
@EnableScheduling
public class FileService {
    private final List<Directory> directories;
    private final As4DkcClient as4DkcClient;

    public FileService(
            As4DkcClient as4DkcClient,
            @Value("${directoryPaths:null}") String directoryPaths) {
        this.as4DkcClient = as4DkcClient;
        directories = new ArrayList<>();
        if (directoryPaths.equals("null")) {
            System.out.println("directoryPaths cmd argument was null.");
            directories.add(new Directory("C:\\Files\\directory3"));
        } else {
            List<String> paths = Arrays.stream(directoryPaths.split(";")).toList();
            paths.forEach(path -> {
                directories.add(new Directory(path));
            });
        }
    }

//    @Scheduled(fixedDelay = 10000)
    private void moveFiles() {
        directories.forEach(Directory::moveFiles);
    }

    @Scheduled(fixedDelay = 20000)
    public void submitDeclarations() {
        List<Document> documents = getDocumentsForSubmission();

        if (documents == null) {
            System.out.println("No new files in temp directories.");
            return;
        }

        documents.forEach(document -> {
            File file = document.getFile();
            try {
                As4ClientResponseDto response = as4DkcClient.submitDeclaration(
                        file.getAbsolutePath(),
                        document.getProcedureType(),
                        document.getDmsService());
                System.out.println("Upload response: " + response.getFirstAttachment());
                file.delete();
            } catch (AS4Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<Document> getDocumentsForSubmission() {
        return directories.stream()
                .map(Directory::listTempFiles)
                .flatMap(Collection::stream)
                .toList();
    }
}
