package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.util.As4DkcClient;
import com.dkcompany.dmsintegration.entity.Notification;
import com.dkcompany.dmsintegration.repository.NotificationRepository;
import dk.skat.mft.dms_declaration_status._1.StatusResponseType;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
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
            var result = requestOldNotifications();
        }

        var result = requestRecentNotifications(now);
    }

    private StatusResponseType requestRecentNotifications(LocalDateTime now) throws AS4Exception {
        StatusResponseType notificationPushRequestResult = as4DkcClient.pushNotificationRequest(now);
        String notificationCode = notificationPushRequestResult.getCode();

        String time = now.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("Status for notification push request was %s (%s)%n", notificationCode, time);

        Notification notification = Notification
                .builder()
                //.timestamp(now)
                .timestamp(now.minusMinutes(10))
                .build();
        notificationRepository.save(notification);

        return notificationPushRequestResult;
    }

    private StatusResponseType requestOldNotifications() {
        throw new UnsupportedOperationException("method not implemented");
    }

    private boolean hasUnrequestedNotifications(LocalDateTime now) {
        Iterable<Notification> notifications = notificationRepository.findAll();
        Optional<Notification> latestNotificationOption = StreamSupport
                .stream(notifications.spliterator(), false)
                .max(Comparator.comparing(Notification::getId));

        if (latestNotificationOption.isEmpty()) {
            return false;
        }

        Notification latestNotification = latestNotificationOption.get();
        LocalDateTime notificationTimestamp = latestNotification.getTimestamp();

        if (notificationTimestamp.isAfter(now.minusMinutes(5))) {
            return false;
        }

        return true;
    }
}