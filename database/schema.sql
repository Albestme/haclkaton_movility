-- Eliminar tablas si existen (con orden correcto por dependencias)
DROP TABLE IF EXISTS REPORT CASCADE;
DROP TABLE IF EXISTS VISIT CASCADE;
DROP TABLE IF EXISTS INCIDENCE CASCADE;
DROP TABLE IF EXISTS CONTRACT CASCADE;
DROP TABLE IF EXISTS CHARGER CASCADE;
DROP TABLE IF EXISTS TECHNICIAN CASCADE;

-- Crear tablas
CREATE TABLE TECHNICIAN (
    technician_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    zone VARCHAR(100),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8)
);

CREATE TABLE CHARGER (
    charger_id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    serial_number VARCHAR(100) UNIQUE,
    brand VARCHAR(50),
    model VARCHAR(50),
    number_of_plugs INTEGER,
    connector_types VARCHAR(100),
    has_rfid BOOLEAN,
    environment VARCHAR(20),
    power_type VARCHAR(20),
    phase_type VARCHAR(20),
    max_power_kw DECIMAL(10,2),
    ocpp_version VARCHAR(20),
    telecom_provider VARCHAR(50),
    installation_date DATE,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    active BOOLEAN
);

CREATE TABLE CONTRACT (
    contract_id SERIAL PRIMARY KEY,
    type VARCHAR(50),
    client_id INTEGER,
    charger_id INTEGER REFERENCES CHARGER(charger_id),
    domain_id INTEGER,
    start_date DATE,
    end_date DATE,
    number_of_visits INTEGER,
    frequency VARCHAR(50),
    status VARCHAR(50)
);

CREATE TABLE INCIDENCE (
    incidence_id SERIAL PRIMARY KEY,
    charger_id INTEGER REFERENCES CHARGER(charger_id),
    reported_at TIMESTAMP,
    priority VARCHAR(50),
    status VARCHAR(50),
    description TEXT,
    estimated_duration_min INTEGER,
    final_duration_min INTEGER,
    resolved_at TIMESTAMP
);

CREATE TABLE VISIT (
    visit_id SERIAL PRIMARY KEY,
    contract_id INTEGER REFERENCES CONTRACT(contract_id),
    incidence_id INTEGER REFERENCES INCIDENCE(incidence_id),
    priority VARCHAR(50),
    technician_id INTEGER REFERENCES TECHNICIAN(technician_id),
    status VARCHAR(50),
    planned_date DATE,
    address TEXT,
    postal_code VARCHAR(10),
    location_source VARCHAR(50),
    estimated_duration_min INTEGER
);

CREATE TABLE REPORT (
    report_id SERIAL PRIMARY KEY,
    visit_id INTEGER REFERENCES VISIT(visit_id),
    report_type VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP
);