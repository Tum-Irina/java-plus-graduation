package ru.practicum;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EndpointHitController {
    private final EndpointHitService endpointHitService;

    public EndpointHitController(EndpointHitService endpointHitService) {
        this.endpointHitService = endpointHitService;
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getAll(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String[] uris,
            @RequestParam(defaultValue = "false") Boolean unique) {
        return endpointHitService.getStatistics(start, end, uris, unique);
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody EndpointHitDto endpointHitDto) {
        endpointHitService.create(endpointHitDto);
    }
}
