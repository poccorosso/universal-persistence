package com.example.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Page response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    /**
     * Page content
     */
    private List<T> content;
    
    /**
     * Total number of elements
     */
    private long totalElements;
    
    /**
     * Total number of pages
     */
    private int totalPages;
    
    /**
     * Current page number
     */
    private int currentPage;
    
    /**
     * Page size
     */
    private int pageSize;
    
    /**
     * Check if this is the first page
     */
    public boolean isFirst() {
        return currentPage == 0;
    }
    
    /**
     * Check if this is the last page
     */
    public boolean isLast() {
        return currentPage == totalPages - 1;
    }
    
    /**
     * Check if there is a next page
     */
    public boolean hasNext() {
        return currentPage < totalPages - 1;
    }
    
    /**
     * Check if there is a previous page
     */
    public boolean hasPrevious() {
        return currentPage > 0;
    }
}