-- Migration script for creating UserManagement table
-- Generated on: 2025-11-28

-- Create UserManagement table
CREATE TABLE UserManagement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 1
);

-- Create indexes for performance
CREATE INDEX idx_UserManagement_status ON UserManagement (status);
CREATE INDEX idx_UserManagement_created_at ON UserManagement (created_at);
CREATE INDEX idx_UserManagement_updated_at ON UserManagement (updated_at);
CREATE INDEX idx_UserManagement_name ON UserManagement (name);
CREATE INDEX idx_UserManagement_created_by ON UserManagement (created_by);

-- Add unique constraint on name
ALTER TABLE UserManagement ADD CONSTRAINT uk_UserManagement_name UNIQUE (name);

-- Add check constraint for status
ALTER TABLE UserManagement ADD CONSTRAINT chk_UserManagement_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED', 'PENDING', 'SUSPENDED', 'UNDER_REVIEW', 'PENDING_DELETION', 'TERMINATED'));

-- Add check constraint for version
ALTER TABLE UserManagement ADD CONSTRAINT chk_UserManagement_version
    CHECK (version > 0);

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_UserManagement_updated_at
    BEFORE UPDATE ON UserManagement
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create trigger to set initial version
CREATE OR REPLACE FUNCTION set_initial_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = 1;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER set_UserManagement_initial_version
    BEFORE INSERT ON UserManagement
    FOR EACH ROW
    EXECUTE FUNCTION set_initial_version();

-- Add comments
COMMENT ON TABLE UserManagement IS ' entity table';
COMMENT ON COLUMN UserManagement.id IS 'Unique identifier for the Usermanagement';
COMMENT ON COLUMN UserManagement.name IS 'Name of the Usermanagement';
COMMENT ON COLUMN UserManagement.description IS 'Description of the Usermanagement';
COMMENT ON COLUMN UserManagement.status IS 'Current status of the Usermanagement';
COMMENT ON COLUMN UserManagement.created_by IS 'User who created the Usermanagement';
COMMENT ON COLUMN UserManagement.created_at IS 'Timestamp when the Usermanagement was created';
COMMENT ON COLUMN UserManagement.updated_by IS 'User who last updated the Usermanagement';
COMMENT ON COLUMN UserManagement.updated_at IS 'Timestamp when the Usermanagement was last updated';
COMMENT ON COLUMN UserManagement.version IS 'Optimistic locking version number';

-- Create audit table for tracking changes
CREATE TABLE UserManagement_audit (
    id BIGSERIAL PRIMARY KEY,
    UserManagement_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL
);

-- Create indexes for audit table
CREATE INDEX idx_UserManagement_audit_UserManagement_id ON UserManagement_audit (UserManagement_id);
CREATE INDEX idx_UserManagement_audit_operation ON UserManagement_audit (operation);
CREATE INDEX idx_UserManagement_audit_changed_at ON UserManagement_audit (changed_at);

-- Add comments to audit table
COMMENT ON TABLE UserManagement_audit IS 'Audit trail for UserManagement changes';
COMMENT ON COLUMN UserManagement_audit.operation IS 'Type of operation (INSERT, UPDATE, DELETE)';
COMMENT ON COLUMN UserManagement_audit.old_data IS 'Previous state of the record';
COMMENT ON COLUMN UserManagement_audit.new_data IS 'New state of the record';
COMMENT ON COLUMN UserManagement_audit.changed_by IS 'User who made the change';
COMMENT ON COLUMN UserManagement_audit.changed_at IS 'Timestamp of the change';
COMMENT ON COLUMN UserManagement_audit.version IS 'Version number of the record';

-- Create audit trigger function
CREATE OR REPLACE FUNCTION audit_UserManagement()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO UserManagement_audit (UserManagement_id, operation, old_data, changed_by, version)
        VALUES (OLD.id, 'DELETE', row_to_json(OLD), OLD.updated_by, OLD.version);
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO UserManagement_audit (UserManagement_id, operation, old_data, new_data, changed_by, version)
        VALUES (NEW.id, 'UPDATE', row_to_json(OLD), row_to_json(NEW), NEW.updated_by, NEW.version);
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO UserManagement_audit (UserManagement_id, operation, new_data, changed_by, version)
        VALUES (NEW.id, 'INSERT', row_to_json(NEW), NEW.created_by, NEW.version);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create audit trigger
CREATE TRIGGER audit_UserManagement_trigger
    AFTER INSERT OR UPDATE OR DELETE ON UserManagement
    FOR EACH ROW
    EXECUTE FUNCTION audit_UserManagement();