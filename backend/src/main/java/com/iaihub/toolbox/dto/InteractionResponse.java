package com.iaihub.toolbox.dto;

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
public class InteractionResponse {

    // Like response fields
    private Boolean liked;
    private Integer likeCount;

    // Favorite response fields
    private Boolean favorited;

    // Comment response fields
    private Long id;
    private String targetType;
    private Long targetId;
    private Long userId;
    private String userName;
    private String userNickname;
    private String userAvatarUrl;
    private Long parentId;
    private Long rootId;
    private String content;
    private Integer commentLikeCount;
    private LocalDateTime createdAt;

    public static InteractionResponse likeToggle(boolean liked, int likeCount) {
        return InteractionResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }

    public static InteractionResponse favoriteToggle(boolean favorited) {
        return InteractionResponse.builder()
                .favorited(favorited)
                .build();
    }
}
