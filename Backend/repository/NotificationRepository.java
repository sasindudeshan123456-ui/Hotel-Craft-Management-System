package com.hotelcraft.repository;

import com.hotelcraft.model.Notification;
import com.hotelcraft.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByTimestampDesc();
}
