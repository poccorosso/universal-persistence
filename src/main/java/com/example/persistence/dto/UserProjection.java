package com.example.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for user projections
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProjection {
    
    private Long id;
    private String uid;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}