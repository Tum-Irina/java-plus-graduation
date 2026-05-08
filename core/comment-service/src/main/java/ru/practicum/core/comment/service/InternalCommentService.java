package ru.practicum.core.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.comment.mapper.CommentMapper;
import ru.practicum.core.comment.model.Comment;
import ru.practicum.core.comment.model.CommentStatus;
import ru.practicum.core.comment.repository.CommentRepository;
import ru.practicum.core.dto.comment.CommentDto;
import ru.practicum.core.dto.comment.NewCommentDto;
import ru.practicum.core.dto.user.UserDto;
import ru.practicum.core.dto.user.UserShortDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InternalCommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;

    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.debug("Internal создание комментария пользователем {} к событию {}", userId, eventId);

        UserDto userDto = userClient.getUserById(userId);
        UserShortDto author = UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();

        Comment comment = CommentMapper.toEntity(newCommentDto, userId, eventId);
        Comment savedComment = commentRepository.save(comment);

        return CommentMapper.toDto(savedComment, author);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.debug("Internal удаление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));
        commentRepository.delete(comment);
    }

    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        log.debug("Internal получение комментариев пользователя {}", userId);

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

    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        log.debug("Internal получение комментариев события {}", eventId);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable);

        return comments.stream()
                .map(this::getCommentDtoWithUser)
                .collect(Collectors.toList());
    }

    public Long getCommentCountByEventId(Long eventId) {
        log.debug("Internal получение количества комментариев события {}", eventId);
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