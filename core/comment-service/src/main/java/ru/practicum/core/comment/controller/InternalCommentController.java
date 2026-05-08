package ru.practicum.core.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.comment.service.InternalCommentService;
import ru.practicum.core.dto.comment.CommentDto;
import ru.practicum.core.dto.comment.NewCommentDto;

import java.util.List;

@RestController
@RequestMapping("/internal/comments")
@RequiredArgsConstructor
@Slf4j
public class InternalCommentController {

    private final InternalCommentService internalCommentService;

    @PostMapping
    public CommentDto createComment(@RequestParam Long userId,
                                    @RequestParam Long eventId,
                                    @RequestBody NewCommentDto newCommentDto) {
        log.info("Internal POST /internal/comments - создание комментария пользователем {} к событию {}", userId, eventId);
        return internalCommentService.createComment(userId, eventId, newCommentDto);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@RequestParam Long userId,
                              @PathVariable Long commentId) {
        log.info("Internal DELETE /internal/comments/{} - удаление комментария пользователем {}", commentId, userId);
        internalCommentService.deleteComment(userId, commentId);
    }

    @GetMapping("/user/{userId}")
    public List<CommentDto> getUserComments(@PathVariable Long userId,
                                            @RequestParam int from,
                                            @RequestParam int size) {
        log.info("Internal GET /internal/comments/user/{} - получение комментариев пользователя", userId);
        return internalCommentService.getUserComments(userId, from, size);
    }

    @GetMapping("/event/{eventId}")
    public List<CommentDto> getEventComments(@PathVariable Long eventId,
                                             @RequestParam int from,
                                             @RequestParam int size) {
        log.info("Internal GET /internal/comments/event/{} - получение комментариев события", eventId);
        return internalCommentService.getEventComments(eventId, from, size);
    }

    @GetMapping("/event/{eventId}/count")
    public Long getCommentCountByEventId(@PathVariable Long eventId) {
        log.info("Internal GET /internal/comments/event/{}/count - получение количества комментариев события", eventId);
        return internalCommentService.getCommentCountByEventId(eventId);
    }
}