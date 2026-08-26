package com.iaihub.toolbox.model;

import com.iaihub.toolbox.exception.BusinessException;

import java.util.Set;

/**
 * Polymorphic target type for unified interactions.
 * Used to identify which content module a like/comment/favorite belongs to.
 */
public enum TargetType {

    TOOL,
    FORUM_POST,
    VIDEO,
    PLUGIN;

    private static final Set<String> VALID_VALUES = Set.of("TOOL", "FORUM_POST", "VIDEO", "PLUGIN");

    /**
     * Validate and parse a string into TargetType.
     *
     * @param value the string value to parse
     * @return the parsed TargetType
     * @throws BusinessException if the value is not a valid target type
     */
    public static TargetType fromString(String value) {
        if (value == null || !VALID_VALUES.contains(value.toUpperCase())) {
            throw new BusinessException(400, "Invalid targetType: " + value + ". Must be one of: TOOL, FORUM_POST, VIDEO, PLUGIN");
        }
        return TargetType.valueOf(value.toUpperCase());
    }
}
