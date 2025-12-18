package com.openmailer.openmailer.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Paginated API response wrapper
 */
public class PaginatedResponse<T> {
    private boolean success;
    private List<T> data;
    private PaginationInfo pagination;
    private LocalDateTime timestamp;

    public PaginatedResponse() {
    }

    public PaginatedResponse(boolean success, List<T> data, PaginationInfo pagination, LocalDateTime timestamp) {
        this.success = success;
        this.data = data;
        this.pagination = pagination;
        this.timestamp = timestamp;
    }

    public PaginatedResponse(List<T> data, PaginationInfo pagination) {
        this.success = true;
        this.data = data;
        this.pagination = pagination;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static class PaginationInfo {
        private int page;
        private int size;
        private long total;
        private int totalPages;

        public PaginationInfo() {
        }

        public PaginationInfo(int page, int size, long total, int totalPages) {
            this.page = page;
            this.size = size;
            this.total = total;
            this.totalPages = totalPages;
        }

        public PaginationInfo(int page, int size, long total) {
            this.page = page;
            this.size = size;
            this.total = total;
            this.totalPages = (int) Math.ceil((double) total / size);
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }
}
