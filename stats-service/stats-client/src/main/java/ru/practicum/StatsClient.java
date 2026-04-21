package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class StatsClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String serverUrl;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);
    private static final ParameterizedTypeReference<List<ViewStatsDto>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    public StatsClient(@Value("${stats.service.url}") String serverUrl) {
        this.serverUrl = serverUrl;
        log.info("StatsClient инициализирован с URL: {}", serverUrl);
    }

    // Сохранение информации о том, что к эндпоинту был запрос
    public void hit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(FORMATTER.format(timestamp))
                .build();
        try {
            restTemplate.postForEntity(serverUrl + "/hit", hitDto, Void.class);
            log.debug("Информация сохранена: app={}, uri={}, ip={}", app, uri, ip);
        } catch (Exception e) {
            log.error("Ошибка сохранения информации: {}", e.getMessage());
        }
    }

    // Получение статистики по посещениям
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        String url = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", start == null ? null : encode(start))
                .queryParam("end", end == null ? null : encode(end))
                .queryParamIfPresent("uris", Optional.ofNullable(uris)
                        .filter(list -> !list.isEmpty()))
                .queryParamIfPresent("unique", Optional.ofNullable(unique))
                .build(false)
                .toUriString();

        try {
            log.debug("Получена статистика для {}", url);
            return restTemplate.exchange(url, HttpMethod.GET, null, LIST_TYPE)
                    .getBody();
        } catch (Exception e) {
            log.warn("Не удалось собрать статистику: {}", e.getMessage());
            return List.of();
        }
    }

    // Кодировка даты и времени
    private String encode(LocalDateTime dateTime) {
        return URLEncoder.encode(FORMATTER.format(dateTime), StandardCharsets.UTF_8);
    }
}