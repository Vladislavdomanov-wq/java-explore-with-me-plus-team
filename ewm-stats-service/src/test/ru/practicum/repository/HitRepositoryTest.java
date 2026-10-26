package ru.practicum.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.model.Hit;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // ← ДОБАВЬ ЭТУ СТРОКУ
class HitRepositoryTest {

    @Autowired
    private HitRepository hitRepository;

    @Test
    void saveHit_shouldReturnSavedHitWithId() {
        Hit hit = new Hit();
        hit.setApp("test-app");
        hit.setUri("/test");
        hit.setIp("127.0.0.1");
        hit.setTimestamp(LocalDateTime.now());

        Hit savedHit = hitRepository.save(hit);

        assertNotNull(savedHit.getId());
        assertEquals("test-app", savedHit.getApp());
    }
}