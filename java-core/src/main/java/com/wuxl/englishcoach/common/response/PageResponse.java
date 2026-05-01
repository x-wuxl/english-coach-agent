package com.wuxl.englishcoach.common.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> items;
    private Integer page;
    private Integer size;
    private Long total;
    private Integer totalPages;
}
