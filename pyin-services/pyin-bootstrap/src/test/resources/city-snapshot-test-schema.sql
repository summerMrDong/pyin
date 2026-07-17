-- H2 compatible schema for city-snapshot plugin tests
-- Table names match @TableName annotations in entity classes

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_no VARCHAR(64),
    user_type VARCHAR(20),
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    avatar_url VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_no VARCHAR(64) NOT NULL,
    citizen_user_id BIGINT,
    issue_type_id BIGINT,
    region_id BIGINT,
    issue_title VARCHAR(200),
    issue_desc TEXT,
    location_text VARCHAR(500),
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    current_status VARCHAR(32) NOT NULL,
    current_department_id BIGINT,
    assigned_user_id BIGINT,
    dispatch_remark VARCHAR(500),
    dispatch_deadline TIMESTAMP,
    dispatch_at TIMESTAMP,
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    rejected_at TIMESTAMP,
    returned_at TIMESTAMP,
    evaluated_at TIMESTAMP,
    result_desc TEXT,
    reject_type VARCHAR(32),
    reject_reason VARCHAR(500),
    return_reason VARCHAR(500),
    evaluation_status VARCHAR(32),
    overdue_flag INT DEFAULT 0,
    latest_flow_id BIGINT,
    report_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_issue_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT,
    attachment_type VARCHAR(20),
    file_name VARCHAR(255),
    file_url VARCHAR(500),
    file_size BIGINT,
    content_type VARCHAR(100),
    sort_order INT DEFAULT 0,
    uploaded_by BIGINT,
    uploaded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_issue_flow_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    flow_action VARCHAR(32) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operator_user_id BIGINT,
    operator_role_type VARCHAR(20),
    from_department_id BIGINT,
    to_department_id BIGINT,
    action_remark VARCHAR(500),
    action_result VARCHAR(50),
    deadline_at TIMESTAMP,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_issue_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    citizen_user_id BIGINT,
    rating_level VARCHAR(20),
    rating_score INT,
    rating_remark VARCHAR(500),
    evaluated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_message_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_no VARCHAR(64),
    target_user_id BIGINT,
    target_user_type VARCHAR(20),
    issue_id BIGINT,
    notice_type VARCHAR(32),
    notice_title VARCHAR(200),
    notice_content TEXT,
    read_flag INT DEFAULT 0,
    read_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_issue_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(64),
    type_name VARCHAR(64),
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_region (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_code VARCHAR(64),
    region_name VARCHAR(64),
    parent_id BIGINT,
    region_level INT,
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_code VARCHAR(64),
    dept_name VARCHAR(128),
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_issue_stat_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date VARCHAR(20),
    stat_type VARCHAR(32),
    stat_dimension_id BIGINT,
    stat_dimension_name VARCHAR(128),
    total_count INT DEFAULT 0,
    pending_count INT DEFAULT 0,
    processing_count INT DEFAULT 0,
    completed_count INT DEFAULT 0,
    rejected_count INT DEFAULT 0,
    returned_count INT DEFAULT 0,
    overdue_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_base_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_group VARCHAR(64),
    config_key VARCHAR(128),
    config_value TEXT,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    role_code VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted INT NOT NULL DEFAULT 0
);
