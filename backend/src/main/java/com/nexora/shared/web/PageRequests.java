package com.nexora.shared.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Builds bounded, whitelisted page requests so clients cannot sort by arbitrary columns. */
public final class PageRequests {

    private PageRequests() {
    }

    /**
     * @param sort        client value such as {@code name,asc}; ignored if the field is not whitelisted
     * @param defaultSort field name, prefixed with {@code -} for descending
     */
    public static Pageable of(int page, int size, String sort, Set<String> allowed, String defaultSort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        boolean defaultDesc = defaultSort.startsWith("-");
        String field = defaultDesc ? defaultSort.substring(1) : defaultSort;
        Sort.Direction dir = defaultDesc ? Sort.Direction.DESC : Sort.Direction.ASC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (allowed.contains(parts[0])) {
                field = parts[0];
                dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            }
        }
        return PageRequest.of(safePage, safeSize, Sort.by(dir, field).and(Sort.by(Sort.Direction.DESC, "id")));
    }
}
