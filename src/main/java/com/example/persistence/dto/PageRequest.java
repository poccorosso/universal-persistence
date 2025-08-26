package com.example.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Page request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /**
     * Page number (starts from 0)
     */
    @Builder.Default
    private int page = 0;

    /**
     * Page size
     */
    @Builder.Default
    private int size = 20;

    /**
     * Sort criteria
     */
    private List<SortCriteria> sorts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortCriteria {
        /**
         * Sort field
         */
        private String field;

        /**
         * Sort direction
         */
        @Builder.Default
        private SortDirection direction = SortDirection.ASC;

        public enum SortDirection {
            ASC, DESC
        }
    }
}
