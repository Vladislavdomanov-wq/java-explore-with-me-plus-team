package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;
import ru.practicum.model.Hit;
import ru.practicum.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final HitRepository hitRepository;

    @Override
    public EndpointHit saveHit(EndpointHit hitDto) {
        Hit hit = new Hit();
        hit.setApp(hitDto.getApp());
        hit.setUri(hitDto.getUri());
        hit.setIp(hitDto.getIp());
        hit.setTimestamp(hitDto.getTimestamp());

        Hit savedHit = hitRepository.save(hit);

        hitDto.setId(savedHit.getId());

        return hitDto;
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        List<Object[]> results;
        if (Boolean.TRUE.equals(unique)) {
            results = hitRepository.getUniqueStats(start, end, uris);
        } else {
            results = hitRepository.getStats(start, end, uris);
        }

        List<ViewStats> stats = new ArrayList<>();
        for (Object[] row : results) {
            ViewStats dto = new ViewStats();
            dto.setApp((String) row[0]);
            dto.setUri((String) row[1]);
            dto.setHits((Long) row[2]);
            stats.add(dto);
        }
        return stats;
    }
}