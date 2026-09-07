package com.homelibrary.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Book {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private String title;

  @ManyToMany(fetch = FetchType.EAGER, mappedBy = "books")
  private List<Author> authors;

  @ManyToOne
  private Subcategory subcategory;

  @Enumerated
  private Priority priority;

  public Category getCategory() {
    return this.getSubcategory().getCategory();
  }
}
