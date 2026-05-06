package ru.practicum.core.user.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.core.dto.user.UserDto;
import ru.practicum.core.user.dto.NewUserRequest;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long userId);

    UserDto getUserById(Long userId);

    List<UserDto> getUsersByIds(List<Long> ids);

    Boolean userExists(Long userId);
}