package com.dkcompany.dmsintegration.repository;

import com.dkcompany.dmsintegration.entity.Notification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, LocalDateTime> {
    Notification findFirstByCertificatePrefixOrderByTimestampDesc(String certificatePrefix);
}
