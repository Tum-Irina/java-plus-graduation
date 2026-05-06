package ru.practicum.core.comment.mapper;

import ru.practicum.core.comment.model.Comment;
import ru.practicum.core.comment.model.CommentStatus;
import ru.practicum.core.dto.comment.CommentDto;
import ru.practicum.core.dto.comment.NewCommentDto;
import ru.practicum.core.dto.user.UserShortDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommentMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CommentDto toDto(Comment comment, UserShortDto author) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(author)
                .eventId(comment.getEventId())
                .created(comment.getCreated().format(FORMATTER))
                .status(comment.getStatus().toString())
                .build();
    }

    public static Comment toEntity(NewCommentDto newCommentDto, Long authorId, Long eventId) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .authorId(authorId)
                .eventId(eventId)
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();
    }
}