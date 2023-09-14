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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Configuration
@EnableScheduling
public class NotificationService {
    private final As4DkcClient as4DkcClient;
    private final NotificationRepository notificationRepository;

    public NotificationService(As4DkcClient as4DkcClient, NotificationRepository notificationRepository) {
        this.as4DkcClient = as4DkcClient;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(fixedRate = 10000)
    public void requestNotifications() throws AS4Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (hasUnrequestedNotifications(now)) {
            requestOldNotifications();
        } else {
            requestRecentNotifications(now);
        }

        pullNotifications();
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

    private void requestOldNotifications() {
        throw new UnsupportedOperationException("method not implemented");
    }

    private void pullNotifications() throws AS4Exception {
        As4ClientResponseDto as4ClientResponseDto = as4DkcClient.pullNotifications();
        String firstAttachment = as4ClientResponseDto.getFirstAttachment();
        if (firstAttachment == null) {
            System.out.println("Pull notifications response was null");
        } else {
            System.out.println("Pull notifications response was not null");
        }
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