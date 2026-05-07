package com.pethotel.dining.service;

import com.pethotel.dining.dto.MenuItemDto;
import com.pethotel.dining.dto.MenuItemRequest;
import com.pethotel.dining.entity.MenuItem;
import com.pethotel.dining.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;

    // @Cacheable("menu-items") — кэшируем весь список меню в Redis на 1 час (TTL задан в CacheConfig).
    // Меню меняется редко (ADMIN добавляет позиции), поэтому долгий кэш оправдан.
    // @CacheEvict на методах create/update/delete инвалидирует кэш при каждом изменении.
    // new ArrayList<>() — см. аналогичный комментарий в RoomService: защита от неизменяемого List при десериализации из Redis.
    @Cacheable("menu-items")
    public List<MenuItemDto> getAll() {
        log.info("Fetching all menu items");
        return new ArrayList<>(menuItemRepository.findAll().stream().map(this::toDto).toList());
    }

    // getById не кэшируется: вызывается редко (только при заказе), и результат уже в кэше getAll().
    public MenuItemDto getById(Long id) {
        log.info("Fetching menu item by id={}", id);
        return toDto(findItem(id));
    }

    @Transactional
    @CacheEvict(value = "menu-items", allEntries = true)
    public MenuItemDto create(MenuItemRequest request) {
        MenuItem item = MenuItem.builder()
                .name(request.getName())
                .price(request.getPrice())
                .category(request.getCategory())
                .available(request.isAvailable())
                .build();
        item = menuItemRepository.save(item);
        log.info("Menu item created: id={} name={}", item.getId(), item.getName());
        return toDto(item);
    }

    @Transactional
    @CacheEvict(value = "menu-items", allEntries = true)
    public MenuItemDto update(Long id, MenuItemRequest request) {
        MenuItem item = findItem(id);
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setAvailable(request.isAvailable());
        log.info("Menu item updated: id={}", id);
        return toDto(menuItemRepository.save(item));
    }

    @Transactional
    @CacheEvict(value = "menu-items", allEntries = true)
    public void delete(Long id) {
        if (!menuItemRepository.existsById(id)) {
            throw new NoSuchElementException("Menu item not found: " + id);
        }
        menuItemRepository.deleteById(id);
        log.info("Menu item deleted: id={}", id);
    }

    // public (не private): OrderService напрямую вызывает findItem для получения цены и проверки доступности.
    // Дублировать логику "найти или 404" в OrderService не нужно.
    public MenuItem findItem(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Menu item not found: " + id));
    }

    private MenuItemDto toDto(MenuItem item) {
        MenuItemDto dto = new MenuItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setPrice(item.getPrice());
        dto.setCategory(item.getCategory());
        dto.setAvailable(item.isAvailable());
        return dto;
    }
}
