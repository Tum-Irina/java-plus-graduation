package ru.practicum.core.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.comment.service.CommentService;
import ru.practicum.core.dto.comment.CommentDto;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/events/{eventId}")
    public List<CommentDto> getEventComments(
            @PathVariable @Positive Long eventId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /comments/events/{}?from={}&size={}", eventId, from, size);
        return commentService.getEventComments(eventId, from, size);
    }
}