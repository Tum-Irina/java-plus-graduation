package ru.practicum.comment;

import lombok.*;
import ru.practicum.user.UserShortDto;

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