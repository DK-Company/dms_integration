package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.util.As4DkcClient;

import com.dkcompany.dmsintegration.as4client.As4ClientResponseDto;
import com.dkcompany.dmsintegration.as4client.AS4Exception;

import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
// Handles request-for and return of notifications
public class NotificationService {
    private final As4DkcClient as4DkcClient;

    public NotificationService(
            As4DkcClient as4DkcClient
    ) {
        this.as4DkcClient = as4DkcClient;
    }

    // Send request to the AS4 gateway
    public void sendRequest(
            String serviceEndpointTxt,   // ex "DMS.Export"
            String serviceTypeTxt,       // ex "Notification",
            Map<String, String> serviceAttributes, // Attributes to be passed to the service
            Properties properties,
            String messageId){
        try {
            // Push the request to the AS4 gateway
            as4DkcClient.pushRequest(serviceEndpointTxt,serviceTypeTxt,serviceAttributes,properties, messageId);
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }
    }


    // Request new notifications to notification-queue
    public void requestNotifications(Properties properties){
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        requestRecentNotifications(now, properties);
    }

    // Pull notifications from notification-queue
    public List<As4ClientResponseDto> pullNotifications(Properties properties) {
        List<As4ClientResponseDto> dtos = new ArrayList<>();

        // Pull notifications until the queue is empty
        while (true) {
            As4ClientResponseDto dto = as4DkcClient.pullNotifications(properties);
            dtos.add(dto);
            if (dto.getRefToOriginalID() == null) break;
        }
        // Return the notifications we got
        return dtos;
    }

    // Save notifications to notifications.txt log file
    public void saveNotifications(Pair<Directory, List<As4ClientResponseDto>> tuple) {
        Directory directory = tuple.getValue0();

        // Extract notification
        StringBuilder notifications = getStringBuilder(tuple);

        //System.out.println(notifications);

        Path fileLocationPath = Paths.get(directory.getInDirectory().getAbsolutePath(), "notifications.txt");
        String fileLocation = fileLocationPath.toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileLocation, true))) {
            writer.write(notifications.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void pushNotificationRequests(Directory directory) {
        requestNotifications(directory.getProperties());
    }

    public Pair<Directory, List<As4ClientResponseDto>> pullNotifications(Directory directory) {
        List<As4ClientResponseDto> dtos = pullNotifications(directory.getProperties());
        return new Pair<>(directory, dtos);
    }

    // Extract the notification from the CTO message
    private static StringBuilder getStringBuilder(Pair<Directory, List<As4ClientResponseDto>> tuple) {
        List<As4ClientResponseDto> dtoList = tuple.getValue1();
        StringBuilder notifications = new StringBuilder();

        // Loop dtoList-notifications, extract notofication-files and build notifications-text
        dtoList.forEach(dto -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String firstAttachment = dto.getFirstAttachment();
            if (firstAttachment == null) {firstAttachment = "Empty.";}
            //DKC/002/START
            else {
                // Ignore TotalSize=0 responses
                if (!firstAttachment.contains("<TotalSize>0</TotalSize>")) {
                    // Write attachment to notification-file
                    Directory directory = tuple.getValue0();
                    String filename = dto.getRefToOriginalID() ;
                    // Find the notification type (PDF, Xml or text)
                    if (firstAttachment.startsWith("%PDF-")) filename += ".pdf";
                    else if (firstAttachment.startsWith("<?xml version=")) filename += ".xml";
                        else filename += ".notification";

                    // Check if the filename is free
                    File file = new File(filename);
                    // Add UUID if the filename is already used
                    if(file.exists()) filename+=UUID.randomUUID().toString();

                    Path fileLocationPath = Paths.get(directory.getInDirectory().getAbsolutePath(), filename);
                    String fileLocation = fileLocationPath.toString();

                    // Save bytestream
                    try {
                        Files.write(Paths.get(fileLocation), dto.getFirstAttachmentBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // Replace firstAttachment with "filename" in the notification-text file
                    // Todo: Dette skal gøres ved alle filer, da vi ikke vil have xml data og andet i notifikationsloggen
                    if (firstAttachment.startsWith("%PDF-")) firstAttachment=filename;
                }
            }
            //DKC/002/STOP
            // Append to the notifications-text
            notifications.append("[NOTIFICATION ");
            notifications.append(time);
            notifications.append(']');
            notifications.append('\n');
            notifications.append(firstAttachment);
            notifications.append('\n');
        });
        return notifications;
    }

    // Request "new" notifications from the last 7 minutes.
    private void requestRecentNotifications(LocalDateTime now, Properties properties) {
        LocalDateTime then = now.minusMinutes(7);
        //String certificatePrefix = properties.getProperty("certificatePrefix");
        try {
            as4DkcClient.pushNotificationRequest(then, now, properties, "");
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }
    }


}