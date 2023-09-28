package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.util.As4DkcClient;
import com.dkcompany.dmsintegration.entity.Notification;
import com.dkcompany.dmsintegration.repository.NotificationRepository;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import dk.toldst.eutk.as4client.utilities.Tools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableScheduling
public class NotificationService {
    private final As4DkcClient as4DkcClient;
    private final NotificationRepository notificationRepository;

    public NotificationService(
            As4DkcClient as4DkcClient,
            NotificationRepository notificationRepository) {
        this.as4DkcClient = as4DkcClient;
        this.notificationRepository = notificationRepository;
    }

//    @Scheduled(fixedDelay = 10000)
    public void uploadDocument() throws AS4Exception {
        var dto = as4DkcClient.SubmitDeclarationExample();

        var uploadStatus = Tools.getStatus(dto.getFirstAttachment()).getCode();
        System.out.println("Upload response: " + uploadStatus);
    }

//    @Scheduled(fixedDelay = 10000)
    public void requestNotifications() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        try {
            if (hasUnrequestedNotifications(now)) {
                requestOldNotifications(now);
            } else {
                requestRecentNotifications(now);
            }
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @Scheduled(fixedDelay = 1000)
    public void pullNotifications() throws AS4Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String time = now.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder notificationEntry = new StringBuilder();

        long startTime = System.nanoTime();
        As4ClientResponseDto as4ClientResponseDto = as4DkcClient.pullNotifications();
        long endTime = System.nanoTime();

        long duration = (endTime - startTime) / 1000000;
        System.out.println("Duration: " + duration + "ms");

        String notification = as4ClientResponseDto.getFirstAttachment();
        if (notification == null) {
            notification = "Empty.\n";
        }

        System.out.println("Notifications pulled:\n" + notification);

        notificationEntry.append('[');
        notificationEntry.append(time);
        notificationEntry.append(']');
        notificationEntry.append('\n');
        notificationEntry.append(notification);
        notificationEntry.append('\n');

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Files\\notifications.txt", true));
            writer.write(notificationEntry.toString());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestRecentNotifications(LocalDateTime now) throws AS4Exception {
        As4ClientResponseDto notificationPushResponseDto = as4DkcClient.pushNotificationRequest(now);

        String notificationResultCode = Tools.getStatus(
                notificationPushResponseDto
                        .getFirstAttachment())
                        .getCode();

        String time = now.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("Status for notification push request was %s (%s)%n", notificationResultCode, time);

        Notification notification = Notification
                .builder()
                .timestamp(now)
                .build();
        notificationRepository.save(notification);
    }

    private void requestOldNotifications(LocalDateTime now) throws AS4Exception {
        // TODO: This method should implement logic that requests all missing notifications
        requestRecentNotifications(now);
    }

    private boolean hasUnrequestedNotifications(LocalDateTime now) {
        Notification latestNotification = notificationRepository.findFirstByOrderByTimestampDesc();

        if (latestNotification == null) {
            return false;
        }

        LocalDateTime notificationTimestamp = latestNotification.getTimestamp();
        if (notificationTimestamp.isAfter(now.minusMinutes(5))) {
            return false;
        }

        return true;
    }
}