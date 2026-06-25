package com.iaihub.toolbox.model.tag;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "tool_tag")
@IdClass(ToolTag.ToolTagId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolTag {

    @Id
    @Column(name = "tool_id")
    private Long toolId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Data
    public static class ToolTagId implements Serializable {
        private Long toolId;
        private Long tagId;
    }
}
