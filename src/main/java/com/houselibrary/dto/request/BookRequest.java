package com.houselibrary.dto.request;

import com.houselibrary.model.Priority;
import lombok.Getter;

import java.util.List;

@Getter
public class BookRequest {

  private String title;
  private Long subcategoryId;
  private List<Long> authors;
  private int priority;
}
