package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.util.As4DkcClient;
import com.dkcompany.dmsintegration.entity.Notification;
import com.dkcompany.dmsintegration.repository.NotificationRepository;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import dk.toldst.eutk.as4client.utilities.Tools;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// @Configuration
// @EnableScheduling
public class NotificationService {
    private final As4DkcClient as4DkcClient;
    private final NotificationRepository notificationRepository;

    public NotificationService(
            As4DkcClient as4DkcClient,
            NotificationRepository notificationRepository
    ) {
        this.as4DkcClient = as4DkcClient;
        this.notificationRepository = notificationRepository;
    }

   // @Scheduled(fixedDelay = 10000)
    public void requestNotifications() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        try {
            // if (hasNoUnrequestedNotifications(now)) {
            //     requestOldNotifications(now);
            // } else {
                requestRecentNotifications(now);
            // }
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }
    }

    // @Scheduled(fixedDelay = 1000)
    public void pullNotifications() throws AS4Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String time = now.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        long startTime = System.nanoTime();
        As4ClientResponseDto as4ClientResponseDto = as4DkcClient.pullNotifications("oces3");
        long endTime = System.nanoTime();

        // TODO: while loop that terminates when all notifications have been pulled
        // while (true) {
        //     As4ClientResponseDto response = as4DkcClient.pullNotifications();
        //     // do something with the response
        //     if (response.getReftoOriginalID() == null) {
        //         break;
        //     }
        // }

        long duration = (endTime - startTime) / 1000000;
        System.out.println("Notification pull duration: " + duration + "ms");

        String firstAttachment = as4ClientResponseDto.getFirstAttachment();
        if (firstAttachment == null) {
            firstAttachment = "Empty.";
        }

        StringBuilder notification = new StringBuilder();
        notification.append("[NOTIFICATION ");
        notification.append(time);
        notification.append(']');
        notification.append('\n');
        notification.append(firstAttachment);
        notification.append('\n');

        System.out.println(notification);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Files\\notifications.txt", true))) {
            writer.write(notification.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestRecentNotifications(LocalDateTime now) throws AS4Exception {
        LocalDateTime then = now.minusMinutes(5);

        As4ClientResponseDto notificationPushResponseDto = as4DkcClient.pushNotificationRequest(then, now, "oces3");

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
        requestRecentNotifications(now);
        // TODO: This method should implement logic that requests all missing notifications
        // LocalDateTime latestNotificationTimestamp = getLatestNotificationTimestamp();
        // if (latestNotificationTimestamp == null || hasNoUnrequestedNotifications(now, latestNotificationTimestamp)) {
        //     requestRecentNotifications(now);
        // } else {
        //     As4ClientResponseDto notificationPushResponseDto = as4DkcClient.pushNotificationRequest(latestNotificationTimestamp, now);
        //
        //     String notificationResultCode = Tools.getStatus(
        //                     notificationPushResponseDto
        //                             .getFirstAttachment())
        //             .getCode();
        //
        //     String time = now.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        //     System.out.printf("Status for notification push request was %s (%s)%n", notificationResultCode, time);
        //
        //     Notification notification = Notification
        //             .builder()
        //             .timestamp(now)
        //             .build();
        //     notificationRepository.save(notification);
        // }
    }

    private LocalDateTime getLatestNotificationTimestamp() {
        Notification latestNotification = notificationRepository.findFirstByOrderByTimestampDesc();
        if (latestNotification == null) {
            return null;
        }

        return latestNotification.getTimestamp();
    }

    private boolean hasNoUnrequestedNotifications(LocalDateTime now, LocalDateTime latestNotificationTimestamp) {
        return !latestNotificationTimestamp.isAfter(now.minusMinutes(5));
    }
}