package com.gogidix.dashboard.centralized.domain.widget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for Widget domain entity.
 * Tests all business logic, validation rules, and widget behaviors.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@DisplayName("Widget Domain Entity Tests")
class WidgetTest {

    private UUID dashboardId;
    private String validWidgetName;
    private String validWidgetType;

    @BeforeEach
    void setUp() {
        dashboardId = UUID.randomUUID();
        validWidgetName = "Test Widget";
        validWidgetType = Widget.TYPE_CHART;
    }

    @Nested
    @DisplayName("Widget Creation Tests")
    class WidgetCreationTests {

        @Test
        @DisplayName("Should create widget with valid parameters")
        void shouldCreateWidgetWithValidParameters() {
            // When
            Widget widget = Widget.create(validWidgetName, dashboardId, validWidgetType);

            // Then
            assertThat(widget).isNotNull();
            assertThat(widget.getId()).isNotNull();
            assertThat(widget.getName()).isEqualTo(validWidgetName);
            assertThat(widget.getDashboardId()).isEqualTo(dashboardId);
            assertThat(widget.getWidgetType()).isEqualTo(validWidgetType);
            assertThat(widget.isActive()).isTrue();
            assertThat(widget.getIsVisible()).isTrue();
            assertThat(widget.getPositionX()).isEqualTo(0);
            assertThat(widget.getPositionY()).isEqualTo(0);
            assertThat(widget.getWidth()).isEqualTo(100);
            assertThat(widget.getHeight()).isEqualTo(100);
            assertThat(widget.getRefreshInterval()).isEqualTo(30);
            assertThat(widget.getCreatedAt()).isNotNull();
            assertThat(widget.getUpdatedAt()).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should throw exception when widget name is null or empty")
        void shouldThrowExceptionWhenNameIsInvalid(String invalidName) {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Widget.create(invalidName, dashboardId, validWidgetType);
            });
        }

        @Test
        @DisplayName("Should throw exception when dashboard ID is null")
        void shouldThrowExceptionWhenDashboardIdIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Widget.create(validWidgetName, null, validWidgetType);
            });
        }

        @Test
        @DisplayName("Should throw exception when widget type is null")
        void shouldThrowExceptionWhenWidgetTypeIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Widget.create(validWidgetName, dashboardId, null);
            });
        }
    }

    @Nested
    @DisplayName("Widget Type Validation Tests")
    class WidgetTypeValidationTests {

        private Widget widget;

        @BeforeEach
        void setUp() {
            widget = Widget.create(validWidgetName, dashboardId, Widget.TYPE_CHART);
        }

        @ParameterizedTest
        @EnumSource(names = {"TYPE_CHART", "TYPE_TABLE", "TYPE_METRIC", "TYPE_GAUGE",
                             "TYPE_HEATMAP", "TYPE_TEXT", "TYPE_IMAGE", "TYPE_LIST"})
        @DisplayName("Should validate all widget types")
        void shouldValidateAllWidgetTypes(String widgetTypeConstant) {
            // Given
            String widgetType = (String) Widget.class.getEnumConstants()[0].getClass()
                    .getDeclaredField(widgetTypeConstant).get(null);

            // When
            widget.setWidgetType(widgetType);

            // Then
            assertTrue(widget.isValidType());
        }

        @Test
        @DisplayName("Should reject invalid widget type")
        void shouldRejectInvalidWidgetType() {
            // Given
            Widget invalidWidget = Widget.builder()
                    .id(UUID.randomUUID())
                    .name("Test")
                    .dashboardId(dashboardId)
                    .widgetType("INVALID_TYPE")
                    .build();

            // Then
            assertFalse(invalidWidget.isValidType());
        }
    }

    @Nested
    @DisplayName("Widget Position Tests")
    class WidgetPositionTests {

        private Widget widget;

        @BeforeEach
        void setUp() {
            widget = Widget.create(validWidgetName, dashboardId, Widget.TYPE_CHART);
        }

        @Test
        @DisplayName("Should update position correctly")
        void shouldUpdatePositionCorrectly() {
            // Given
            int newX = 100;
            int newY = 200;

            // When
            widget.updatePosition(newX, newY);

            // Then
            assertThat(widget.getPositionX()).isEqualTo(newX);
            assertThat(widget.getPositionY()).isEqualTo(newY);
        }

        @Test
        @DisplayName("Should update size correctly")
        void shouldUpdateSizeCorrectly() {
            // Given
            int newWidth = 200;
            int newHeight = 300;

            // When
            widget.updateSize(newWidth, newHeight);

            // Then
            assertThat(widget.getWidth()).isEqualTo(newWidth);
            assertThat(widget.getHeight()).isEqualTo(newHeight);
        }

        @ParameterizedTest
        @CsvSource({
            "0, 0, 0, 0",
            "100, 200, 50, 75",
            "-10, -5, -1, -1"
        })
        @DisplayName("Should handle negative and zero coordinates")
        void shouldHandleCoordinates(int x, int y, int width, int height) {
            // When
            widget.updatePosition(x, y);
            widget.updateSize(width, height);

            // Then
            assertThat(widget.getPositionX()).isEqualTo(x);
            assertThat(widget.getPositionY()).isEqualTo(y);
            assertThat(widget.getWidth()).isEqualTo(width);
            assertThat(widget.getHeight()).isEqualTo(height);
        }
    }

    @Nested
    @DisplayName("Widget Lifecycle Tests")
    class WidgetLifecycleTests {

        private Widget widget;

        @BeforeEach
        void setUp() {
            widget = Widget.create(validWidgetName, dashboardId, Widget.TYPE_CHART);
        }

        @Test
        @DisplayName("Should activate widget")
        void shouldActivateWidget() {
            // Given
            widget.deactivate();
            assertFalse(widget.isActive());

            // When
            widget.activate();

            // Then
            assertTrue(widget.isActive());
        }

        @Test
        @DisplayName("Should deactivate widget")
        void shouldDeactivateWidget() {
            // Given
            assertTrue(widget.isActive());

            // When
            widget.deactivate();

            // Then
            assertFalse(widget.isActive());
        }

        @Test
        @DisplayName("Should toggle visibility correctly")
        void shouldToggleVisibility() {
            // Given
            assertTrue(widget.getIsVisible());

            // When
            widget.hide();

            // Then
            assertFalse(widget.getIsVisible());

            // When
            widget.show();

            // Then
            assertTrue(widget.getIsVisible());
        }

        @Test
        @DisplayName("Should delete widget (soft delete)")
        void shouldDeleteWidget() {
            // When
            widget.delete();

            // Then
            assertFalse(widget.isActive());
        }
    }

    @Nested
    @DisplayName("Widget Configuration Tests")
    class WidgetConfigurationTests {

        private Widget widget;

        @BeforeEach
        void setUp() {
            widget = Widget.create(validWidgetName, dashboardId, Widget.TYPE_CHART);
        }

        @Test
        @DisplayName("Should set and get configuration")
        void shouldSetAndGetConfiguration() {
            // Given
            String config = "{\"type\": \"line\", \"data\": {}}";

            // When
            widget.updateConfiguration(config);

            // Then
            assertThat(widget.getConfiguration()).isEqualTo(config);
        }

        @Test
        @DisplayName("Should set and get data source")
        void shouldSetAndGetDataSource() {
            // Given
            String dataSource = "database.table.analytics_data";

            // When
            widget.setDataSource(dataSource);

            // Then
            assertThat(widget.getDataSourceId()).isEqualTo(dataSource);
        }

        @Test
        @DisplayName("Should update refresh interval")
        void shouldUpdateRefreshInterval() {
            // Given
            int newInterval = 60;

            // When
            widget.setRefreshInterval(newInterval);

            // Then
            assertThat(widget.getRefreshInterval()).isEqualTo(newInterval);
        }

        @Test
        @DisplayName("Should update all parameters at once")
        void shouldUpdateAllParameters() {
            // When
            widget.updatePosition(100, 200, 150, 120, 1);

            // Then
            assertThat(widget.getPositionX()).isEqualTo(100);
            assertThat(widget.getPositionY()).isEqualTo(200);
            assertThat(widget.getWidth()).isEqualTo(150);
            assertThat(widget.getHeight()).isEqualTo(120);
        }
    }

    @Nested
    @DisplayName("Widget Refresh Logic Tests")
    class WidgetRefreshTests {

        private Widget widget;

        @BeforeEach
        void setUp() {
            widget = Widget.create(validWidgetName, dashboardId, Widget.TYPE_CHART);
        }

        @Test
        @DisplayName("Should check if widget needs refresh when never refreshed")
        void shouldCheckRefreshWhenNeverRefreshed() {
            // When & Then
            assertTrue(widget.needsRefresh());
        }

        @Test
        @DisplayName("Should need refresh when interval is exceeded")
        void shouldNeedRefreshWhenIntervalExceeded() {
            // Given
            widget.setRefreshInterval(1); // 1 second
            widget.markAsRefreshed(); // Mark as just refreshed

            // When
            try {
                Thread.sleep(1100); // Wait longer than refresh interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then
            assertTrue(widget.needsRefresh());
        }

        @Test
        @DisplayName("Should not need refresh when interval is not exceeded")
        void shouldNotNeedRefreshWhenIntervalNotExceeded() {
            // Given
            widget.setRefreshInterval(3600); // 1 hour
            widget.markAsRefreshed();

            // When & Then
            assertFalse(widget.needsRefresh());
        }

        @Test
        @DisplayName("Should mark as refreshed correctly")
        void shouldMarkAsRefreshed() {
            // When
            widget.markAsRefreshed();

            // Then
            assertThat(widget.getLastRefreshedAt()).isNotNull();
            assertThat(widget.getLastRefreshedAt()).isAfter(widget.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Widget Style and Appearance Tests")
    class WidgetStyleTests {

        private Widget widget;

        @BeforeEach
        void setUp() {
            widget = Widget.create(validWidgetName, dashboardId, Widget.TYPE_CHART);
        }

        @Test
        @DisplayName("Should set and get style configuration")
        void shouldSetAndGetStyleConfiguration() {
            // Given
            String styleConfig = "{\"color\": \"#FF0000\", \"border\": \"2px solid\"}";

            // When
            widget.setStyleConfig(styleConfig);

            // Then
            assertThat(widget.getStyleConfig()).isEqualTo(styleConfig);
        }

        @Test
        @DisplayName("Should update title and description")
        void shouldUpdateTitleAndDescription() {
            // Given
            String newTitle = "Updated Widget Title";
            String newDescription = "Updated widget description";

            // When
            widget.setTitle(newTitle);
            widget.setDescription(newDescription);

            // Then
            assertThat(widget.getTitle()).isEqualTo(newTitle);
            assertThat(widget.getDescription()).isEqualTo(newDescription);
        }
    }

    @Nested
    @DisplayName("Widget Business Logic Tests")
    class WidgetBusinessLogicTests {

        @Test
        @DisplayName("Should calculate widget area correctly")
        void shouldCalculateWidgetArea() {
            // Given
            Widget widget = Widget.create("Test", dashboardId, Widget.TYPE_CHART);
            widget.updateSize(200, 300);

            // When
            double area = widget.getWidth() * widget.getHeight();

            // Then
            assertThat(area).isEqualTo(60000.0);
        }

        @Test
        @DisplayName("Should validate widget dimensions")
        void shouldValidateWidgetDimensions() {
            // Given
            Widget widget = Widget.create("Test", dashboardId, Widget.TYPE_CHART);

            // When & Then - Valid dimensions
            assertDoesNotThrow(() -> widget.updateSize(50, 50));
            assertDoesNotThrow(() -> widget.updateSize(1000, 1000));

            // When & Then - Zero dimensions
            assertDoesNotThrow(() -> widget.updateSize(0, 0));
        }

        @ParameterizedTest
        @ValueSource(strings = {"CHART", "TABLE", "METRIC"})
        @DisplayName("Should support different widget types with default configurations")
        void shouldSupportDifferentWidgetTypes(String widgetType) {
            // When
            Widget widget = Widget.create("Test", dashboardId, widgetType);

            // Then
            assertThat(widget.getWidgetType()).isEqualTo(widgetType);
            assertTrue(widget.isValidType());
        }
    }

    @Nested
    @DisplayName("Widget Equality and HashCode Tests")
    class EqualityTests {

        private Widget widget1;
        private Widget widget2;
        private Widget widget3;

        @BeforeEach
        void setUp() {
            UUID id = UUID.randomUUID();
            widget1 = Widget.builder()
                    .id(id)
                    .name("Test")
                    .dashboardId(dashboardId)
                    .widgetType(Widget.TYPE_CHART)
                    .build();
            widget2 = Widget.builder()
                    .id(id)
                    .name("Test")
                    .dashboardId(dashboardId)
                    .widgetType(Widget.TYPE_CHART)
                    .build();
            widget3 = Widget.builder()
                    .id(UUID.randomUUID())
                    .name("Test")
                    .dashboardId(dashboardId)
                    .widgetType(Widget.TYPE_CHART)
                    .build();
        }

        @Test
        @DisplayName("Should be equal when IDs are same")
        void shouldBeEqualWhenIdsAreSame() {
            // Then
            assertThat(widget1).isEqualTo(widget2);
            assertThat(widget1.hashCode()).isEqualTo(widget2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Then
            assertThat(widget1).isNotEqualTo(widget3);
            assertThat(widget1.hashCode()).isNotEqualTo(widget3.hashCode());
        }
    }
}