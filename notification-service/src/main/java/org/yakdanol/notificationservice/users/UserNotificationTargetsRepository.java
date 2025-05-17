package org.yakdanol.notificationservice.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationTargetsRepository extends JpaRepository<UserNotificationTargets, Long> {

    /** Маршрут для конкретного internalUserName. */
    Optional<UserNotificationTargets> findByMonitoredInternalUserName(String internalUserName);
}

