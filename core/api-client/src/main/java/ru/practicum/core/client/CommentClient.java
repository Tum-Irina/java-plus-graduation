package ru.practicum.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.core.dto.comment.CommentDto;
import ru.practicum.core.dto.comment.NewCommentDto;

import java.util.List;

@FeignClient(name = "comment-service")
public interface CommentClient {

    @PostMapping("/internal/comments")
    CommentDto createComment(@RequestParam("userId") Long userId,
                             @RequestParam("eventId") Long eventId,
                             @RequestBody NewCommentDto newCommentDto);

    @DeleteMapping("/internal/comments/{commentId}")
    void deleteComment(@RequestParam("userId") Long userId,
                       @PathVariable("commentId") Long commentId);

    @GetMapping("/internal/comments/user/{userId}")
    List<CommentDto> getUserComments(@PathVariable("userId") Long userId,
                                     @RequestParam("from") int from,
                                     @RequestParam("size") int size);

    @GetMapping("/internal/comments/event/{eventId}")
    List<CommentDto> getEventComments(@PathVariable("eventId") Long eventId,
                                      @RequestParam("from") int from,
                                      @RequestParam("size") int size);

    @GetMapping("/internal/comments/event/{eventId}/count")
    Long getCommentCountByEventId(@PathVariable("eventId") Long eventId);
}