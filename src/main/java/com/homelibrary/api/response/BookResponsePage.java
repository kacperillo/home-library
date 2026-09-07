package com.homelibrary.api.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookResponsePage {
    private List<BookResponse> content;
    private Integer pageNo;
    private Integer pageSize;
    private Integer totalElements;
    private Integer totalPages;
}
