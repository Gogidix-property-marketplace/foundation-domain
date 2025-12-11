-- Create dynamic_configs table
CREATE TABLE dynamic_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    default_value TEXT,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('GLOBAL', 'APPLICATION', 'SERVICE', 'ENVIRONMENT', 'USER')),
    application_name VARCHAR(255),
    service_name VARCHAR(255),
    environment VARCHAR(100),
    user_id VARCHAR(100),
    description VARCHAR(1000),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT true,
    tags VARCHAR(500),
    encrypted BOOLEAN DEFAULT false,
    requires_restart BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique constraint on config_key and scope for global configs
CREATE UNIQUE INDEX uq_dynamic_global_key ON dynamic_configs(config_key, scope)
WHERE scope = 'GLOBAL';

-- Create composite unique constraints for scoped configurations
CREATE UNIQUE INDEX uq_dynamic_app_key ON dynamic_configs(config_key, scope, application_name)
WHERE scope = 'APPLICATION' AND application_name IS NOT NULL;

CREATE UNIQUE INDEX uq_dynamic_service_key ON dynamic_configs(config_key, scope, service_name)
WHERE scope = 'SERVICE' AND service_name IS NOT NULL;

CREATE UNIQUE INDEX uq_dynamic_env_key ON dynamic_configs(config_key, scope, environment)
WHERE scope = 'ENVIRONMENT' AND environment IS NOT NULL;

CREATE UNIQUE INDEX uq_dynamic_user_key ON dynamic_configs(config_key, scope, user_id)
WHERE scope = 'USER' AND user_id IS NOT NULL;

-- Create indexes for performance
CREATE INDEX idx_dynamic_key_scope ON dynamic_configs(config_key, scope);
CREATE INDEX idx_dynamic_application ON dynamic_configs(application_name);
CREATE INDEX idx_dynamic_service ON dynamic_configs(service_name);
CREATE INDEX idx_dynamic_environment ON dynamic_configs(environment);
CREATE INDEX idx_dynamic_user_id ON dynamic_configs(user_id);
CREATE INDEX idx_dynamic_created_at ON dynamic_configs(created_at);
CREATE INDEX idx_dynamic_updated_at ON dynamic_configs(updated_at);
CREATE INDEX idx_dynamic_active ON dynamic_configs(active) WHERE active = true;
CREATE INDEX idx_dynamic_encrypted ON dynamic_configs(encrypted);
CREATE INDEX idx_dynamic_requires_restart ON dynamic_configs(requires_restart);

-- Create dynamic_config_history table for audit trail
CREATE TABLE dynamic_config_history (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    scope VARCHAR(20) NOT NULL,
    application_name VARCHAR(255),
    service_name VARCHAR(255),
    environment VARCHAR(100),
    user_id VARCHAR(100),
    change_type VARCHAR(20) NOT NULL CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE')),
    change_reason TEXT,
    changed_by VARCHAR(100),
    version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for history table
CREATE INDEX idx_hist_config_id ON dynamic_config_history(config_id);
CREATE INDEX idx_hist_key_scope ON dynamic_config_history(config_key, scope);
CREATE INDEX idx_hist_created_at ON dynamic_config_history(created_at);
CREATE INDEX idx_hist_changed_by ON dynamic_config_history(changed_by);
CREATE INDEX idx_hist_change_type ON dynamic_config_history(change_type);
CREATE INDEX idx_hist_application ON dynamic_config_history(application_name);
CREATE INDEX idx_hist_service ON dynamic_config_history(service_name);
CREATE INDEX idx_hist_environment ON dynamic_config_history(environment);
CREATE INDEX idx_hist_user_id ON dynamic_config_history(user_id);

-- Add comments for documentation
COMMENT ON TABLE dynamic_configs IS 'Stores dynamic configurations with real-time update capabilities and multi-scoped support';
COMMENT ON TABLE dynamic_config_history IS 'Audit trail for dynamic configuration changes with WebSocket notifications';

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_dynamic_config_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_dynamic_configs_updated_at
    BEFORE UPDATE ON dynamic_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_dynamic_config_updated_at();

-- Create trigger for history tracking
CREATE OR REPLACE FUNCTION log_dynamic_config_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO dynamic_config_history (config_id, config_key, old_value, new_value, scope, application_name, service_name, environment, user_id, change_type, changed_by, version)
        VALUES (NEW.id, NEW.config_key, NULL, NEW.config_value, NEW.scope, NEW.application_name, NEW.service_name, NEW.environment, NEW.user_id, 'CREATE', NEW.created_by, NEW.version);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.config_value IS DISTINCT FROM NEW.config_value OR OLD.active IS DISTINCT FROM NEW.active THEN
            INSERT INTO dynamic_config_history (config_id, config_key, old_value, new_value, scope, application_name, service_name, environment, user_id, change_type, changed_by, version)
            VALUES (NEW.id, NEW.config_key, OLD.config_value, NEW.config_value, NEW.scope, NEW.application_name, NEW.service_name, NEW.environment, NEW.user_id,
                    CASE WHEN OLD.active = false AND NEW.active = true THEN 'ACTIVATE'
                         WHEN OLD.active = true AND NEW.active = false THEN 'DEACTIVATE'
                         ELSE 'UPDATE' END,
                    NEW.updated_by, NEW.version);
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO dynamic_config_history (config_id, config_key, old_value, new_value, scope, application_name, service_name, environment, user_id, change_type, changed_by, version)
        VALUES (OLD.id, OLD.config_key, OLD.config_value, NULL, OLD.scope, OLD.application_name, OLD.service_name, OLD.environment, OLD.user_id, 'DELETE', 'system', OLD.version);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

CREATE TRIGGER dynamic_config_history_trigger
    AFTER INSERT OR UPDATE OR DELETE ON dynamic_configs
    FOR EACH ROW
    EXECUTE FUNCTION log_dynamic_config_changes();