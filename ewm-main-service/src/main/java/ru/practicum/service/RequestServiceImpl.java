package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatus;
import ru.practicum.model.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор не может подать заявку на свое событие");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя подать заявку на неопубликованное событие");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Заявка уже существует");
        }

        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (limit > 0 && confirmed >= limit) {
            throw new ConflictException("Достигнут лимит участников");
        }

        RequestStatus status = (!Boolean.TRUE.equals(event.getRequestModeration()) || limit == 0)
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(user.getId())
                .status(status)
                .build();

        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (request.getStatus() == RequestStatus.CANCELED) {
            throw new ConflictException("Заявка уже отменена");
        }

        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest update) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        RequestStatus targetStatus;
        try {
            targetStatus = RequestStatus.valueOf(update.getStatus());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ConflictException("Статус может быть только CONFIRMED или REJECTED");
        }
        if (targetStatus != RequestStatus.CONFIRMED && targetStatus != RequestStatus.REJECTED) {
            throw new ConflictException("Статус может быть только CONFIRMED или REJECTED");
        }

        List<Request> requests = requestRepository.findAllByIdInAndEventId(update.getRequestIds(), eventId);
        if (requests.size() != update.getRequestIds().size()) {
            throw new NotFoundException("Одна или несколько заявок не найдены для события с id=" + eventId);
        }
        if (requests.stream().anyMatch(request -> request.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Изменить статус можно только у заявок в статусе PENDING");
        }

        if (targetStatus == RequestStatus.REJECTED) {
            requests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
            List<Request> rejected = requestRepository.saveAll(requests);
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(rejected.stream().map(RequestMapper::toDto).toList())
                    .build();
        }

        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (limit > 0 && limit - confirmed < requests.size()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        requests.forEach(request -> request.setStatus(RequestStatus.CONFIRMED));
        List<Request> confirmedRequests = requestRepository.saveAll(requests);

        List<Request> rejectedRequests = new ArrayList<>();
        if (limit > 0 && confirmed + confirmedRequests.size() >= limit) {
            rejectedRequests = requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
            rejectedRequests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(rejectedRequests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream().map(RequestMapper::toDto).toList())
                .rejectedRequests(rejectedRequests.stream().map(RequestMapper::toDto).toList())
                .build();
    }
}
