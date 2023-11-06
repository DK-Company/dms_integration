package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.util.As4DkcClient;
import com.dkcompany.dmsintegration.entity.Notification;
import com.dkcompany.dmsintegration.repository.NotificationRepository;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
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

    public void requestNotifications(String certificatePrefix) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (hasNoUnrequestedNotifications(now, certificatePrefix)) {
            requestOldNotifications(now, certificatePrefix);
        } else {
            requestRecentNotifications(now, certificatePrefix);
        }
    }

    public List<As4ClientResponseDto> pullNotifications(String certificatePrefix) {
        List<As4ClientResponseDto> dtos = new ArrayList<>();

        while (true) {
            As4ClientResponseDto dto = as4DkcClient.pullNotifications(certificatePrefix);
            dtos.add(dto);

            if (dto.getReftoOriginalID() == null) {
                break;
            }
        }

        return dtos;
    }

    public void saveNotifications(Pair<Directory, List<As4ClientResponseDto>> tuple) {
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

    public Void pushNotificationRequests(Directory directory) {
        String certificatePrefix = directory.getCertificatePrefix();
        String nowFormatted = LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("Pushing notification request for %s (%s).%n", directory.getBaseDirectory(), nowFormatted);

        requestNotifications(certificatePrefix);

        return null;
    }

    public Pair<Directory, List<As4ClientResponseDto>> pullNotifications(Directory directory) {
        String nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("Pulling notifications for %s (%s).%n", directory.getBaseDirectory(), nowFormatted);

        List<As4ClientResponseDto> dtos = pullNotifications(directory.getCertificatePrefix());
        return new Pair<Directory, List<As4ClientResponseDto>>(
                directory,
                dtos
        );
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

    private void requestRecentNotifications(LocalDateTime now, String certificatePrefix) {
        LocalDateTime then = now.minusMinutes(5);

        try {
            as4DkcClient.pushNotificationRequest(then, now, certificatePrefix);
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }

        saveNotificationToRepository(now, certificatePrefix);
    }

    private boolean hasNoUnrequestedNotifications(LocalDateTime now, String certificatePrefix) {
        LocalDateTime latestNotificationTimestamp = getLatestNotificationTimestamp(certificatePrefix);
        if (latestNotificationTimestamp == null) {
            return true;
        }
        return !latestNotificationTimestamp.isAfter(now.minusMinutes(5));
    }

    private void requestOldNotifications(LocalDateTime now, String certificatePrefix) {
        if (hasNoUnrequestedNotifications(now, certificatePrefix)) {
            requestRecentNotifications(now, certificatePrefix);
        } else {
            LocalDateTime latestNotificationTimestamp = getLatestNotificationTimestamp(certificatePrefix);
            try {
                as4DkcClient.pushNotificationRequest(latestNotificationTimestamp, now, certificatePrefix);
            } catch (AS4Exception e) {
                throw new RuntimeException(e);
            }

            saveNotificationToRepository(now, certificatePrefix);
        }
    }

    private void saveNotificationToRepository(LocalDateTime now, String certificatePrefix) {
        int offset = new Random().nextInt(2000000) - 1000000;
        LocalDateTime nowWithNoise = now.plusNanos(offset);

        Notification notification = Notification
                .builder()
                .timestamp(nowWithNoise)
                .certificatePrefix(certificatePrefix)
                .build();
        notificationRepository.save(notification);
    }

    private LocalDateTime getLatestNotificationTimestamp(String certificatePrefix) {
        Notification latestNotification = notificationRepository
                .findFirstByCertificatePrefixOrderByTimestampDesc(certificatePrefix);
        if (latestNotification == null) {
            return null;
        }

        return latestNotification.getTimestamp();
    }
}