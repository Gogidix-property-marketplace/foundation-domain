package com.gogidix.foundation.config.service;

import com.gogidix.foundation.config.dto.ConfigurationDto;
import com.gogidix.foundation.config.dto.ConfigurationSearchRequest;
import com.gogidix.foundation.config.entity.Configuration;
import com.gogidix.foundation.config.entity.ConfigurationHistory;
import com.gogidix.foundation.config.enums.ConfigType;
import com.gogidix.foundation.config.enums.Environment;
import com.gogidix.foundation.config.repository.ConfigurationHistoryRepository;
import com.gogidix.foundation.config.repository.ConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private ConfigurationHistoryRepository historyRepository;

    public Page<ConfigurationDto> searchConfigurations(ConfigurationSearchRequest request) {
        logger.info("Searching configurations with criteria: key={}, environment={}, application={}",
                   request.getKey(), request.getEnvironment(), request.getApplicationName());

        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Configuration> configurations = configurationRepository.searchConfigurations(
                request.getKey(),
                request.getEnvironment(),
                request.getApplicationName(),
                pageable
        );

        return configurations.map(this::convertToDto);
    }

    public Optional<ConfigurationDto> getConfigurationById(Long id) {
        logger.debug("Fetching configuration with ID: {}", id);
        return configurationRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<ConfigurationDto> getConfigurationByKeyAndEnvironment(String key, Environment environment) {
        logger.debug("Fetching configuration with key: {} and environment: {}", key, environment);
        return configurationRepository.findByKeyAndEnvironment(key, environment)
                .map(this::convertToDto);
    }

    public ConfigurationDto createConfiguration(ConfigurationDto dto, String username) {
        logger.info("Creating new configuration: key={}, environment={}, user={}",
                   dto.getKey(), dto.getEnvironment(), username);

        if (configurationRepository.existsByKeyAndEnvironment(dto.getKey(), dto.getEnvironment())) {
            throw new IllegalArgumentException("Configuration with key '" + dto.getKey() +
                    "' already exists for environment '" + dto.getEnvironment() + "'");
        }

        Configuration configuration = convertToEntity(dto);
        configuration.setCreatedBy(username);
        configuration.setVersion(1);

        Configuration savedConfiguration = configurationRepository.save(configuration);

        createHistoryEntry(savedConfiguration.getId(), dto.getKey(), null, dto.getValue(),
                          dto.getType(), dto.getEnvironment(), username, "CREATE",
                          "Initial configuration creation");

        logger.info("Successfully created configuration with ID: {}", savedConfiguration.getId());
        return convertToDto(savedConfiguration);
    }

    public ConfigurationDto updateConfiguration(Long id, ConfigurationDto dto, String username) {
        logger.info("Updating configuration with ID: {}, user={}", id, username);

        Optional<Configuration> existingOpt = configurationRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Configuration not found with ID: " + id);
        }

        Configuration existing = existingOpt.get();
        String oldValue = existing.getValue();

        existing.setKey(dto.getKey());
        existing.setValue(dto.getValue());
        existing.setType(dto.getType());
        existing.setEnvironment(dto.getEnvironment());
        existing.setApplicationName(dto.getApplicationName());
        existing.setDescription(dto.getDescription());
        existing.setActive(dto.getActive());
        existing.setTags(dto.getTags() != null ? String.join(",", dto.getTags()) : null);
        existing.setUpdatedBy(username);

        Configuration updatedConfiguration = configurationRepository.save(existing);

        createHistoryEntry(id, dto.getKey(), oldValue, dto.getValue(),
                          dto.getType(), dto.getEnvironment(), username, "UPDATE",
                          "Configuration updated");

        logger.info("Successfully updated configuration with ID: {}", id);
        return convertToDto(updatedConfiguration);
    }

    public void deleteConfiguration(Long id, String username) {
        logger.info("Deleting configuration with ID: {}, user={}", id, username);

        Optional<Configuration> configurationOpt = configurationRepository.findById(id);
        if (configurationOpt.isEmpty()) {
            throw new IllegalArgumentException("Configuration not found with ID: " + id);
        }

        Configuration configuration = configurationOpt.get();

        createHistoryEntry(id, configuration.getKey(), configuration.getValue(), null,
                          configuration.getType(), configuration.getEnvironment(), username, "DELETE",
                          "Configuration deleted");

        configurationRepository.deleteById(id);
        logger.info("Successfully deleted configuration with ID: {}", id);
    }

    public List<ConfigurationDto> getConfigurationsByEnvironment(Environment environment) {
        logger.debug("Fetching configurations for environment: {}", environment);
        List<Configuration> configurations = configurationRepository.findActiveByEnvironment(environment);
        return configurations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ConfigurationDto> getConfigurationsByApplication(String applicationName) {
        logger.debug("Fetching configurations for application: {}", applicationName);
        List<Configuration> configurations = configurationRepository.findByApplicationName(applicationName);
        return configurations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<String> getAllApplicationNames() {
        logger.debug("Fetching all application names");
        return configurationRepository.findAllApplicationNames();
    }

    public Page<ConfigurationDto> getConfigurationHistory(Long configurationId, Pageable pageable) {
        logger.debug("Fetching history for configuration ID: {}", configurationId);
        Page<ConfigurationHistory> history = historyRepository.findByConfigurationIdOrderByCreatedAtDesc(
                configurationId, pageable);
        return history.map(this::convertHistoryToDto);
    }

    private Configuration convertToEntity(ConfigurationDto dto) {
        Configuration configuration = new Configuration();
        configuration.setId(dto.getId());
        configuration.setKey(dto.getKey());
        configuration.setValue(dto.getValue());
        configuration.setType(dto.getType());
        configuration.setEnvironment(dto.getEnvironment());
        configuration.setApplicationName(dto.getApplicationName());
        configuration.setDescription(dto.getDescription());
        configuration.setActive(dto.getActive());
        configuration.setTags(dto.getTags() != null ? String.join(",", dto.getTags()) : null);
        return configuration;
    }

    private ConfigurationDto convertToDto(Configuration configuration) {
        ConfigurationDto dto = new ConfigurationDto();
        dto.setId(configuration.getId());
        dto.setKey(configuration.getKey());
        dto.setValue(configuration.getValue());
        dto.setType(configuration.getType());
        dto.setEnvironment(configuration.getEnvironment());
        dto.setApplicationName(configuration.getApplicationName());
        dto.setDescription(configuration.getDescription());
        dto.setCreatedBy(configuration.getCreatedBy());
        dto.setUpdatedBy(configuration.getUpdatedBy());
        dto.setVersion(configuration.getVersion());
        dto.setActive(configuration.getActive());

        if (configuration.getTags() != null && !configuration.getTags().isEmpty()) {
            dto.setTags(Arrays.asList(configuration.getTags().split(",")));
        }

        dto.setCreatedAt(configuration.getCreatedAt());
        dto.setUpdatedAt(configuration.getUpdatedAt());

        return dto;
    }

    private ConfigurationDto convertHistoryToDto(ConfigurationHistory history) {
        ConfigurationDto dto = new ConfigurationDto();
        dto.setId(history.getId());
        dto.setKey(history.getConfigKey());
        dto.setValue(history.getNewValue());
        dto.setType(history.getType());
        dto.setEnvironment(history.getEnvironment());
        dto.setApplicationName(history.getApplicationName());
        dto.setCreatedBy(history.getChangedBy());
        dto.setVersion(history.getVersion());
        dto.setCreatedAt(history.getCreatedAt());

        return dto;
    }

    private void createHistoryEntry(Long configurationId, String configKey, String oldValue,
                                  String newValue, ConfigType type, Environment environment,
                                  String username, String changeType, String reason) {
        ConfigurationHistory history = new ConfigurationHistory(
                configurationId, configKey, oldValue, newValue, type, environment, username, changeType
        );
        history.setChangeReason(reason);
        history.setVersion((int)historyRepository.countChangesSince(configurationId, LocalDateTime.MIN) + 1);

        historyRepository.save(history);
    }
}