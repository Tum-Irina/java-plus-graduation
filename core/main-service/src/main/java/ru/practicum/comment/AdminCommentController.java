package ru.practicum.comment;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping("/moderation")
    public List<CommentDto> getCommentsForModeration(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /admin/comments/moderation?from={}&size={}", from, size);
        return commentService.getCommentsForModeration(from, size);
    }

    @PatchMapping("/{commentId}/moderate")
    public CommentDto moderateComment(
            @PathVariable @Positive Long commentId,
            @RequestParam Boolean approve) {
        log.info("PATCH /admin/comments/{}/moderate?approve={}", commentId, approve);
        return commentService.moderateComment(commentId, approve);
    }
}