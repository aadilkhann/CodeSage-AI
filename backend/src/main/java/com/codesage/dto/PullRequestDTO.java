package com.codesage.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for PullRequest responses
 */
@Data
public class PullRequestDTO {

    private UUID id;
    private UUID repositoryId;
    private String repositoryFullName;
    private Integer prNumber;
    private String title;
    private String description;
    private String author;
    private String baseBranch;
    private String headBranch;
    private String status;
    private String githubUrl;
    private Integer filesChanged;
    private Integer additions;
    private Integer deletions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
