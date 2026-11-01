package com.befit.backend.controller;

import com.befit.backend.dto.NotificationDTO;
import com.befit.backend.security.BranchAccess;
import com.befit.backend.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The logged-in user's own notifications (any role). The user is always taken from the authenticated token - there is no
 * user id in any URL or body - so nobody can read or change someone else's notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final BranchAccess access;

    public NotificationController(NotificationService notificationService, BranchAccess access) {
        this.notificationService = notificationService;
        this.access = access;
    }

    /** Newest first. ?unreadOnly=true  &limit=30 (1-100). */
    @GetMapping
    public List<NotificationDTO> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                      @RequestParam(required = false) Integer limit) {
        return notificationService.list(access.current(), unreadOnly, limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.unreadCount(access.current()));
    }

    /** 404 when the notification does not exist or belongs to another user. */
    @PostMapping("/{id}/read")
    public NotificationDTO markRead(@PathVariable Long id) {
        return notificationService.markRead(access.current(), id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead(access.current()));
    }
}
