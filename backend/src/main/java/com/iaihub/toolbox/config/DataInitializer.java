package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            log.info("Initializing default categories...");

            List<Category> categories = List.of(
                Category.builder().name("Skill").icon("🛠️").sortOrder(1).build(),
                Category.builder().name("MCP").icon("🔌").sortOrder(2).build(),
                Category.builder().name("API").icon("🌐").sortOrder(3).build(),
                Category.builder().name("Prompt").icon("💬").sortOrder(4).build(),
                Category.builder().name("其他").icon("📦").sortOrder(5).build()
            );

            categoryRepository.saveAll(categories);
            log.info("Default categories initialized: {}", categories.size());
        }
    }
}
