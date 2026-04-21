package ru.practicum.comment;

import java.util.List;

public interface CommentService {

    // Private API (для пользователей)
    // Пользователь создает комментарий
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    // Пользователь удаляет свой комментарий
    void deleteComment(Long userId, Long commentId);

    // Получить комментарии пользователя
    List<CommentDto> getUserComments(Long userId, int from, int size);

    // Public API (публичный доступ)
    // Получить опубликованные комментарии события (публичный доступ)
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    // Admin API (для администраторов)
    // Админ получает комментарии на модерации
    List<CommentDto> getCommentsForModeration(int from, int size);

    // Админ модерирует комментарий
    CommentDto moderateComment(Long commentId, Boolean approve);
}