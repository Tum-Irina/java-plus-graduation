package ru.practicum.core.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.user.UserDto;
import ru.practicum.core.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserDto getUserById(@PathVariable Long userId) {
        log.info("Internal GET /internal/users/{}", userId);
        return userService.getUserById(userId);
    }

    @GetMapping
    public Map<Long, UserDto> getUsersByIds(@RequestParam List<Long> ids) {
        log.info("Internal GET /internal/users?ids={}", ids);
        return userService.getUsersByIds(ids).stream()
                .collect(Collectors.toMap(UserDto::getId, u -> u));
    }

    @GetMapping("/{userId}/exists")
    public Boolean userExists(@PathVariable Long userId) {
        log.info("Internal GET /internal/users/{}/exists", userId);
        return userService.userExists(userId);
    }
}