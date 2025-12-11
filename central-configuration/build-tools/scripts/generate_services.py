#!/usr/bin/env python3
import os
import re
from datetime import datetime

base_path = r"C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services"

# Service configurations with specific entity requirements
services = {
    "SecretsManagementService": {
        "port": 8890,
        "package": "com.gogidix.foundation.security",
        "entities": [
            {
                "name": "Secret",
                "fields": [
                    {"name": "id", "type": "Long", "annotations": ["@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)"]},
                    {"name": "secretKey", "type": "String", "annotations": ["@NotBlank", "@Column(nullable = false, unique = true, length = 255)"]},
                    {"name": "encryptedValue", "type": "String", "annotations": ["@Lob", "@Column(nullable = false)"]},
                    {"name": "secretType", "type": "SecretType", "annotations": ["@Enumerated(EnumType.STRING)", "@Column(nullable = false)"]},
                    {"name": "environment", "type": "Environment", "annotations": ["@Enumerated(EnumType.STRING)", "@Column(nullable = false)"]},
                    {"name": "applicationName", "type": "String", "annotations": ["@Column(length = 255)"]},
                    {"name": "description", "type": "String", "annotations": ["@Column(length = 1000)"]},
                    {"name": "version", "type": "Integer", "annotations": ["@Column(nullable = false)"]},
                    {"name": "active", "type": "Boolean", "annotations": ["@Column(nullable = false)"]},
                    {"name": "createdBy", "type": "String", "annotations": ["@Column(length = 100)"]},
                    {"name": "accessCount", "type": "Long", "annotations": ["@Column(nullable = false)"]},
                    {"name": "lastAccessedAt", "type": "LocalDateTime", "annotations": []},
                    {"name": "expiresAt", "type": "LocalDateTime", "annotations": []},
                    {"name": "createdAt", "type": "LocalDateTime", "annotations": ["@CreationTimestamp", "@Column(nullable = false, updatable = false)"]},
                    {"name": "updatedAt", "type": "LocalDateTime", "annotations": ["@UpdateTimestamp", "@Column(nullable = false)"]}
                ]
            },
            {
                "name": "SecretAccessLog",
                "fields": [
                    {"name": "id", "type": "Long", "annotations": ["@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)"]},
                    {"name": "secretId", "type": "Long", "annotations": ["@Column(nullable = false)"]},
                    {"name": "secretKey", "type": "String", "annotations": ["@Column(nullable = false, length = 255)"]},
                    {"name": "accessedBy", "type": "String", "annotations": ["@Column(nullable = false, length = 100)"]},
                    {"name": "accessType", "type": "String", "annotations": ["@Column(length = 50)"]},
                    {"name": "ipAddress", "type": "String", "annotations": ["@Column(length = 45)"]},
                    {"name": "userAgent", "type": "String", "annotations": ["@Column(length = 500)"]},
                    {"name": "accessGranted", "type": "Boolean", "annotations": ["@Column(nullable = false)"]},
                    {"name": "accessReason", "type": "String", "annotations": ["@Column(length = 500)"]},
                    {"name": "createdAt", "type": "LocalDateTime", "annotations": ["@CreationTimestamp", "@Column(nullable = false)"]}
                ]
            }
        ],
        "enums": [
            {
                "name": "SecretType",
                "values": ["API_KEY", "DATABASE_PASSWORD", "CERTIFICATE", "TOKEN", "SSH_KEY", "ENCRYPTION_KEY", "OTHER"]
            }
        ]
    },
    "SecretsRotationService": {
        "port": 8891,
        "package": "com.gogidix.foundation.security",
        "entities": [
            {
                "name": "RotationPolicy",
                "fields": [
                    {"name": "id", "type": "Long", "annotations": ["@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)"]},
                    {"name": "policyName", "type": "String", "annotations": ["@NotBlank", "@Column(nullable = false, unique = true, length = 255)"]},
                    {"name": "secretKeyPattern", "type": "String", "annotations": ["@NotBlank", "@Column(nullable = false, length = 255)"]},
                    {"name": "rotationFrequency", "type": "Integer", "annotations": ["@Column(nullable = false)"]},
                    {"name": "rotationUnit", "type": "RotationUnit", "annotations": ["@Enumerated(EnumType.STRING)", "@Column(nullable = false)"]},
                    {"name": "autoRotate", "type": "Boolean", "annotations": ["@Column(nullable = false)"]},
                    {"name": "notificationBeforeDays", "type": "Integer", "annotations": ["@Column(nullable = false)"]},
                    {"name": "active", "type": "Boolean", "annotations": ["@Column(nullable = false)"]},
                    {"name": "lastRotationAt", "type": "LocalDateTime", "annotations": []},
                    {"name": "nextRotationAt", "type": "LocalDateTime", "annotations": []},
                    {"name": "createdBy", "type": "String", "annotations": ["@Column(length = 100)"]},
                    {"name": "createdAt", "type": "LocalDateTime", "annotations": ["@CreationTimestamp", "@Column(nullable = false)"]},
                    {"name": "updatedAt", "type": "LocalDateTime", "annotations": ["@UpdateTimestamp", "@Column(nullable = false)"]}
                ]
            },
            {
                "name": "RotationHistory",
                "fields": [
                    {"name": "id", "type": "Long", "annotations": ["@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)"]},
                    {"name": "secretId", "type": "Long", "annotations": ["@Column(nullable = false)"]},
                    {"name": "policyId", "type": "Long", "annotations": ["@Column(nullable = false)"]},
                    {"name": "rotationStatus", "type": "RotationStatus", "annotations": ["@Enumerated(EnumType.STRING)", "@Column(nullable = false)"]},
                    {"name": "oldValueHash", "type": "String", "annotations": ["@Column(nullable = false, length = 255)"]},
                    {"name": "newValueHash", "type": "String", "annotations": ["@Column(nullable = false, length = 255)"]},
                    {"name": "rotatedBy", "type": "String", "annotations": ["@Column(length = 100)"]},
                    {"name": "rotationMethod", "type": "String", "annotations": ["@Column(length = 100)"]},
                    {"name": "errorMessage", "type": "String", "annotations": ["@Column(length = 1000)"]},
                    {"name": "createdAt", "type": "LocalDateTime", "annotations": ["@CreationTimestamp", "@Column(nullable = false)"]}
                ]
            }
        ],
        "enums": [
            {
                "name": "RotationUnit",
                "values": ["DAYS", "WEEKS", "MONTHS", "YEARS"]
            },
            {
                "name": "RotationStatus",
                "values": ["SUCCESS", "FAILED", "PENDING", "SKIPPED"]
            }
        ]
    },
    "FeatureFlagsService": {
        "port": 8892,
        "package": "com.gogidix.foundation.config",
        "entities": [
            {
                "name": "FeatureFlag",
                "fields": [
                    {"name": "id", "type": "Long", "annotations": ["@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)"]},
                    {"name": "featureKey", "type": "String", "annotations": ["@NotBlank", "@Column(nullable = false, unique = true, length = 255)"]},
                    {"name": "enabled", "type": "Boolean", "annotations": ["@Column(nullable = false)"]},
                    {"name": "rolloutPercentage", "type": "Integer", "annotations": ["@Column(nullable = false)"]},
                    {"name": "targetUsers", "type": "String", "annotations": ["@Lob"]},
                    {"name": "targetSegments", "type": "String", "annotations": ["@Lob"]},
                    {"name": "environment", "type": "String", "annotations": ["@Column(nullable = false, length = 50)"]},
                    {"name": "description", "type": "String", "annotations": ["@Column(length = 1000)"]},
                    {"name": "tags", "type": "String", "annotations": ["@Column(length = 500)"]},
                    {"name": "activeFrom", "type": "LocalDateTime", "annotations": []},
                    {"name": "activeTo", "type": "LocalDateTime", "annotations": []},
                    {"name": "createdBy", "type": "String", "annotations": ["@Column(length = 100)"]},
                    {"name": "createdAt", "type": "LocalDateTime", "annotations': ["@CreationTimestamp", "@Column(nullable = false)"]},
                    {"name": "updatedAt", "type": "LocalDateTime", "annotations": ["@UpdateTimestamp", "@Column(nullable = false)"]}
                ]
            },
            {
                "name": "FeatureFlagEvaluation",
                "fields": [
                    {"name": "id", "type": "Long", "annotations": ["@Id", "@GeneratedValue(strategy = GenerationType.IDENTITY)"]},
                    {"name": "flagId", "type": "Long", "annotations": ["@Column(nullable = false)"]},
                    {"name": "userId", "type": "String", "annotations": ["@Column(length = 255)"]},
                    {"name": "sessionId", "type": "String", "annotations": ["@Column(length = 255)"]},
                    {"name": "context", "type": "String", "annotations": ["@Lob"]},
                    {"name": "result", "type": "Boolean", "annotations": ["@Column(nullable = false)"]},
                    {"name": "variation", "type": "String", "annotations": ["@Column(length = 100)"]},
                    {"name": "evaluationTime", "type": "LocalDateTime", "annotations": ["@CreationTimestamp", "@Column(nullable = false)"]}
                ]
            }
        ]
    }
}

def generate_entity(entity, package_name):
    """Generate Java entity code"""
    code = f"package {package_name}.entity;\n\n"

    # Add imports
    imports = [
        "jakarta.persistence.*",
        "jakarta.validation.constraints.NotBlank",
        "org.hibernate.annotations.CreationTimestamp",
        "org.hibernate.annotations.UpdateTimestamp",
        "java.time.LocalDateTime"
    ]

    for imp in sorted(set(imports)):
        code += f"import {imp};\n"

    if any(f["type"] in ["SecretType", "RotationUnit", "RotationStatus"] for f in entity["fields"]):
        code += f"import {package_name}.enums.*;\n"

    code += "\n"

    # Class declaration
    code += f"@Entity\n"
    code += f"@Table(name = \"{entity['name'].lower()}\")\n"
    code += f"public class {entity['name']} {{\n\n"

    # Fields
    for field in entity["fields"]:
        for annotation in field["annotations"]:
            code += f"    {annotation}\n"
        code += f"    private {field['type']} {field['name']};\n\n"

    # Constructors
    code += "    public " + entity['name'] + "() {}\n\n"

    # Getters and setters
    for field in entity["fields"]:
        # Getter
        field_name_cap = field["name"][0].upper() + field["name"][1:] if field["name"] else field["name"]
        code += f"    public {field['type']} get{field_name_cap}() {{\n"
        code += f"        return {field['name']};\n"
        code += f"    }}\n\n"

        # Setter
        code += f"    public void set{field_name_cap}({field['type']} {field['name']}) {{\n"
        code += f"        this.{field['name']} = {field['name']};\n"
        code += f"    }}\n\n"

    code += "}\n"
    return code

def generate_enum(enum_def, package_name):
    """Generate Java enum code"""
    code = f"package {package_name}.enums;\n\n"

    code += f"public enum {enum_def['name']} {{\n"

    for i, value in enumerate(enum_def["values"]):
        code += f"    {value}(\"{value.lower().replace('_', '-')}\")"
        if i < len(enum_def["values"]) - 1:
            code += ","
        code += "\n"

    code += ";\n\n"
    code += "    private final String value;\n\n"

    code += f"    {enum_def['name']}(String value) {{\n"
    code += "        this.value = value;\n"
    code += "    }\n\n"

    code += "    public String getValue() {\n"
    code += "        return value;\n"
    code += "    }\n\n"

    code += "    public static " + enum_def['name'] + " fromValue(String value) {\n"
    code += f"        for ({enum_def['name']} type : {enum_def['name']}.values()) {{\n"
    code += "            if (type.value.equalsIgnoreCase(value)) {\n"
    code += "                return type;\n"
    code += "            }\n"
    code += "        }\n"
    code += "        throw new IllegalArgumentException(\"Unknown \" + value);\n"
    code += "    }\n"

    code += "}\n"
    return code

def generate_repository(entity_name, package_name):
    """Generate repository interface"""
    code = f"package {package_name}.repository;\n\n"
    code += f"import {package_name}.entity.{entity_name};\n"
    code += "import org.springframework.data.domain.Page;\n"
    code += "import org.springframework.data.domain.Pageable;\n"
    code += "import org.springframework.data.jpa.repository.JpaRepository;\n"
    code += "import org.springframework.data.jpa.repository.Query;\n"
    code += "import org.springframework.data.repository.query.Param;\n"
    code += "import org.springframework.stereotype.Repository;\n\n"

    code += "import java.util.List;\n"
    code += "import java.util.Optional;\n\n"

    code += "@Repository\n"
    code += f"public interface {entity_name}Repository extends JpaRepository<{entity_name}, Long> {{\n\n"

    # Add common methods
    if entity_name == "Secret":
        code += "    Optional<Secret> findBySecretKeyAndEnvironment(String secretKey, String environment);\n"
        code += "    List<Secret> findByApplicationName(String applicationName);\n"
        code += "    List<Secret> findByActiveTrue();\n"
        code += "    @Query(\"SELECT s FROM Secret s WHERE s.expiresAt <= :now AND s.active = true\")\n"
        code += "    List<Secret> findExpiredSecrets(@Param(\"now\") LocalDateTime now);\n"
    elif entity_name == "FeatureFlag":
        code += "    Optional<FeatureFlag> findByFeatureKeyAndEnvironment(String featureKey, String environment);\n"
        code += "    List<FeatureFlag> findByEnabledTrueAndEnvironment(String environment);\n"
        code += "    List<FeatureFlag> findByActiveFromBeforeAndActiveToAfter(LocalDateTime now, LocalDateTime now);\n"

    code += "}\n"
    return code

def generate_service(entity_name, package_name):
    """Generate service class"""
    entity_lower = entity_name.lower()
    code = f"package {package_name}.service;\n\n"
    code += f"import {package_name}.entity.{entity_name};\n"
    code += f"import {package_name}.repository.{entity_name}Repository;\n"
    code += "import org.slf4j.Logger;\n"
    code += "import org.slf4j.LoggerFactory;\n"
    code += "import org.springframework.beans.factory.annotation.Autowired;\n"
    code += "import org.springframework.stereotype.Service;\n"
    code += "import org.springframework.transaction.annotation.Transactional;\n\n"

    code += "import java.util.List;\n"
    code += "import java.util.Optional;\n\n"

    code += "@Service\n"
    code += "@Transactional\n"
    code += f"public class {entity_name}Service {{\n\n"

    code += f"    private static final Logger logger = LoggerFactory.getLogger({entity_name}Service.class);\n\n"

    code += f"    @Autowired\n"
    code += f"    private {entity_name}Repository {entity_lower}Repository;\n\n"

    # CRUD methods
    code += f"    public List<{entity_name}> getAll{entity_name}s() {{\n"
    code += f"        return {entity_lower}Repository.findAll();\n"
    code += "    }\n\n"

    code += f"    public Optional<{entity_name}> get{entity_name}ById(Long id) {{\n"
    code += f"        return {entity_lower}Repository.findById(id);\n"
    code += "    }\n\n"

    code += f"    public {entity_name} create{entity_name}({entity_name} {entity_lower}) {{\n"
    code += f"        {entity_name} saved = {entity_lower}Repository.save({entity_lower});\n"
    code += f"        logger.info(\"Created {entity_lower} with ID: {{}}\", saved.getId());\n"
    code += "        return saved;\n"
    code += "    }\n\n"

    code += f"    public {entity_name} update{entity_name}(Long id, {entity_name} {entity_lower}) {{\n"
    code += f"        {entity_lower}.setId(id);\n"
    code += f"        {entity_name} updated = {entity_lower}Repository.save({entity_lower});\n"
    code += f"        logger.info(\"Updated {entity_lower} with ID: {{}}\", updated.getId());\n"
    code += "        return updated;\n"
    code += "    }\n\n"

    code += f"    public void delete{entity_name}(Long id) {{\n"
    code += f"        {entity_lower}Repository.deleteById(id);\n"
    code += f"        logger.info(\"Deleted {entity_lower} with ID: {{}}\", id);\n"
    code += "    }\n"

    code += "}\n"
    return code

def generate_controller(entity_name, package_name, port, context_path):
    """Generate REST controller"""
    entity_lower = entity_name.lower()
    entity_plural = entity_name + "s"
    context_path_clean = context_path.replace("-", "")

    code = f"package {package_name}.controller;\n\n"
    code += f"import {package_name}.entity.{entity_name};\n"
    code += f"import {package_name}.service.{entity_name}Service;\n"
    code += "import io.swagger.v3.oas.annotations.Operation;\n"
    code += "import io.swagger.v3.oas.annotations.tags.Tag;\n"
    code += "import org.springframework.beans.factory.annotation.Autowired;\n"
    code += "import org.springframework.http.HttpStatus;\n"
    code += "import org.springframework.http.ResponseEntity;\n"
    code += "import org.springframework.security.access.prepost.PreAuthorize;\n"
    code += "import org.springframework.web.bind.annotation.*;\n\n"

    code += "import java.util.List;\n"
    code += "import java.util.Optional;\n\n"

    code += "@RestController\n"
    code += f"@RequestMapping(\"/api/{entity_plural.lower()}\")\n"
    code += f"@Tag(name = \"{entity_name} Management\", description = \"API for managing {entity_lower}\")\n"
    code += f"@PreAuthorize(\"hasRole('{context_path_clean.upper()}_ADMIN') or hasRole('{context_path_clean.upper()}_USER')\")\n"
    code += f"public class {entity_name}Controller {{\n\n"

    code += f"    @Autowired\n"
    code += f"    private {entity_name}Service {entity_lower}Service;\n\n"

    # Endpoints
    code += f"    @Operation(summary = \"Get all {entity_plural.lower()}\")\n"
    code += f"    @GetMapping\n"
    code += f"    public ResponseEntity<List<{entity_name}>> getAll{entity_plural}() {{\n"
    code += f"        List<{entity_name}> {entity_plural} = {entity_lower}Service.getAll{entity_plural}();\n"
    code += "        return ResponseEntity.ok({entity_plural});\n"
    code += "    }\n\n"

    code += f"    @Operation(summary = \"Get {entity_lower} by ID\")\n"
    code += f"    @GetMapping(\"/{{id}}\")\n"
    code += f"    public ResponseEntity<{entity_name}> get{entity_name}ById(@PathVariable Long id) {{\n"
    code += f"        Optional<{entity_name}> {entity_lower} = {entity_lower}Service.get{entity_name}ById(id);\n"
    code += "        return {entity_lower}.map(ResponseEntity::ok)\n"
    code += "                              .orElse(ResponseEntity.notFound().build());\n"
    code += "    }\n\n"

    code += f"    @Operation(summary = \"Create {entity_lower}\")\n"
    code += f"    @PostMapping\n"
    code += f"    @PreAuthorize(\"hasRole('{context_path_clean.upper()}_ADMIN')\")\n"
    code += f"    public ResponseEntity<{entity_name}> create{entity_name}(@RequestBody {entity_name} {entity_lower}) {{\n"
    code += f"        {entity_name} created = {entity_lower}Service.create{entity_name}({entity_lower});\n"
    code += "        return ResponseEntity.status(HttpStatus.CREATED).body(created);\n"
    code += "    }\n\n"

    code += f"    @Operation(summary = \"Update {entity_lower}\")\n"
    code += f"    @PutMapping(\"/{{id}}\")\n"
    code += f"    @PreAuthorize(\"hasRole('{context_path_clean.upper()}_ADMIN')\")\n"
    code += f"    public ResponseEntity<{entity_name}> update{entity_name}(@PathVariable Long id, @RequestBody {entity_name} {entity_lower}) {{\n"
    code += f"        {entity_name} updated = {entity_lower}Service.update{entity_name}(id, {entity_lower});\n"
    code += "        return ResponseEntity.ok(updated);\n"
    code += "    }\n\n"

    code += f"    @Operation(summary = \"Delete {entity_lower}\")\n"
    code += f"    @DeleteMapping(\"/{{id}}\")\n"
    code += f"    @PreAuthorize(\"hasRole('{context_path_clean.upper()}_ADMIN')\")\n"
    code += f"    public ResponseEntity<Void> delete{entity_name}(@PathVariable Long id) {{\n"
    code += f"        {entity_lower}Service.delete{entity_name}(id);\n"
    code += "        return ResponseEntity.noContent().build();\n"
    code += "    }\n"

    code += "}\n"
    return code

def generate_database_migration(entity_name, service_config):
    """Generate Flyway migration SQL"""
    table_name = entity_name.lower()
    sql = f"-- Create {table_name} table\n"
    sql += f"CREATE TABLE {table_name} (\n"
    sql += "    id BIGSERIAL PRIMARY KEY,\n"

    # Add columns based on entity
    if entity_name == "Secret":
        sql += "    secret_key VARCHAR(255) NOT NULL UNIQUE,\n"
        sql += "    encrypted_value TEXT NOT NULL,\n"
        sql += "    secret_type VARCHAR(50) NOT NULL,\n"
        sql += "    environment VARCHAR(50) NOT NULL,\n"
        sql += "    application_name VARCHAR(255),\n"
        sql += "    description VARCHAR(1000),\n"
        sql += "    version INTEGER NOT NULL DEFAULT 1,\n"
        sql += "    active BOOLEAN NOT NULL DEFAULT true,\n"
        sql += "    created_by VARCHAR(100),\n"
        sql += "    access_count BIGINT NOT NULL DEFAULT 0,\n"
        sql += "    last_accessed_at TIMESTAMP,\n"
        sql += "    expires_at TIMESTAMP,\n"
    elif entity_name == "FeatureFlag":
        sql += "    feature_key VARCHAR(255) NOT NULL UNIQUE,\n"
        sql += "    enabled BOOLEAN NOT NULL DEFAULT false,\n"
        sql += "    rollout_percentage INTEGER NOT NULL DEFAULT 0,\n"
        sql += "    target_users TEXT,\n"
        sql += "    target_segments TEXT,\n"
        sql += "    environment VARCHAR(50) NOT NULL,\n"
        sql += "    description VARCHAR(1000),\n"
        sql += "    tags VARCHAR(500),\n"
        sql += "    active_from TIMESTAMP,\n"
        sql += "    active_to TIMESTAMP,\n"

    sql += "    created_by VARCHAR(100),\n"
    sql += "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n"
    sql += "    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n"
    sql += ");\n\n"

    # Add indexes
    if entity_name == "Secret":
        sql += "CREATE INDEX idx_secret_key_env ON {table_name}(secret_key, environment);\n"
        sql += "CREATE INDEX idx_secret_app ON {table_name}(application_name);\n"
        sql += "CREATE INDEX idx_secret_active ON {table_name}(active);\n"
        sql += "CREATE INDEX idx_secret_expires ON {table_name}(expires_at);\n"
    elif entity_name == "FeatureFlag":
        sql += "CREATE INDEX idx_flag_key_env ON {table_name}(feature_key, environment);\n"
        sql += "CREATE INDEX idx_flag_enabled ON {table_name}(enabled);\n"
        sql += "CREATE INDEX idx_flag_env ON {table_name}(environment);\n"

    # Add comments
    sql += f"\nCOMMENT ON TABLE {table_name} IS 'Stores {entity_name.lower()} data with audit trail';\n"

    return sql

def main():
    print("=== Generating Foundation Services ===\n")

    for service_name, config in services.items():
        print(f"Generating {service_name}...")

        service_path = os.path.join(base_path, service_name, service_name)

        # Create directories
        for subdir in ["controller", "entity", "repository", "service", "enums"]:
            dir_path = os.path.join(service_path, "src", "main", "java", *config["package"].split("."), subdir)
            os.makedirs(dir_path, exist_ok=True)

        migration_dir = os.path.join(service_path, "src", "main", "resources", "db", "migration")
        os.makedirs(migration_dir, exist_ok=True)

        # Generate enums
        if "enums" in config:
            for enum_def in config["enums"]:
                enum_code = generate_enum(enum_def, config["package"])
                enum_file = os.path.join(service_path, "src", "main", "java", *config["package"].split("."), "enums", f"{enum_def['name']}.java")
                with open(enum_file, 'w') as f:
                    f.write(enum_code)

        # Generate entities, repositories, services, and controllers
        for entity in config["entities"]:
            # Entity
            entity_code = generate_entity(entity, config["package"])
            entity_file = os.path.join(service_path, "src", "main", "java", *config["package"].split("."), "entity", f"{entity['name']}.java")
            with open(entity_file, 'w') as f:
                f.write(entity_code)

            # Repository
            repo_code = generate_repository(entity['name'], config["package"])
            repo_file = os.path.join(service_path, "src", "main", "java", *config["package"].split("."), "repository", f"{entity['name']}Repository.java")
            with open(repo_file, 'w') as f:
                f.write(repo_code)

            # Service
            service_code = generate_service(entity['name'], config["package"])
            service_file = os.path.join(service_path, "src", "main", "java", *config["package"].split("."), "service", f"{entity['name']}Service.java")
            with open(service_file, 'w') as f:
                f.write(service_code)

            # Controller
            controller_code = generate_controller(entity['name'], config["package"], config["port"], service_name.replace("Service", "").lower())
            controller_file = os.path.join(service_path, "src", "main", "java", *config["package"].split("."), "controller", f"{entity['name']}Controller.java")
            with open(controller_file, 'w') as f:
                f.write(controller_code)

            # Database migration
            migration_code = generate_database_migration(entity['name'], config)
            migration_file = os.path.join(migration_dir, f"V1__Create_{entity['name']}_Table.sql")
            with open(migration_file, 'w') as f:
                f.write(migration_code)

        print(f"  [OK] {service_name} generated")

    print("\n=== All services generated successfully! ===")
    print("\nNext steps:")
    print("1. Add required dependencies to pom.xml files")
    print("2. Implement custom business logic as needed")
    print("3. Add unit and integration tests")
    print("4. Configure security settings")
    print("5. Update application.yml files")
    print("6. Run Maven compile and test")

if __name__ == "__main__":
    main()