package com.iaihub.toolbox.dto.notification;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private String type;
    private String targetType;
    private Long targetId;
    private String message;
    private Long actorId;
    private String actorName;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
