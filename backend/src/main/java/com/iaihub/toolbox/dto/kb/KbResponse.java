package com.iaihub.toolbox.dto.kb;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KbResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerNickname;
    private Long documentCount;
    private String ragCollection;
    private LocalDateTime createdAt;
}
