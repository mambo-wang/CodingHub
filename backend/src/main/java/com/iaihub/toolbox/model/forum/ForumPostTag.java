package com.iaihub.toolbox.model.forum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "forum_post_tag")
@IdClass(ForumPostTag.ForumPostTagId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostTag {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Data
    public static class ForumPostTagId implements Serializable {
        private Long postId;
        private Long tagId;
    }
}