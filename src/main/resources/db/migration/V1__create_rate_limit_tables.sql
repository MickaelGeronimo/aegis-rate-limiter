CREATE TABLE rate_limit_policies (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    route_pattern VARCHAR(255) NOT NULL,
    algorithm VARCHAR(40) NOT NULL,
    capacity BIGINT NOT NULL,
    refill_tokens BIGINT NOT NULL,
    window_seconds BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_rate_limit_policy_tenant_route UNIQUE (tenant_id, route_pattern)
);

CREATE TABLE rate_limit_audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    policy_id VARCHAR(64),
    tenant_id VARCHAR(120) NOT NULL,
    route_path VARCHAR(255) NOT NULL,
    client_key VARCHAR(160) NOT NULL,
    algorithm VARCHAR(40) NOT NULL,
    permits_requested BIGINT NOT NULL,
    allowed BOOLEAN NOT NULL,
    remaining BIGINT NOT NULL,
    retry_after_seconds BIGINT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_rate_limit_audit_tenant_time
    ON rate_limit_audit_logs (tenant_id, occurred_at DESC);
