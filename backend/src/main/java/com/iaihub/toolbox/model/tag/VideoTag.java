package com.iaihub.toolbox.model.tag;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "video_tag")
@IdClass(VideoTag.VideoTagId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoTag {

    @Id
    @Column(name = "video_id")
    private Long videoId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Data
    public static class VideoTagId implements Serializable {
        private Long videoId;
        private Long tagId;
    }
}
