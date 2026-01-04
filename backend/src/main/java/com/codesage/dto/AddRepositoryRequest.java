package com.codesage.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a new repository
 */
@Data
public class AddRepositoryRequest {

    @NotNull(message = "GitHub repository ID is required")
    private Long githubRepoId;

    @NotBlank(message = "Repository full name is required")
    private String fullName;

    private String description;

    private String language;
}
