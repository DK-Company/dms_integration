package com.dkcompany.dmsintegration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    private LocalDateTime timestamp;
    private String certificatePrefix;
}
