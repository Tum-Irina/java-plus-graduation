package ru.practicum.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.Event;
import ru.practicum.event.State;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);

        // Проверяем существование пользователя
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        // Проверяем существование события
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        // Проверяем, что событие опубликовано
        if (event.getState() != State.PUBLISHED) {
            throw new ConditionsNotMetException("Комментировать можно только опубликованные события");
        }

        // Проверяем, что пользователь посетил событие (имел подтвержденную заявку)
        boolean hasParticipated = requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED
        );

        if (!hasParticipated) {
            throw new ConditionsNotMetException("Комментировать могут только участники события");
        }

        // Создаем комментарий - ВСЕ комментарии на модерацию
        Comment comment = CommentMapper.toEntity(newCommentDto);
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setStatus(CommentStatus.PENDING); // Всегда на модерации

        Comment savedComment = commentRepository.save(comment);
        log.info("Создан комментарий с ID {}", savedComment.getId());

        return CommentMapper.toDto(savedComment);
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

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByAuthorId(userId, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        log.info("Получение комментариев события {}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsForModeration(int from, int size) {
        log.info("Получение комментариев на модерацию");

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByStatus(CommentStatus.PENDING, pageable).stream()
                .map(CommentMapper::toDto)
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

        return CommentMapper.toDto(moderatedComment);
    }
}