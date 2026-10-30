package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.service.CategoryService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto add(@Valid @RequestBody NewCategoryDto dto) {
        log.info("POST запрос на создание категории: {}", dto.getName());
        return categoryService.add(dto);
    }

    @PatchMapping("/admin/categories/{catId}")
    public CategoryDto update(@PathVariable Long catId, @Valid @RequestBody CategoryDto dto) {
        log.info("PATCH запрос на обновление категории с id={}", catId);
        return categoryService.update(catId, dto);
    }

    @DeleteMapping("/admin/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catId) {
        log.info("DELETE запрос на удаление категории с id={}", catId);
        categoryService.delete(catId);
    }

    @GetMapping("/categories")
    public List<CategoryDto> getAll(@RequestParam(defaultValue = "0") Integer from,
                                    @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET запрос на получение списка категорий, from={}, size={}", from, size);
        return categoryService.getAll(from, size);
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto getById(@PathVariable Long catId) {
        log.info("GET запрос на получение категории с id={}", catId);
        return categoryService.getById(catId);
    }
}