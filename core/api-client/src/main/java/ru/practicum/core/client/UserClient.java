package ru.practicum.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.user.UserDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users")
    Map<Long, UserDto> getUsersByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/internal/users/{userId}/exists")
    Boolean userExists(@PathVariable("userId") Long userId);
}