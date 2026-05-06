package ru.practicum.core.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.client.EventClient;
import ru.practicum.core.client.RequestClient;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.comment.mapper.CommentMapper;
import ru.practicum.core.comment.model.Comment;
import ru.practicum.core.comment.model.CommentStatus;
import ru.practicum.core.comment.repository.CommentRepository;
import ru.practicum.core.dto.comment.CommentDto;
import ru.practicum.core.dto.comment.NewCommentDto;
import ru.practicum.core.dto.event.EventState;
import ru.practicum.core.dto.user.UserDto;
import ru.practicum.core.dto.user.UserShortDto;
import ru.practicum.core.exception.ConditionsNotMetException;
import ru.practicum.core.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestClient requestClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);

        // Проверяем существование пользователя
        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        // Проверяем существование события
        if (!eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }

        // Проверяем, что событие опубликовано
        EventState eventState = eventClient.getEventState(eventId);
        if (eventState != EventState.PUBLISHED) {
            throw new ConditionsNotMetException("Комментировать можно только опубликованные события");
        }

        // Проверяем, что пользователь посетил событие (имел подтвержденную заявку)
        boolean hasParticipated = requestClient.hasConfirmedRequest(userId, eventId);
        if (!hasParticipated) {
            throw new ConditionsNotMetException("Комментировать могут только участники события");
        }

        // Создаем комментарий - ВСЕ комментарии на модерацию
        Comment comment = CommentMapper.toEntity(newCommentDto, userId, eventId);
        comment.setStatus(CommentStatus.PENDING);

        Comment savedComment = commentRepository.save(comment);
        log.info("Создан комментарий с ID {}", savedComment.getId());

        return getCommentDtoWithUser(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        log.info("Получение комментариев пользователя {}", userId);

        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

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

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        log.info("Получение комментариев события {}", eventId);

        if (!eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }

        Pageable pageable = PageRequest.of(from / size, size);

        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable);

        return comments.stream()
                .map(this::getCommentDtoWithUser)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsForModeration(int from, int size) {
        log.info("Получение комментариев на модерацию");

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByStatus(CommentStatus.PENDING, pageable).stream()
                .map(this::getCommentDtoWithUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, Boolean approve) {
        log.info("Модерация комментария {}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConditionsNotMetException("Можно модерировать только комментарии на рассмотрении");
        }

        comment.setStatus(approve ? CommentStatus.PUBLISHED : CommentStatus.REJECTED);
        Comment moderatedComment = commentRepository.save(comment);

        return getCommentDtoWithUser(moderatedComment);
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