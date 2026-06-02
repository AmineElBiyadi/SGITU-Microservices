package ma.sgitu.g5;

import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.entity.NotificationType;
import ma.sgitu.g5.repository.NotificationRepository;
import ma.sgitu.g5.repository.specification.NotificationSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class G5ApplicationTests {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void testNotificationSpecification() {
        notificationRepository.deleteAll();

        Notification n1 = Notification.builder()
                .notificationId("notif-1")
                .userId("user-123")
                .type(NotificationType.EMAIL)
                .channel("EMAIL")
                .status(NotificationStatus.SENT)
                .sourceService("AUTH")
                .createdAt(LocalDateTime.now())
                .build();

        Notification n2 = Notification.builder()
                .notificationId("notif-2")
                .userId("user-456")
                .type(NotificationType.SMS)
                .channel("SMS")
                .status(NotificationStatus.FAILED)
                .sourceService("PAYMENT")
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(n1);
        notificationRepository.save(n2);

        // Test filter by userId
        Specification<Notification> spec1 = NotificationSpecification.withFilters(
                "user-123", null, null, null, null, null);
        List<Notification> list1 = notificationRepository.findAll(spec1);
        assertEquals(1, list1.size());
        assertEquals("notif-1", list1.get(0).getNotificationId());

        // Test filter by status
        Specification<Notification> spec2 = NotificationSpecification.withFilters(
                null, NotificationStatus.FAILED, null, null, null, null);
        List<Notification> list2 = notificationRepository.findAll(spec2);
        assertEquals(1, list2.size());
        assertEquals("notif-2", list2.get(0).getNotificationId());

        // Test filter by sourceService
        Specification<Notification> spec3 = NotificationSpecification.withFilters(
                null, null, "PAYMENT", null, null, null);
        List<Notification> list3 = notificationRepository.findAll(spec3);
        assertEquals(1, list3.size());
        assertEquals("notif-2", list3.get(0).getNotificationId());

        // Test combined filter
        Specification<Notification> spec4 = NotificationSpecification.withFilters(
                "user-123", NotificationStatus.SENT, "AUTH", null, null, null);
        List<Notification> list4 = notificationRepository.findAll(spec4);
        assertEquals(1, list4.size());

        // Test no match
        Specification<Notification> spec5 = NotificationSpecification.withFilters(
                "user-123", NotificationStatus.FAILED, null, null, null, null);
        List<Notification> list5 = notificationRepository.findAll(spec5);
        assertEquals(0, list5.size());
    }

    @Test
    void testControllerFilter() throws Exception {
        notificationRepository.deleteAll();

        Notification n1 = Notification.builder()
                .notificationId("notif-1")
                .userId("user-123")
                .type(NotificationType.EMAIL)
                .channel("EMAIL")
                .status(NotificationStatus.SENT)
                .sourceService("AUTH")
                .createdAt(LocalDateTime.now())
                .build();

        Notification n2 = Notification.builder()
                .notificationId("notif-2")
                .userId("user-456")
                .type(NotificationType.SMS)
                .channel("SMS")
                .status(NotificationStatus.FAILED)
                .sourceService("PAYMENT")
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(n1);
        notificationRepository.save(n2);

        // Test controller endpoint with userId filter (auth via headers Gateway G10)
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "admin-test")
                        .header("X-Roles", "ROLE_USER")
                        .param("userId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notificationId").value("notif-1"));

        // Test controller endpoint with status filter
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "admin-test")
                        .header("X-Roles", "ROLE_USER")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notificationId").value("notif-2"));

        // Test controller endpoint with sourceService filter
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "admin-test")
                        .header("X-Roles", "ROLE_USER")
                        .param("sourceService", "PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notificationId").value("notif-2"));

        // Test controller endpoint with no filters
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "admin-test")
                        .header("X-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        // Test controller endpoint with invalid status (expect 400 Bad Request)
        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "admin-test")
                        .header("X-Roles", "ROLE_USER")
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest());
    }
}


