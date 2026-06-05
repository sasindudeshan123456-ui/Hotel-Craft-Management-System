package com.hotelcraft.service;

import com.hotelcraft.model.Notification;
import com.hotelcraft.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByTimestampDesc();
    }

    public Notification createNotification(String title, String message, String type) {
        Notification notification = new Notification(title, message, type);
        return notificationRepository.save(notification);
    }

    public long getNotificationCount() {
        return notificationRepository.count();
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
