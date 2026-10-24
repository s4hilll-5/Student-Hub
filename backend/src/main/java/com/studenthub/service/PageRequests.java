package com.studenthub.service;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.studenthub.exception.BadRequestException;

/** Validates client-supplied paging so users can only sort by whitelisted fields. */
public final class PageRequests {

    public static final int MAX_PAGE_SIZE = 100;

    private PageRequests() {
    }

    /**
     * @param pageable    page/size/sort from the query string
     * @param allowed     entity properties that may be sorted on
     * @param defaultSort used when the client sends no sort
     * @return a page request with a stable id tie-breaker, so rows never jump between pages
     */
    public static Pageable sanitize(Pageable pageable, Set<String> allowed, Sort defaultSort) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Can't sort by '" + order.getProperty() + "'. Use one of: " + String.join(", ", new TreeSet<>(allowed)),
                        "sort");
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, sort);
    }
}
