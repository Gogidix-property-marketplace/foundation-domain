package com.gogidix.foundation.config.service;

import com.gogidix.foundation.config.dto.ConfigurationDto;
import com.gogidix.foundation.config.dto.ConfigurationSearchRequest;
import com.gogidix.foundation.config.entity.Configuration;
import com.gogidix.foundation.config.enums.ConfigType;
import com.gogidix.foundation.config.enums.Environment;
import com.gogidix.foundation.config.repository.ConfigurationHistoryRepository;
import com.gogidix.foundation.config.repository.ConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private ConfigurationRepository configurationRepository;

    @Mock
    private ConfigurationHistoryRepository historyRepository;

    @InjectMocks
    private ConfigurationService configurationService;

    private Configuration testConfiguration;
    private ConfigurationDto testConfigurationDto;

    @BeforeEach
    void setUp() {
        testConfiguration = new Configuration();
        testConfiguration.setId(1L);
        testConfiguration.setKey("test.key");
        testConfiguration.setValue("test.value");
        testConfiguration.setType(ConfigType.STRING);
        testConfiguration.setEnvironment(Environment.DEVELOPMENT);
        testConfiguration.setApplicationName("test-app");
        testConfiguration.setActive(true);
        testConfiguration.setVersion(1);

        testConfigurationDto = new ConfigurationDto();
        testConfigurationDto.setId(1L);
        testConfigurationDto.setKey("test.key");
        testConfigurationDto.setValue("test.value");
        testConfigurationDto.setType(ConfigType.STRING);
        testConfigurationDto.setEnvironment(Environment.DEVELOPMENT);
        testConfigurationDto.setApplicationName("test-app");
        testConfigurationDto.setActive(true);
    }

    @Test
    void testGetConfigurationById_Exists() {
        when(configurationRepository.findById(1L)).thenReturn(Optional.of(testConfiguration));

        Optional<ConfigurationDto> result = configurationService.getConfigurationById(1L);

        assertTrue(result.isPresent());
        assertEquals("test.key", result.get().getKey());
        assertEquals(Environment.DEVELOPMENT, result.get().getEnvironment());
    }

    @Test
    void testGetConfigurationById_NotExists() {
        when(configurationRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<ConfigurationDto> result = configurationService.getConfigurationById(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void testCreateConfiguration_Success() {
        ConfigurationDto newDto = new ConfigurationDto();
        newDto.setKey("new.key");
        newDto.setValue("new.value");
        newDto.setType(ConfigType.INTEGER);
        newDto.setEnvironment(Environment.PRODUCTION);

        when(configurationRepository.existsByKeyAndEnvironment("new.key", Environment.PRODUCTION))
                .thenReturn(false);
        when(configurationRepository.save(any(Configuration.class))).thenReturn(testConfiguration);
        when(historyRepository.countChangesSince(any(), any())).thenReturn(0L);

        ConfigurationDto result = configurationService.createConfiguration(newDto, "testuser");

        assertNotNull(result);
        assertEquals("test.key", result.getKey());
        verify(configurationRepository).save(any(Configuration.class));
        verify(historyRepository).save(any());
    }

    @Test
    void testCreateConfiguration_DuplicateKey() {
        when(configurationRepository.existsByKeyAndEnvironment("test.key", Environment.DEVELOPMENT))
                .thenReturn(true);

        ConfigurationDto newDto = new ConfigurationDto();
        newDto.setKey("test.key");
        newDto.setEnvironment(Environment.DEVELOPMENT);

        assertThrows(IllegalArgumentException.class, () -> {
            configurationService.createConfiguration(newDto, "testuser");
        });

        verify(configurationRepository, never()).save(any());
    }

    @Test
    void testUpdateConfiguration_Success() {
        when(configurationRepository.findById(1L)).thenReturn(Optional.of(testConfiguration));
        when(configurationRepository.save(any(Configuration.class))).thenReturn(testConfiguration);
        when(historyRepository.countChangesSince(eq(1L), any())).thenReturn(0L);

        ConfigurationDto result = configurationService.updateConfiguration(1L, testConfigurationDto, "testuser");

        assertNotNull(result);
        verify(configurationRepository).save(any(Configuration.class));
        verify(historyRepository).save(any());
    }

    @Test
    void testUpdateConfiguration_NotFound() {
        when(configurationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            configurationService.updateConfiguration(1L, testConfigurationDto, "testuser");
        });

        verify(configurationRepository, never()).save(any());
    }

    @Test
    void testDeleteConfiguration_Success() {
        when(configurationRepository.findById(1L)).thenReturn(Optional.of(testConfiguration));
        when(historyRepository.countChangesSince(eq(1L), any())).thenReturn(0L);

        configurationService.deleteConfiguration(1L, "testuser");

        verify(configurationRepository).deleteById(1L);
        verify(historyRepository).save(any());
    }

    @Test
    void testDeleteConfiguration_NotFound() {
        when(configurationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            configurationService.deleteConfiguration(1L, "testuser");
        });

        verify(configurationRepository, never()).deleteById(any());
    }

    @Test
    void testGetConfigurationsByEnvironment() {
        List<Configuration> configurations = Arrays.asList(testConfiguration);
        when(configurationRepository.findActiveByEnvironment(Environment.DEVELOPMENT))
                .thenReturn(configurations);

        List<ConfigurationDto> result = configurationService.getConfigurationsByEnvironment(Environment.DEVELOPMENT);

        assertEquals(1, result.size());
        assertEquals("test.key", result.get(0).getKey());
    }

    @Test
    void testSearchConfigurations() {
        ConfigurationSearchRequest request = new ConfigurationSearchRequest();
        request.setKey("test");
        request.setEnvironment(Environment.DEVELOPMENT);

        Page<Configuration> configurationPage = new PageImpl<>(Arrays.asList(testConfiguration));
        when(configurationRepository.searchConfigurations(any(), any(), any(), any(Pageable.class)))
                .thenReturn(configurationPage);

        Page<ConfigurationDto> result = configurationService.searchConfigurations(request);

        assertEquals(1, result.getContent().size());
        assertEquals("test.key", result.getContent().get(0).getKey());
    }
}