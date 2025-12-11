package com.gogidix.foundation.dynamic.service;

import com.gogidix.foundation.dynamic.dto.DynamicConfigDto;
import com.gogidix.foundation.dynamic.dto.DynamicConfigSearchRequest;
import com.gogidix.foundation.dynamic.entity.DynamicConfig;
import com.gogidix.foundation.dynamic.enums.ChangeType;
import com.gogidix.foundation.dynamic.enums.ConfigScope;
import com.gogidix.foundation.dynamic.repository.DynamicConfigHistoryRepository;
import com.gogidix.foundation.dynamic.repository.DynamicConfigRepository;
import com.gogidix.foundation.dynamic.websocket.ConfigChangeNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicConfigServiceTest {

    @Mock
    private DynamicConfigRepository configRepository;

    @Mock
    private DynamicConfigHistoryRepository historyRepository;

    @Mock
    private ConfigChangeNotifier configChangeNotifier;

    @InjectMocks
    private DynamicConfigService dynamicConfigService;

    private DynamicConfig testConfig;
    private DynamicConfigDto testConfigDto;

    @BeforeEach
    void setUp() {
        testConfig = new DynamicConfig();
        testConfig.setId(1L);
        testConfig.setConfigKey("test.feature.enabled");
        testConfig.setConfigValue("true");
        testConfig.setScope(ConfigScope.GLOBAL);
        testConfig.setActive(true);
        testConfig.setVersion(1);
        testConfig.setCreatedAt(LocalDateTime.now());
        testConfig.setUpdatedAt(LocalDateTime.now());

        testConfigDto = new DynamicConfigDto();
        testConfigDto.setConfigKey("test.feature.enabled");
        testConfigDto.setConfigValue("true");
        testConfigDto.setScope(ConfigScope.GLOBAL);
        testConfigDto.setActive(true);
    }

    @Test
    void searchConfigs_ShouldReturnPageOfConfigs() {
        // Given
        DynamicConfigSearchRequest request = new DynamicConfigSearchRequest();
        request.setConfigKey("test");
        request.setScope(ConfigScope.GLOBAL);
        request.setPage(0);
        request.setSize(20);

        Pageable pageable = PageRequest.of(0, 20);
        Page<DynamicConfig> configPage = new PageImpl<>(Arrays.asList(testConfig));

        when(configRepository.searchDynamicConfigs(
            anyString(), any(ConfigScope.class), anyString(), anyString(),
            anyString(), anyBoolean(), any(Pageable.class)))
            .thenReturn(configPage);

        // When
        Page<DynamicConfigDto> result = dynamicConfigService.searchConfigs(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("test.feature.enabled", result.getContent().get(0).getConfigKey());
        verify(configRepository).searchDynamicConfigs(
            anyString(), any(ConfigScope.class), anyString(), anyString(),
            anyString(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void getConfigById_WhenConfigExists_ShouldReturnConfig() {
        // Given
        when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));

        // When
        Optional<DynamicConfigDto> result = dynamicConfigService.getConfigById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("test.feature.enabled", result.get().getConfigKey());
        verify(configRepository).findById(1L);
    }

    @Test
    void getConfigById_WhenConfigNotExists_ShouldReturnEmpty() {
        // Given
        when(configRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<DynamicConfigDto> result = dynamicConfigService.getConfigById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(configRepository).findById(1L);
    }

    @Test
    void getConfigByKeyAndScope_ShouldReturnConfig() {
        // Given
        when(configRepository.findByConfigKeyAndScope("test.feature.enabled", ConfigScope.GLOBAL))
            .thenReturn(Optional.of(testConfig));

        // When
        Optional<DynamicConfigDto> result = dynamicConfigService.getConfigByKeyAndScope(
            "test.feature.enabled", ConfigScope.GLOBAL);

        // Then
        assertTrue(result.isPresent());
        assertEquals("test.feature.enabled", result.get().getConfigKey());
        verify(configRepository).findByConfigKeyAndScope("test.feature.enabled", ConfigScope.GLOBAL);
    }

    @Test
    void createConfig_WhenConfigNotExists_ShouldCreateConfig() {
        // Given
        when(configRepository.existsByConfigKeyAndScope("test.feature.enabled", ConfigScope.GLOBAL))
            .thenReturn(false);
        when(configRepository.save(any(DynamicConfig.class))).thenReturn(testConfig);
        when(historyRepository.countChangesSince(anyLong())).thenReturn(0L);

        // When
        DynamicConfigDto result = dynamicConfigService.createConfig(testConfigDto, "testuser");

        // Then
        assertNotNull(result);
        assertEquals("test.feature.enabled", result.getConfigKey());
        assertEquals(ConfigScope.GLOBAL, result.getScope());
        verify(configRepository).existsByConfigKeyAndScope("test.feature.enabled", ConfigScope.GLOBAL);
        verify(configRepository).save(any(DynamicConfig.class));
        verify(historyRepository).save(any());
        verify(configChangeNotifier).notifyConfigChange(any());
    }

    @Test
    void createConfig_WhenConfigExists_ShouldThrowException() {
        // Given
        when(configRepository.existsByConfigKeyAndScope("test.feature.enabled", ConfigScope.GLOBAL))
            .thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dynamicConfigService.createConfig(testConfigDto, "testuser")
        );

        assertEquals("Configuration with key 'test.feature.enabled' already exists for scope 'GLOBAL'",
                    exception.getMessage());
        verify(configRepository).existsByConfigKeyAndScope("test.feature.enabled", ConfigScope.GLOBAL);
        verify(configRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
        verify(configChangeNotifier, never()).notifyConfigChange(any());
    }

    @Test
    void updateConfig_WhenConfigExists_ShouldUpdateConfig() {
        // Given
        DynamicConfigDto updatedConfigDto = new DynamicConfigDto();
        updatedConfigDto.setConfigKey("test.feature.enabled");
        updatedConfigDto.setConfigValue("false");
        updatedConfigDto.setScope(ConfigScope.GLOBAL);
        updatedConfigDto.setActive(true);

        when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(DynamicConfig.class))).thenReturn(testConfig);
        when(historyRepository.countChangesSince(anyLong())).thenReturn(1L);

        // When
        DynamicConfigDto result = dynamicConfigService.updateConfig(1L, updatedConfigDto, "testuser");

        // Then
        assertNotNull(result);
        verify(configRepository).findById(1L);
        verify(configRepository).save(any(DynamicConfig.class));
        verify(historyRepository).save(any());
        verify(configChangeNotifier).notifyConfigChange(any());
    }

    @Test
    void updateConfig_WhenConfigNotExists_ShouldThrowException() {
        // Given
        when(configRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dynamicConfigService.updateConfig(1L, testConfigDto, "testuser")
        );

        assertEquals("Dynamic configuration not found with ID: 1", exception.getMessage());
        verify(configRepository).findById(1L);
        verify(configRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
        verify(configChangeNotifier, never()).notifyConfigChange(any());
    }

    @Test
    void deleteConfig_WhenConfigExists_ShouldDeleteConfig() {
        // Given
        when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));

        // When
        dynamicConfigService.deleteConfig(1L, "testuser");

        // Then
        verify(configRepository).findById(1L);
        verify(configRepository).deleteById(1L);
        verify(historyRepository).save(any());
        verify(configChangeNotifier).notifyConfigChange(any());
    }

    @Test
    void deleteConfig_WhenConfigNotExists_ShouldThrowException() {
        // Given
        when(configRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> dynamicConfigService.deleteConfig(1L, "testuser")
        );

        assertEquals("Dynamic configuration not found with ID: 1", exception.getMessage());
        verify(configRepository).findById(1L);
        verify(configRepository, never()).deleteById(any());
        verify(historyRepository, never()).save(any());
        verify(configChangeNotifier, never()).notifyConfigChange(any());
    }

    @Test
    void getConfigsByScope_ShouldReturnConfigs() {
        // Given
        when(configRepository.findByScopeAndActiveTrue(ConfigScope.GLOBAL))
            .thenReturn(Arrays.asList(testConfig));

        // When
        List<DynamicConfigDto> result = dynamicConfigService.getConfigsByScope(ConfigScope.GLOBAL);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.feature.enabled", result.get(0).getConfigKey());
        verify(configRepository).findByScopeAndActiveTrue(ConfigScope.GLOBAL);
    }

    @Test
    void getConfigsByApplication_ShouldReturnConfigs() {
        // Given
        when(configRepository.findByApplicationNameAndActiveTrue("test-app"))
            .thenReturn(Arrays.asList(testConfig));

        // When
        List<DynamicConfigDto> result = dynamicConfigService.getConfigsByApplication("test-app");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(configRepository).findByApplicationNameAndActiveTrue("test-app");
    }

    @Test
    void getAllApplicationNames_ShouldReturnApplicationNames() {
        // Given
        List<String> appNames = Arrays.asList("app1", "app2", "app3");
        when(configRepository.findAllApplicationNames()).thenReturn(appNames);

        // When
        List<String> result = dynamicConfigService.getAllApplicationNames();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("app1", result.get(0));
        verify(configRepository).findAllApplicationNames();
    }

    @Test
    void getConfigHistory_ShouldReturnConfigHistory() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        when(historyRepository.findByConfigIdOrderByCreatedAtDesc(1L, pageable))
            .thenReturn(new PageImpl<>(Arrays.asList()));

        // When
        Page<DynamicConfigDto> result = dynamicConfigService.getConfigHistory(1L, pageable);

        // Then
        assertNotNull(result);
        verify(historyRepository).findByConfigIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void getRecentChanges_ShouldReturnRecentChanges() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        when(historyRepository.findRecentChanges(since)).thenReturn(Arrays.asList());

        // When
        List<DynamicConfigDto> result = dynamicConfigService.getRecentChanges(since);

        // Then
        assertNotNull(result);
        verify(historyRepository).findRecentChanges(since);
    }
}