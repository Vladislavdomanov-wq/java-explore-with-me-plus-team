package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        validateEmail(request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email=" + request.getEmail() + " already exists");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        int offset = from == null ? 0 : from;
        int limit = size == null ? 10 : size;
        List<User> users = (ids == null || ids.isEmpty())
                ? userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                : userRepository.findAllByIdIn(ids).stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();

        int start = Math.min(offset, users.size());
        int end = Math.min(start + limit, users.size());
        return users.subList(start, end).stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
    }

    private void validateEmail(String email) {
        if (email == null) {
            throw new ValidationException("Email must not be null");
        }
        if (email.length() > 254) {
            throw new ValidationException("Email length must not exceed 254 characters");
        }
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            throw new ValidationException("Email is not valid");
        }
        String localPart = email.substring(0, at);
        String domainPart = email.substring(at + 1);
        if (localPart.length() > 64) {
            throw new ValidationException("Email local part length must not exceed 64 characters");
        }
        for (String label : domainPart.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63) {
                throw new ValidationException("Email domain part length must not exceed 63 characters");
            }
        }
    }
}
