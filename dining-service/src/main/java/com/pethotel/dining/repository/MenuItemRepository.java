package com.pethotel.dining.repository;

import com.pethotel.dining.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    // findByAvailableTrue() — derived query: SELECT * FROM dining.menu_items WHERE available = true
    // Используется для отображения меню клиентам (скрывает временно недоступные позиции).
    List<MenuItem> findByAvailableTrue();

    // findByCategory — фильтрация по категории для группировки на фронте ("Завтрак", "Ужин" и т.д.).
    List<MenuItem> findByCategory(String category);
}
