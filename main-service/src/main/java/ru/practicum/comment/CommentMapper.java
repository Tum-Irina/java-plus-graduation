package ru.practicum.comment;

import ru.practicum.user.UserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommentMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .created(comment.getCreated().format(FORMATTER))
                .status(comment.getStatus().toString())
                .build();
    }

    public static Comment toEntity(NewCommentDto newCommentDto) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();
    }
}