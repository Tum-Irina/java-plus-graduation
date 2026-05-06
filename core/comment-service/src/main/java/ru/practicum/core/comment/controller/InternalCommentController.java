package ru.practicum.core.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.comment.mapper.CommentMapper;
import ru.practicum.core.comment.model.Comment;
import ru.practicum.core.comment.model.CommentStatus;
import ru.practicum.core.comment.repository.CommentRepository;
import ru.practicum.core.dto.comment.CommentDto;
import ru.practicum.core.dto.comment.NewCommentDto;
import ru.practicum.core.dto.user.UserDto;
import ru.practicum.core.dto.user.UserShortDto;
import ru.practicum.core.client.UserClient;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/comments")
@RequiredArgsConstructor
@Slf4j
public class InternalCommentController {

    private final CommentRepository commentRepository;
    private final UserClient userClient;

    @PostMapping
    public CommentDto createComment(@RequestParam Long userId,
                                    @RequestParam Long eventId,
                                    @RequestBody NewCommentDto newCommentDto) {
        log.info("Internal POST /internal/comments - создание комментария пользователем {} к событию {}", userId, eventId);

        UserDto userDto = userClient.getUserById(userId);
        UserShortDto author = UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();

        Comment comment = CommentMapper.toEntity(newCommentDto, userId, eventId);
        Comment savedComment = commentRepository.save(comment);

        return CommentMapper.toDto(savedComment, author);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@RequestParam Long userId,
                              @PathVariable Long commentId) {
        log.info("Internal DELETE /internal/comments/{} - удаление комментария пользователем {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));
        commentRepository.delete(comment);
    }

    @GetMapping("/user/{userId}")
    public List<CommentDto> getUserComments(@PathVariable Long userId,
                                            @RequestParam int from,
                                            @RequestParam int size) {
        log.info("Internal GET /internal/comments/user/{} - получение комментариев пользователя", userId);

        UserDto userDto = userClient.getUserById(userId);
        UserShortDto author = UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByAuthorId(userId, pageable).stream()
                .map(comment -> CommentMapper.toDto(comment, author))
                .collect(Collectors.toList());
    }

    @GetMapping("/event/{eventId}")
    public List<CommentDto> getEventComments(@PathVariable Long eventId,
                                             @RequestParam int from,
                                             @RequestParam int size) {
        log.info("Internal GET /internal/comments/event/{} - получение комментариев события", eventId);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable);

        return comments.stream()
                .map(this::getCommentDtoWithUser)
                .collect(Collectors.toList());
    }

    @GetMapping("/event/{eventId}/count")
    public Long getCommentCountByEventId(@PathVariable Long eventId) {
        log.info("Internal GET /internal/comments/event/{}/count - получение количества комментариев события", eventId);
        return commentRepository.countByEventIdAndStatus(eventId, CommentStatus.PUBLISHED);
    }

    private CommentDto getCommentDtoWithUser(Comment comment) {
        UserDto userDto = userClient.getUserById(comment.getAuthorId());
        UserShortDto author = UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();
        return CommentMapper.toDto(comment, author);
    }
}