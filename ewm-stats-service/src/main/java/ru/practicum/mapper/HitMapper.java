package ru.practicum.mapper;

import ru.practicum.dto.EndpointHit;
import ru.practicum.model.Hit;

public final class HitMapper {

    private HitMapper() {
    }

    public static Hit toEntity(EndpointHit dto) {
        if (dto == null) {
            return null;
        }
        Hit hit = new Hit();
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setTimestamp(dto.getTimestamp());
        return hit;
    }

    public static EndpointHit toDto(Hit hit) {
        if (hit == null) {
            return null;
        }
        EndpointHit dto = new EndpointHit();
        dto.setId(hit.getId());
        dto.setApp(hit.getApp());
        dto.setUri(hit.getUri());
        dto.setIp(hit.getIp());
        dto.setTimestamp(hit.getTimestamp());
        return dto;
    }
}