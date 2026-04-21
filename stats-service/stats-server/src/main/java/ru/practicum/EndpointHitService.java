package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointHitService {
    private final EndpointHitRepository endpointHitRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public List<ViewStatsDto> getStatistics(String start, String end, String[] uris, Boolean unique) {
        log.info("Получение статистики по посещениям с параметрами start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        String decodedStart = URLDecoder.decode(start, StandardCharsets.UTF_8);
        String decodedEnd = URLDecoder.decode(end, StandardCharsets.UTF_8);
        Instant startTime = Instant.from(FORMATTER.parse(decodedStart));
        Instant endTime = Instant.from(FORMATTER.parse(decodedEnd));
        if (startTime.isAfter(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала диапазона не может быть позже даты окончания");
        }
        if (unique == true) {
            return endpointHitRepository.findStatisticsUniqueIp(startTime, endTime, uris);
        } else {
            return endpointHitRepository.findStatistics(startTime, endTime, uris);
        }
    }

    public void create(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = EndpointHitMapper.toEndpointHit(endpointHitDto);
        endpointHitRepository.save(endpointHit);
        log.info("Сохранение информации о запросе к эндпоинту endpointHitDto={}", endpointHitDto);
    }
}
