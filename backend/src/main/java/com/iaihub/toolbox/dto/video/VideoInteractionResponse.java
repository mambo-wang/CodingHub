package com.iaihub.toolbox.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoInteractionResponse {

    private Boolean liked;
    private Boolean favorited;
    private Integer likeCount;
    private Integer commentCount;
}
