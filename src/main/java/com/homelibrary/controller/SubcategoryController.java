package com.homelibrary.controller;

import com.homelibrary.api.request.SubcategoryRequest;
import com.homelibrary.api.request.UpdateNameRequest;
import com.homelibrary.api.response.SubcategoryResponse;

import com.homelibrary.service.SubcategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    @PostMapping
    public ResponseEntity<SubcategoryResponse> addSubcategory(@Valid @RequestBody SubcategoryRequest subcategoryRequest) {
        SubcategoryResponse subcategoryResponse = subcategoryService.addSubcategory(subcategoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(subcategoryResponse);
    }

    @PutMapping("/{subcategoryId}")
    public ResponseEntity<SubcategoryResponse> updateSubcategoryName(
            @PathVariable Integer subcategoryId, @Valid @RequestBody UpdateNameRequest updateNameRequest) {
        SubcategoryResponse subcategoryResponse =
                subcategoryService.updateSubcategoryName(subcategoryId, updateNameRequest);
        return ResponseEntity.ok().body(subcategoryResponse);
    }

    @DeleteMapping("/{subcategoryId}")
    public ResponseEntity<Void> deleteSubcategory(@PathVariable Integer subcategoryId) {
        subcategoryService.deleteSubcategory(subcategoryId);
        return ResponseEntity.noContent().build();
    }
}
