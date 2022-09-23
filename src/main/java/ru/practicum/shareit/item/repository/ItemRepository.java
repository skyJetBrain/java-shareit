package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface ItemRepository {
    Item add(Item item, long userId);
    Item getById(long itemId);
    Collection<Item> findAll();
    Set<Long> getUserItems(long userId);

}
