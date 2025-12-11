package com.gogidix.foundation.dynamic.service;

import com.gogidix.foundation.dynamic.dto.ConfigChangeNotification;
import com.gogidix.foundation.dynamic.dto.DynamicConfigDto;
import com.gogidix.foundation.dynamic.dto.DynamicConfigSearchRequest;
import com.gogidix.foundation.dynamic.entity.DynamicConfig;
import com.gogidix.foundation.dynamic.entity.DynamicConfigHistory;
import com.gogidix.foundation.dynamic.enums.ChangeType;
import com.gogidix.foundation.dynamic.enums.ConfigScope;
import com.gogidix.foundation.dynamic.repository.DynamicConfigHistoryRepository;
import com.gogidix.foundation.dynamic.repository.DynamicConfigRepository;
import com.gogidix.foundation.dynamic.websocket.ConfigChangeNotifier;
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
public class DynamicConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfigService.class);

    @Autowired
    private DynamicConfigRepository configRepository;

    @Autowired
    private DynamicConfigHistoryRepository historyRepository;

    @Autowired
    private ConfigChangeNotifier configChangeNotifier;

    public Page<DynamicConfigDto> searchConfigs(DynamicConfigSearchRequest request) {
        logger.info("Searching dynamic configurations with criteria: key={}, scope={}, application={}",
                   request.getConfigKey(), request.getScope(), request.getApplicationName());

        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<DynamicConfig> configs = configRepository.searchDynamicConfigs(
                request.getConfigKey(),
                request.getScope(),
                request.getApplicationName(),
                request.getServiceName(),
                request.getEnvironment(),
                request.getActive(),
                pageable
        );

        return configs.map(this::convertToDto);
    }

    public Optional<DynamicConfigDto> getConfigById(Long id) {
        logger.debug("Fetching dynamic configuration with ID: {}", id);
        return configRepository.findById(id)
                .map(this::convertToDto);
    }

    public Optional<DynamicConfigDto> getConfigByKeyAndScope(String configKey, ConfigScope scope) {
        logger.debug("Fetching dynamic configuration with key: {} and scope: {}", configKey, scope);
        return configRepository.findByConfigKeyAndScope(configKey, scope)
                .map(this::convertToDto);
    }

    public List<DynamicConfigDto> getActiveConfigsByKeyAndScope(String configKey, ConfigScope scope) {
        logger.debug("Fetching active dynamic configurations with key: {} and scope: {}", configKey, scope);
        List<DynamicConfig> configs = configRepository.findByConfigKeyAndScopeAndActiveTrue(configKey, scope);
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public DynamicConfigDto createConfig(DynamicConfigDto dto, String username) {
        logger.info("Creating new dynamic configuration: key={}, scope={}, user={}",
                   dto.getConfigKey(), dto.getScope(), username);

        if (configRepository.existsByConfigKeyAndScope(dto.getConfigKey(), dto.getScope())) {
            throw new IllegalArgumentException("Configuration with key '" + dto.getConfigKey() +
                    "' already exists for scope '" + dto.getScope() + "'");
        }

        DynamicConfig config = convertToEntity(dto);
        config.setCreatedBy(username);
        config.setVersion(1);

        DynamicConfig savedConfig = configRepository.save(config);

        createHistoryEntry(savedConfig.getId(), dto.getConfigKey(), null, dto.getConfigValue(),
                          dto.getScope(), username, ChangeType.CREATE,
                          "Initial dynamic configuration creation");

        ConfigChangeNotification notification = new ConfigChangeNotification(
                savedConfig.getId(), dto.getConfigKey(), null, dto.getConfigValue(),
                dto.getScope(), ChangeType.CREATE, username);
        notification.setApplicationName(dto.getApplicationName());
        notification.setServiceName(dto.getServiceName());
        notification.setEnvironment(dto.getEnvironment());
        notification.setUserId(dto.getUserId());
        notification.setRequiresRestart(dto.getRequiresRestart());
        notification.setVersion(savedConfig.getVersion());

        configChangeNotifier.notifyConfigChange(notification);

        logger.info("Successfully created dynamic configuration with ID: {}", savedConfig.getId());
        return convertToDto(savedConfig);
    }

    public DynamicConfigDto updateConfig(Long id, DynamicConfigDto dto, String username) {
        logger.info("Updating dynamic configuration with ID: {}, user={}", id, username);

        Optional<DynamicConfig> existingOpt = configRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Dynamic configuration not found with ID: " + id);
        }

        DynamicConfig existing = existingOpt.get();
        String oldValue = existing.getConfigValue();

        existing.setConfigKey(dto.getConfigKey());
        existing.setConfigValue(dto.getConfigValue());
        existing.setDefaultValue(dto.getDefaultValue());
        existing.setScope(dto.getScope());
        existing.setApplicationName(dto.getApplicationName());
        existing.setServiceName(dto.getServiceName());
        existing.setEnvironment(dto.getEnvironment());
        existing.setUserId(dto.getUserId());
        existing.setDescription(dto.getDescription());
        existing.setActive(dto.getActive());
        existing.setTags(dto.getTags() != null ? String.join(",", dto.getTags()) : null);
        existing.setEncrypted(dto.getEncrypted());
        existing.setRequiresRestart(dto.getRequiresRestart());
        existing.setUpdatedBy(username);

        DynamicConfig updatedConfig = configRepository.save(existing);

        createHistoryEntry(id, dto.getConfigKey(), oldValue, dto.getConfigValue(),
                          dto.getScope(), username, ChangeType.UPDATE,
                          "Dynamic configuration updated");

        ConfigChangeNotification notification = new ConfigChangeNotification(
                id, dto.getConfigKey(), oldValue, dto.getConfigValue(),
                dto.getScope(), ChangeType.UPDATE, username);
        notification.setApplicationName(dto.getApplicationName());
        notification.setServiceName(dto.getServiceName());
        notification.setEnvironment(dto.getEnvironment());
        notification.setUserId(dto.getUserId());
        notification.setRequiresRestart(dto.getRequiresRestart());
        notification.setVersion(updatedConfig.getVersion());

        configChangeNotifier.notifyConfigChange(notification);

        logger.info("Successfully updated dynamic configuration with ID: {}", id);
        return convertToDto(updatedConfig);
    }

    public void deleteConfig(Long id, String username) {
        logger.info("Deleting dynamic configuration with ID: {}, user={}", id, username);

        Optional<DynamicConfig> configOpt = configRepository.findById(id);
        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException("Dynamic configuration not found with ID: " + id);
        }

        DynamicConfig config = configOpt.get();

        createHistoryEntry(id, config.getConfigKey(), config.getConfigValue(), null,
                          config.getScope(), username, ChangeType.DELETE,
                          "Dynamic configuration deleted");

        configRepository.deleteById(id);

        ConfigChangeNotification notification = new ConfigChangeNotification(
                id, config.getConfigKey(), config.getConfigValue(), null,
                config.getScope(), ChangeType.DELETE, username);
        notification.setApplicationName(config.getApplicationName());
        notification.setServiceName(config.getServiceName());
        notification.setEnvironment(config.getEnvironment());
        notification.setUserId(config.getUserId());
        notification.setRequiresRestart(config.getRequiresRestart());
        notification.setVersion(config.getVersion());

        configChangeNotifier.notifyConfigChange(notification);

        logger.info("Successfully deleted dynamic configuration with ID: {}", id);
    }

    public List<DynamicConfigDto> getConfigsByScope(ConfigScope scope) {
        logger.debug("Fetching dynamic configurations for scope: {}", scope);
        List<DynamicConfig> configs = configRepository.findByScopeAndActiveTrue(scope);
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<DynamicConfigDto> getConfigsByApplication(String applicationName) {
        logger.debug("Fetching dynamic configurations for application: {}", applicationName);
        List<DynamicConfig> configs = configRepository.findByApplicationNameAndActiveTrue(applicationName);
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<DynamicConfigDto> getConfigsByService(String serviceName) {
        logger.debug("Fetching dynamic configurations for service: {}", serviceName);
        List<DynamicConfig> configs = configRepository.findByServiceNameAndActiveTrue(serviceName);
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<DynamicConfigDto> getConfigsByEnvironment(String environment) {
        logger.debug("Fetching dynamic configurations for environment: {}", environment);
        List<DynamicConfig> configs = configRepository.findByEnvironmentAndActiveTrue(environment);
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<DynamicConfigDto> getConfigsByUser(String userId) {
        logger.debug("Fetching dynamic configurations for user: {}", userId);
        List<DynamicConfig> configs = configRepository.findByUserIdAndActiveTrue(userId);
        return configs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<String> getAllApplicationNames() {
        logger.debug("Fetching all application names");
        return configRepository.findAllApplicationNames();
    }

    public List<String> getAllServiceNames() {
        logger.debug("Fetching all service names");
        return configRepository.findAllServiceNames();
    }

    public List<String> getAllEnvironments() {
        logger.debug("Fetching all environments");
        return configRepository.findAllEnvironments();
    }

    public Page<DynamicConfigDto> getConfigHistory(Long configId, Pageable pageable) {
        logger.debug("Fetching history for dynamic configuration ID: {}", configId);
        Page<DynamicConfigHistory> history = historyRepository.findByConfigIdOrderByCreatedAtDesc(
                configId, pageable);
        return history.map(this::convertHistoryToDto);
    }

    public List<DynamicConfigDto> getRecentChanges(LocalDateTime since) {
        logger.debug("Fetching recent configuration changes since: {}", since);
        List<DynamicConfigHistory> history = historyRepository.findRecentChanges(since);
        return history.stream()
                .map(this::convertHistoryToDto)
                .collect(Collectors.toList());
    }

    private DynamicConfig convertToEntity(DynamicConfigDto dto) {
        DynamicConfig config = new DynamicConfig();
        config.setId(dto.getId());
        config.setConfigKey(dto.getConfigKey());
        config.setConfigValue(dto.getConfigValue());
        config.setDefaultValue(dto.getDefaultValue());
        config.setScope(dto.getScope());
        config.setApplicationName(dto.getApplicationName());
        config.setServiceName(dto.getServiceName());
        config.setEnvironment(dto.getEnvironment());
        config.setUserId(dto.getUserId());
        config.setDescription(dto.getDescription());
        config.setActive(dto.getActive());
        config.setTags(dto.getTags() != null ? String.join(",", dto.getTags()) : null);
        config.setEncrypted(dto.getEncrypted());
        config.setRequiresRestart(dto.getRequiresRestart());
        return config;
    }

    private DynamicConfigDto convertToDto(DynamicConfig config) {
        DynamicConfigDto dto = new DynamicConfigDto();
        dto.setId(config.getId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setDefaultValue(config.getDefaultValue());
        dto.setScope(config.getScope());
        dto.setApplicationName(config.getApplicationName());
        dto.setServiceName(config.getServiceName());
        dto.setEnvironment(config.getEnvironment());
        dto.setUserId(config.getUserId());
        dto.setDescription(config.getDescription());
        dto.setCreatedBy(config.getCreatedBy());
        dto.setUpdatedBy(config.getUpdatedBy());
        dto.setVersion(config.getVersion());
        dto.setActive(config.getActive());
        dto.setEncrypted(config.getEncrypted());
        dto.setRequiresRestart(config.getRequiresRestart());

        if (config.getTags() != null && !config.getTags().isEmpty()) {
            dto.setTags(Arrays.asList(config.getTags().split(",")));
        }

        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());

        return dto;
    }

    private DynamicConfigDto convertHistoryToDto(DynamicConfigHistory history) {
        DynamicConfigDto dto = new DynamicConfigDto();
        dto.setId(history.getId());
        dto.setConfigKey(history.getConfigKey());
        dto.setConfigValue(history.getNewValue());
        dto.setScope(history.getScope());
        dto.setApplicationName(history.getApplicationName());
        dto.setServiceName(history.getServiceName());
        dto.setEnvironment(history.getEnvironment());
        dto.setUserId(history.getUserId());
        dto.setCreatedBy(history.getChangedBy());
        dto.setVersion(history.getVersion());
        dto.setCreatedAt(history.getCreatedAt());

        return dto;
    }

    private void createHistoryEntry(Long configId, String configKey, String oldValue,
                                  String newValue, ConfigScope scope, String username,
                                  ChangeType changeType, String reason) {
        DynamicConfigHistory history = new DynamicConfigHistory(
                configId, configKey, oldValue, newValue, scope, username, changeType
        );
        history.setChangeReason(reason);
        history.setVersion((int) historyRepository.countChangesSince(configId) + 1);

        historyRepository.save(history);
    }
}