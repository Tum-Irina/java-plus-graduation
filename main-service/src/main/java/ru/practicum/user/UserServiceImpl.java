package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Создание пользователя с email: {}", newUserRequest.getEmail());

        User user = UserMapper.toEntity(newUserRequest);
        User savedUser = userRepository.save(user);
        log.info("Пользователь создан с ID: {}", savedUser.getId());

        return UserMapper.toDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Pageable pageable) {
        log.info("Получение пользователей по IDs: {}, пагинация: {}", ids, pageable);

        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            users = userRepository.findByIdIn(ids, pageable);
        }

        log.info("Найдено {} пользователей", users.size());
        return users.stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя с ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с ID {} не найден", userId);
            throw new NotFoundException(String.format("Пользователь с ID %d не найден", userId));
        }

        userRepository.deleteById(userId);
        log.info("Пользователь с ID {} успешно удален", userId);
    }
}