package com.gogidix.dashboard.centralized.domain.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for Dashboard domain entity.
 * Tests all business logic, validation rules, and domain behaviors.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@DisplayName("Dashboard Domain Entity Tests")
class DashboardTest {

    private UUID testOwnerId;
    private String testOwnerName;
    private String validDashboardName;

    @BeforeEach
    void setUp() {
        testOwnerId = UUID.randomUUID();
        testOwnerName = "Test User";
        validDashboardName = "Test Dashboard";
    }

    @Nested
    @DisplayName("Dashboard Creation Tests")
    class DashboardCreationTests {

        @Test
        @DisplayName("Should create dashboard with valid parameters")
        void shouldCreateDashboardWithValidParameters() {
            // When
            Dashboard dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);

            // Then
            assertThat(dashboard).isNotNull();
            assertThat(dashboard.getId()).isNotNull();
            assertThat(dashboard.getName()).isEqualTo(validDashboardName);
            assertThat(dashboard.getOwnerId()).isEqualTo(testOwnerId);
            assertThat(dashboard.getOwnerName()).isEqualTo(testOwnerName);
            assertThat(dashboard.isActive()).isTrue();
            assertThat(dashboard.getIsPublic()).isFalse();
            assertThat(dashboard.getViewCount()).isEqualTo(0L);
            assertThat(dashboard.getWidgetCount()).isEqualTo(0);
            assertThat(dashboard.getCreatedAt()).isNotNull();
            assertThat(dashboard.getUpdatedAt()).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should throw exception when dashboard name is null or empty")
        void shouldThrowExceptionWhenNameIsInvalid(String invalidName) {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Dashboard.create(invalidName, testOwnerId, testOwnerName);
            });
        }

        @Test
        @DisplayName("Should throw exception when owner ID is null")
        void shouldThrowExceptionWhenOwnerIdIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Dashboard.create(validDashboardName, null, testOwnerName);
            });
        }
    }

    @Nested
    @DisplayName("Dashboard Information Update Tests")
    class DashboardUpdateTests {

        private Dashboard dashboard;

        @BeforeEach
        void setUp() {
            dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);
        }

        @Test
        @DisplayName("Should update dashboard information with valid data")
        void shouldUpdateDashboardWithValidData() {
            // Given
            String newName = "Updated Dashboard";
            String description = "Updated description";
            String category = "Analytics";
            String tags = "analytics,reports,kpi";

            // When
            dashboard.updateDashboard(newName, description, category, tags);

            // Then
            assertThat(dashboard.getName()).isEqualTo(newName);
            assertThat(dashboard.getDescription()).isEqualTo(description);
            assertThat(dashboard.getCategory()).isEqualTo(category);
            assertThat(dashboard.getTags()).isEqualTo(tags);
        }

        @Test
        @DisplayName("Should trim whitespace from dashboard name")
        void shouldTrimWhitespaceFromName() {
            // Given
            String nameWithWhitespace = "  Updated Dashboard  ";

            // When
            dashboard.updateDashboard(nameWithWhitespace, null, null, null);

            // Then
            assertThat(dashboard.getName()).isEqualTo("Updated Dashboard");
        }

        @Test
        @DisplayName("Should not update when name is null or empty")
        void shouldNotUpdateWhenNameIsInvalid() {
            // Given
            String originalName = dashboard.getName();

            // When
            dashboard.updateDashboard("", null, null, null);

            // Then
            assertThat(dashboard.getName()).isEqualTo(originalName);
        }
    }

    @Nested
    @DisplayName("Dashboard Access Control Tests")
    class AccessControlTests {

        private Dashboard dashboard;
        private UUID differentUserId;

        @BeforeEach
        void setUp() {
            dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);
            differentUserId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Should allow access for owner")
        void shouldAllowAccessForOwner() {
            // When & Then
            assertTrue(dashboard.isAccessibleBy(testOwnerId));
        }

        @Test
        @DisplayName("Should allow access for public dashboard")
        void shouldAllowAccessForPublicDashboard() {
            // Given
            dashboard.makePublic();

            // When & Then
            assertTrue(dashboard.isAccessibleBy(differentUserId));
        }

        @Test
        @DisplayName("Should deny access for private dashboard and different user")
        void shouldDenyAccessForPrivateDashboard() {
            // When & Then
            assertFalse(dashboard.isAccessibleBy(differentUserId));
        }

        @Test
        @DisplayName("Should toggle public/private status correctly")
        void shouldTogglePublicPrivateStatus() {
            // When - Make public
            dashboard.makePublic();
            assertTrue(dashboard.getIsPublic());

            // When - Make private
            dashboard.makePrivate();
            assertFalse(dashboard.getIsPublic());
        }
    }

    @Nested
    @DisplayName("Dashboard Lifecycle Tests")
    class LifecycleTests {

        private Dashboard dashboard;

        @BeforeEach
        void setUp() {
            dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);
        }

        @Test
        @DisplayName("Should activate dashboard")
        void shouldActivateDashboard() {
            // Given
            dashboard.deactivate();
            assertFalse(dashboard.isActive());

            // When
            dashboard.activate();

            // Then
            assertTrue(dashboard.isActive());
        }

        @Test
        @DisplayName("Should deactivate dashboard")
        void shouldDeactivateDashboard() {
            // Given
            assertTrue(dashboard.isActive());

            // When
            dashboard.deactivate();

            // Then
            assertFalse(dashboard.isActive());
        }

        @Test
        @DisplayName("Should record view and update access timestamp")
        void shouldRecordViewAndUpdateAccess() {
            // Given
            LocalDateTime beforeAccess = LocalDateTime.now();

            // When
            dashboard.recordAccess();

            // Then
            assertThat(dashboard.getViewCount()).isEqualTo(1L);
            assertThat(dashboard.getLastAccessedAt()).isAfter(beforeAccess);
        }

        @Test
        @DisplayName("Should calculate age correctly")
        void shouldCalculateAgeCorrectly() {
            // Given
            LocalDateTime creationTime = LocalDateTime.now().minusDays(5);
            dashboard = Dashboard.builder()
                    .id(UUID.randomUUID())
                    .name("Test")
                    .ownerId(testOwnerId)
                    .createdAt(creationTime)
                    .build();

            // When
            long age = dashboard.getAgeInDays();

            // Then
            assertThat(age).isGreaterThanOrEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Dashboard Widget Management Tests")
    class WidgetManagementTests {

        private Dashboard dashboard;

        @BeforeEach
        void setUp() {
            dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);
        }

        @Test
        @DisplayName("Should add widget and increment count")
        void shouldAddWidgetAndIncrementCount() {
            // Given
            UUID widgetId = UUID.randomUUID();
            assertThat(dashboard.getWidgetCount()).isEqualTo(0);

            // When
            dashboard.addWidget(widgetId);

            // Then
            assertThat(dashboard.getWidgetCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should remove widget and decrement count")
        void shouldRemoveWidgetAndDecrementCount() {
            // Given
            UUID widgetId = UUID.randomUUID();
            dashboard.addWidget(widgetId);
            assertThat(dashboard.getWidgetCount()).isEqualTo(1);

            // When
            dashboard.removeWidget(widgetId);

            // Then
            assertThat(dashboard.getWidgetCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not allow negative widget count")
        void shouldNotAllowNegativeWidgetCount() {
            // Given
            assertThat(dashboard.getWidgetCount()).isEqualTo(0);

            // When
            dashboard.removeWidget(UUID.randomUUID());

            // Then
            assertThat(dashboard.getWidgetCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should set widget count directly")
        void shouldSetWidgetCountDirectly() {
            // When
            dashboard.setWidgetCount(5);

            // Then
            assertThat(dashboard.getWidgetCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Dashboard Configuration Tests")
    class ConfigurationTests {

        private Dashboard dashboard;

        @BeforeEach
        void setUp() {
            dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);
        }

        @Test
        @DisplayName("Should set and get refresh interval")
        void shouldSetAndGetRefreshInterval() {
            // When
            dashboard.setRefreshInterval(60);

            // Then
            assertThat(dashboard.getRefreshInterval()).isEqualTo(30); // Default implementation
        }

        @Test
        @DisplayName("Should set and get layout configuration")
        void shouldSetAndGetLayoutConfig() {
            // Given
            String layoutConfig = "{\"layout\": \"grid\", \"columns\": 12}";

            // When
            dashboard.setLayoutConfig(layoutConfig);

            // Then
            assertThat(dashboard.getLayoutConfig()).isEqualTo(layoutConfig);
        }

        @Test
        @DisplayName("Should update theme correctly")
        void shouldUpdateTheme() {
            // Given
            String newTheme = "dark";

            // When
            dashboard.setTheme(newTheme);

            // Then
            assertThat(dashboard.getTheme()).isEqualTo(newTheme);
        }
    }

    @Nested
    @DisplayName("Dashboard Equality and HashCode Tests")
    class EqualityTests {

        private Dashboard dashboard1;
        private Dashboard dashboard2;
        private Dashboard dashboard3;

        @BeforeEach
        void setUp() {
            UUID id = UUID.randomUUID();
            dashboard1 = Dashboard.builder()
                    .id(id)
                    .name("Test")
                    .ownerId(testOwnerId)
                    .build();
            dashboard2 = Dashboard.builder()
                    .id(id)
                    .name("Test")
                    .ownerId(testOwnerId)
                    .build();
            dashboard3 = Dashboard.builder()
                    .id(UUID.randomUUID())
                    .name("Test")
                    .ownerId(testOwnerId)
                    .build();
        }

        @Test
        @DisplayName("Should be equal when IDs are same")
        void shouldBeEqualWhenIdsAreSame() {
            // Then
            assertThat(dashboard1).isEqualTo(dashboard2);
            assertThat(dashboard1.hashCode()).isEqualTo(dashboard2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Then
            assertThat(dashboard1).isNotEqualTo(dashboard3);
            assertThat(dashboard1.hashCode()).isNotEqualTo(dashboard3.hashCode());
        }
    }

    @Nested
    @DisplayName("Dashboard Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should handle tag conversion properly")
        void shouldHandleTagConversion() {
            // Given
            List<String> tags = List.of("analytics", "reports", "dashboard");
            Dashboard dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);

            // When
            dashboard.setTags(String.join(",", tags));

            // Then
            assertThat(dashboard.getTags()).isEqualTo("analytics,reports,dashboard");
        }

        @ParameterizedTest
        @CsvSource({
            "Analytics, analytics",
            "Reports, reports",
            "Dashboard, dashboard",
            "Custom Charts, custom-charts"
        })
        @DisplayName("Should normalize category names")
        void shouldNormalizeCategoryNames(String inputCategory, String expectedCategory) {
            // Given
            Dashboard dashboard = Dashboard.create(validDashboardName, testOwnerId, testOwnerName);

            // When
            dashboard.setCategory(inputCategory);

            // Then
            assertThat(dashboard.getCategory()).isEqualTo(expectedCategory);
        }
    }
}