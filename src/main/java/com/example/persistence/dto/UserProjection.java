package com.example.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user projections
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProjection {
    
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Integer age;
    private String status;
}