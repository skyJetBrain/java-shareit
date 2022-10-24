package ru.practicum.shareit.user.dto;


import lombok.Data;
import ru.practicum.shareit.Create;
import ru.practicum.shareit.Update;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
public class UserDto {
    private final Long id;
    @NotBlank(groups = {Create.class})
    private final String name;
    @NotEmpty(groups = {Create.class})
    @Email(groups = {Create.class, Update.class})
    private final String email;
}
