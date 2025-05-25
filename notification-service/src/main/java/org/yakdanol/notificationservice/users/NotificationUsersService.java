package org.yakdanol.notificationservice.users;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationUsersService {

    private final NotificationUsersRepository userRepository;

    /**
     * Возвращает всех пользователей.
     */
    @Transactional(readOnly = true)
    public List<NotificationUsers> findAll() {
        log.debug("Loading all NotificationUsers");
        return userRepository.findAll();
    }

    /**
     * Ищет пользователя по ID.
     * @throws EntityNotFoundException, если не найден.
     */
    @Transactional(readOnly = true)
    public NotificationUsers findById(Long id) {
        log.debug("Looking for NotificationUsers with id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NotificationUsers not found with id=" + id));
    }

    /**
     * Создаёт нового пользователя.
     */
    @Transactional
    public NotificationUsers create(NotificationUsers user) {
        log.info("Creating NotificationUsers for '{}'", user.getFullName());
        return userRepository.save(user);
    }

    /**
     * Обновляет существующего пользователя.
     * @throws EntityNotFoundException, если не найден.
     */
    @Transactional
    public NotificationUsers update(Long id, NotificationUsers updated) {
        NotificationUsers existing = findById(id);
        log.info("Updating NotificationUsers id={}", id);

        existing.setFullName(updated.getFullName());
        existing.setInternalUserName(updated.getInternalUserName());
        existing.setPosition(updated.getPosition());
        existing.setPhoneNumber(updated.getPhoneNumber());
        existing.setEmail(updated.getEmail());
        existing.setTelegramAccount(updated.getTelegramAccount());

        return userRepository.save(existing);
    }

    /**
     * Удаляет пользователя по ID.
     * @throws EntityNotFoundException, если не найден.
     */
    @Transactional
    public void delete(Long id) {
        NotificationUsers existing = findById(id);
        log.info("Deleting NotificationUsers id={}", id);
        userRepository.delete(existing);
    }
}
