package com.codesage.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Repository responses
 * Excludes sensitive data like webhook secrets
 */
@Data
public class RepositoryDTO {

    private UUID id;
    private Long githubRepoId;
    private String fullName;
    private String description;
    private String language;
    private Boolean isActive;
    private Map<String, Object> settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
