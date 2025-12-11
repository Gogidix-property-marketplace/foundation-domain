-- Create configurations table
CREATE TABLE configurations (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(255) NOT NULL,
    value TEXT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('STRING', 'INTEGER', 'BOOLEAN', 'DOUBLE', 'JSON', 'YAML', 'ENCRYPTED')),
    environment VARCHAR(20) NOT NULL CHECK (environment IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION', 'TESTING')),
    application_name VARCHAR(255),
    description VARCHAR(1000),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT true,
    tags VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique constraint on key and environment
CREATE UNIQUE INDEX uq_config_key_env ON configurations(key, environment);

-- Create indexes for performance
CREATE INDEX idx_config_key_env ON configurations(key, environment);
CREATE INDEX idx_config_application ON configurations(application_name);
CREATE INDEX idx_config_environment ON configurations(environment);
CREATE INDEX idx_config_created_at ON configurations(created_at);
CREATE INDEX idx_config_active ON configurations(active) WHERE active = true;

-- Create configuration_history table for audit trail
CREATE TABLE configuration_history (
    id BIGSERIAL PRIMARY KEY,
    configuration_id BIGINT NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    type VARCHAR(20) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    application_name VARCHAR(255),
    changed_by VARCHAR(100),
    change_type VARCHAR(50),
    change_reason TEXT,
    version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for history table
CREATE INDEX idx_hist_config_id ON configuration_history(configuration_id);
CREATE INDEX idx_hist_key_env ON configuration_history(config_key, environment);
CREATE INDEX idx_hist_created_at ON configuration_history(created_at);
CREATE INDEX idx_hist_changed_by ON configuration_history(changed_by);
CREATE INDEX idx_hist_change_type ON configuration_history(change_type);

-- Add comments for documentation
COMMENT ON TABLE configurations IS 'Stores application configurations with versioning and environment support';
COMMENT ON TABLE configuration_history IS 'Audit trail for configuration changes';

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_configurations_updated_at
    BEFORE UPDATE ON configurations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();