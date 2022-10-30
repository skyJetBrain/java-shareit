package ru.practicum.shareit.item.model;

import lombok.*;
import ru.practicum.shareit.request.model.ItemRequest;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "available")
    private Boolean available;
    @CollectionTable(name = "users", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "userId")
    private Long userId;
    @ManyToOne
    @JoinColumn(name = "item_request_id")
    private ItemRequest itemRequest;
}


