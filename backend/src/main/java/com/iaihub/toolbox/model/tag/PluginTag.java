package com.iaihub.toolbox.model.tag;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "plugin_tag")
@IdClass(PluginTag.PluginTagId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginTag {

    @Id
    @Column(name = "plugin_id")
    private Long pluginId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Data
    public static class PluginTagId implements Serializable {
        private Long pluginId;
        private Long tagId;
    }
}
