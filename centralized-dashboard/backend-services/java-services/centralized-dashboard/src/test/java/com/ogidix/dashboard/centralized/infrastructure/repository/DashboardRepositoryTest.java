package com.gogidix.dashboard.centralized.infrastructure.repository;

import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DashboardRepository.
 * Tests all custom query methods and JPA repository functionality.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Dashboard Repository Tests")
class DashboardRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DashboardRepository dashboardRepository;

    private Dashboard testDashboard;
    private UUID testOwnerId;

    @BeforeEach
    void setUp() {
        testOwnerId = UUID.randomUUID();
        testDashboard = Dashboard.create("Test Dashboard", testOwnerId, "Test User");
        testDashboard.setDescription("Test Description");
        testDashboard.setCategory("Analytics");
        testDashboard.setTags("test,dashboard");
        entityManager.persistAndFlush(testDashboard);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save and find dashboard")
        void shouldSaveAndFindDashboard() {
            // Given
            Dashboard newDashboard = Dashboard.create("New Dashboard", UUID.randomUUID(), "Owner");

            // When
            Dashboard saved = dashboardRepository.save(newDashboard);

            // Then
            Optional<Dashboard> found = dashboardRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("New Dashboard");
        }

        @Test
        @DisplayName("Should update dashboard")
        void shouldUpdateDashboard() {
            // When
            testDashboard.setName("Updated Dashboard");
            Dashboard updated = dashboardRepository.save(testDashboard);

            // Then
            assertThat(updated.getName()).isEqualTo("Updated Dashboard");
            assertThat(updated.getUpdatedAt()).isAfter(testDashboard.getCreatedAt());
        }

        @Test
        @DisplayName("Should delete dashboard")
        void shouldDeleteDashboard() {
            // When
            dashboardRepository.delete(testDashboard);

            // Then
            Optional<Dashboard> found = dashboardRepository.findById(testDashboard.getId());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryTests {

        @Test
        @DisplayName("Should find dashboard by name")
        void shouldFindDashboardByName() {
            // When
            Optional<Dashboard> found = dashboardRepository.findByName("Test Dashboard");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(testDashboard.getId());
        }

        @Test
        @DisplayName("Should find dashboards by owner")
        void shouldFindDashboardsByOwner() {
            // Given
            Dashboard anotherDashboard = Dashboard.create("Another Dashboard", testOwnerId, "Test User");
            entityManager.persistAndFlush(anotherDashboard);

            // When
            List<Dashboard> dashboards = dashboardRepository.findByOwnerId(testOwnerId);

            // Then
            assertThat(dashboards).hasSize(2);
            assertThat(dashboards).extracting(Dashboard::getName)
                    .containsExactlyInAnyOrder("Test Dashboard", "Another Dashboard");
        }

        @Test
        @DisplayName("Should find public dashboards")
        void shouldFindPublicDashboards() {
            // Given
            testDashboard.makePublic();
            entityManager.persistAndFlush(testDashboard);

            // When
            List<Dashboard> publicDashboards = dashboardRepository.findByIsPublicTrue();

            // Then
            assertThat(publicDashboards).hasSize(1);
            assertThat(publicDashboards.get(0).getIsPublic()).isTrue();
        }

        @Test
        @DisplayName("Should find active dashboards")
        void shouldFindActiveDashboards() {
            // When
            List<Dashboard> activeDashboards = dashboardRepository.findByIsActiveTrue();

            // Then
            assertThat(activeDashboards).hasSize(1);
            assertThat(activeDashboards.get(0).isActive()).isTrue();
        }

        @Test
        @DisplayName("Should find accessible dashboards")
        void shouldFindAccessibleDashboards() {
            // Given
            testDashboard.makePublic();
            Dashboard privateDashboard = Dashboard.create("Private Dashboard", testOwnerId, "Test User");
            entityManager.persistAndFlush(privateDashboard);

            // When
            List<Dashboard> accessible = dashboardRepository.findAccessibleDashboards(testOwnerId);

            // Then
            assertThat(accessible).hasSize(2);
        }

        @Test
        @DisplayName("Should count dashboards by owner")
        void shouldCountDashboardsByOwner() {
            // Given
            Dashboard anotherDashboard = Dashboard.create("Another Dashboard", testOwnerId, "Test User");
            entityManager.persistAndFlush(anotherDashboard);

            // When
            Long count = dashboardRepository.countByOwnerId(testOwnerId);

            // Then
            assertThat(count).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should count public dashboards")
        void shouldCountPublicDashboards() {
            // Given
            testDashboard.makePublic();
            entityManager.persistAndFlush(testDashboard);

            // When
            Long count = dashboardRepository.countByIsPublicTrue();

            // Then
            assertThat(count).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Pagination Support")
    class PaginationTests {

        @Test
        @DisplayName("Should find dashboards with pagination")
        void shouldFindDashboardsWithPagination() {
            // Given
            for (int i = 0; i < 5; i++) {
                Dashboard dashboard = Dashboard.create("Dashboard " + i, testOwnerId, "User " + i);
                entityManager.persistAndFlush(dashboard);
            }

            Pageable pageable = PageRequest.of(0, 3);

            // When
            Page<Dashboard> page = dashboardRepository.findAll(pageable);

            // Then
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isGreaterThan(5);
            assertThat(page.getTotalPages()).isGreaterThan(1);
        }

        @Test
        @DisplayName("Should find dashboards by owner and active status with pagination")
        void shouldFindDashboardsByOwnerAndActiveStatusWithPagination() {
            // Given
            for (int i = 0; i < 3; i++) {
                Dashboard dashboard = Dashboard.create("Dashboard " + i, testOwnerId, "User " + i);
                if (i == 1) dashboard.deactivate();
                entityManager.persistAndFlush(dashboard);
            }

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Dashboard> page = dashboardRepository.findByOwnerIdAndIsActive(testOwnerId, true, pageable);

            // Then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).allMatch(Dashboard::isActive);
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should find public active dashboards with pagination")
        void shouldFindPublicActiveDashboardsWithPagination() {
            // Given
            testDashboard.makePublic();
            entityManager.persistAndFlush(testDashboard);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Dashboard> page = dashboardRepository.findByIsPublicTrueAndIsActiveTrue(pageable);

            // Then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getIsPublic()).isTrue();
            assertThat(page.getContent().get(0).isActive()).isTrue();
        }

        @Test
        @DisplayName("Should search dashboards with pagination")
        void shouldSearchDashboardsWithPagination() {
            // Given
            Dashboard searchableDashboard = Dashboard.create("Analytics Dashboard", testOwnerId, "User");
            searchableDashboard.setDescription("Analytics and reporting dashboard");
            entityManager.persistAndFlush(searchableDashboard);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Dashboard> page = dashboardRepository.searchDashboards("Analytics", pageable);

            // Then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getName()).contains("Analytics");
        }
    }

    @Nested
    @DisplayName("Advanced Query Methods")
    class AdvancedQueryTests {

        @Test
        @DisplayName("Should find dashboards by category")
        void shouldFindDashboardsByCategory() {
            // Given
            Dashboard analyticsDashboard = Dashboard.create("Analytics", testOwnerId, "User");
            analyticsDashboard.setCategory("Analytics");
            entityManager.persistAndFlush(analyticsDashboard);

            // When
            List<Dashboard> dashboards = dashboardRepository.findByCategory("Analytics");

            // Then
            assertThat(dashboards).hasSize(1);
            assertThat(dashboards.get(0).getCategory()).isEqualTo("Analytics");
        }

        @Test
        @DisplayName("Should find recently accessed dashboards")
        void shouldFindRecentlyAccessedDashboards() {
            // Given
            Dashboard recentDashboard = Dashboard.create("Recent", testOwnerId, "User");
            recentDashboard.setLastAccessedAt(LocalDateTime.now().minusHours(1));
            entityManager.persistAndFlush(recentDashboard);

            // When
            List<Dashboard> recent = dashboardRepository.findRecentlyAccessedDashboards(testOwnerId, 5);

            // Then
            assertThat(recent).hasSize(2); // Including testDashboard
        }

        @Test
        @DisplayName("Should find most viewed dashboards")
        void shouldFindMostViewedDashboards() {
            // Given
            testDashboard.makePublic();
            testDashboard.recordView();
            testDashboard.recordView();
            testDashboard.recordView();
            entityManager.persistAndFlush(testDashboard);

            // When
            List<Dashboard> popular = dashboardRepository.findMostViewedDashboards(5);

            // Then
            assertThat(popular).hasSize(1);
            assertThat(popular.get(0).getViewCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should count active dashboards by owner")
        void shouldCountActiveDashboardsByOwner() {
            // Given
            Dashboard inactiveDashboard = Dashboard.create("Inactive", testOwnerId, "User");
            inactiveDashboard.deactivate();
            entityManager.persistAndFlush(inactiveDashboard);

            // When
            Long count = dashboardRepository.countActiveByOwnerId(testOwnerId);

            // Then
            assertThat(count).isEqualTo(1L); // Only testDashboard is active
        }

        @Test
        @DisplayName("Should find popular dashboards with pagination")
        void shouldFindPopularDashboardsWithPagination() {
            // Given
            testDashboard.makePublic();
            testDashboard.recordView();
            entityManager.persistAndFlush(testDashboard);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Dashboard> page = dashboardRepository.findPopularDashboards(pageable);

            // Then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getViewCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Date-based Queries")
    class DateBasedQueries {

        @Test
        @DisplayName("Should find dashboards created after date")
        void shouldFindDashboardsCreatedAfterDate() {
            // Given
            LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
            Dashboard oldDashboard = Dashboard.create("Old", testOwnerId, "User");
            oldDashboard.setCreatedAt(LocalDateTime.now().minusDays(2));
            entityManager.persistAndFlush(oldDashboard);

            // When
            List<Dashboard> recent = dashboardRepository.findByCreatedAtAfter(cutoff);

            // Then
            assertThat(recent).hasSize(1);
            assertThat(recent.get(0).getName()).isEqualTo("Test Dashboard");
        }

        @Test
        @DisplayName("Should find dashboards accessed after date")
        void shouldFindDashboardsAccessedAfterDate() {
            // Given
            LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
            testDashboard.setLastAccessedAt(LocalDateTime.now().minusMinutes(30));
            entityManager.persistAndFlush(testDashboard);

            // When
            List<Dashboard> recentlyAccessed = dashboardRepository.findByLastAccessedAtAfter(cutoff);

            // Then
            assertThat(recentlyAccessed).hasSize(1);
        }
    }
}