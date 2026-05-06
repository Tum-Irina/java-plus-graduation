package ru.practicum.core.dto.comment;

import lombok.*;
import ru.practicum.core.dto.user.UserShortDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private String text;
    private UserShortDto author;
    private Long eventId;
    private String created;
    private String status;
}