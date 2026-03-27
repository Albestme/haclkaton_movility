-- ======================================================================
-- GENERADOR DE MOCK DATA PER A HACKATÓ (POSTGRESQL)
-- ======================================================================

-- 1. Crear 20 Tècnics
INSERT INTO TECHNICIAN (name, zone, latitude, longitude)
SELECT 
    'Tècnic Hackato ' || i,
    (ARRAY['Barcelona Centre', 'Tarragona Sud', 'Girona i Nord', 'Lleida i Ponent', 'Àrea Metropolitana'])[floor(random() * 5 + 1)],
    41.0 + random() * 1.5,
    0.5 + random() * 2.5
FROM generate_series(1, 20) AS i;

-- 2. Crear 100 Carregadors (Amb els teus paràmetres)
INSERT INTO CHARGER (name, serial_number, brand, model, number_of_plugs, connector_types, has_rfid, environment, power_type, phase_type, max_power_kw, ocpp_version, telecom_provider, installation_date, latitude, longitude, active)
SELECT 
    'Charger HCK-' || i,
    'SN-' || LPAD(i::text, 5, '0'),
    (ARRAY['Circutor', 'Wallbox', 'Kempower'])[floor(random() * 3 + 1)],
    (ARRAY['Bàsic', 'Avançat', 'Ultra-ràpid'])[floor(random() * 3 + 1)],
    floor(random() * 5 + 1)::int,
    (ARRAY['Tipus 2', 'CCS2', 'CHAdeMO'])[floor(random() * 3 + 1)],
    random() > 0.5,
    (ARRAY['EdRSR', 'EdRR', 'EdRUR'])[floor(random() * 3 + 1)],
    (ARRAY['AC', 'DC', 'Mixta'])[floor(random() * 3 + 1)],
    (ARRAY['Monofásico', 'Trifásico', 'No aplica/DC'])[floor(random() * 3 + 1)],
    (ARRAY[3.7, 7.4, 11, 22, 50, 150, 350, 400])[floor(random() * 8 + 1)],
    (ARRAY['OCPP 1.5', 'OCPP 1.6', 'OCPP 2.0.1'])[floor(random() * 3 + 1)],
    (ARRAY['Movistar', 'Vodafone', 'Orange'])[floor(random() * 3 + 1)],
    DATE '2015-01-01' + (random() * (current_date - DATE '2015-01-01'))::int,
    41.0 + random() * 1.5,
    0.5 + random() * 2.5,
    random() > 0.1 -- 90% dels carregadors actius
FROM generate_series(1, 100) AS i;

-- 3. Crear 100 Contractes (Un per carregador)
INSERT INTO CONTRACT (type, client_id, charger_id, domain_id, start_date, end_date, number_of_visits, frequency, status)
SELECT 
    (ARRAY['Manteniment Integral', 'SLA Premium', 'Bàsic'])[floor(random() * 3 + 1)],
    floor(random() * 50 + 100)::int,
    i,
    floor(random() * 5 + 1)::int * 10,
    DATE '2020-01-01' + (random() * 1000)::int,
    DATE '2025-01-01' + (random() * 1000)::int,
    floor(random() * 4 + 1)::int,
    (ARRAY['Mensual', 'Trimestral', 'Semestral', 'Anual'])[floor(random() * 4 + 1)],
    'Actiu'
FROM generate_series(1, 100) AS i;

-- 4. Crear 500 Incidències per entrenar l'XGBoost (AMB LÒGICA INCLOSA)
INSERT INTO INCIDENCE (charger_id, reported_at, priority, status, description, estimated_duration_min, final_duration_min)
SELECT 
    c.charger_id,
    TIMESTAMP '2024-01-01 00:00:00' + (random() * (current_timestamp - TIMESTAMP '2024-01-01 00:00:00')),
    p.priority,
    'Tancada',
    'Error generat per a entrenament',
    floor(random() * 120 + 30)::int,
    -- LÒGICA: Aquí definim el que el model ha d'aprendre
    CASE 
        WHEN p.priority = 'Correctiu crític' THEN 180 + (random() * 60)
        WHEN p.priority = 'Correctiu no crític' THEN 90 + (random() * 45)
        WHEN p.priority = 'Posada en marxa' THEN 120 + (random() * 30)
        WHEN p.priority = 'Visita de diagnosi' THEN 45 + (random() * 20)
        ELSE 30 + (random() * 15)
    END + (CASE WHEN c.model = 'Ultra-ràpid' THEN 40 ELSE 0 END)
FROM generate_series(1, 500) AS i
CROSS JOIN LATERAL (
    SELECT charger_id, model FROM CHARGER ORDER BY random() LIMIT 1
) AS c
CROSS JOIN LATERAL (
    SELECT (ARRAY['Correctiu crític', 'Correctiu no crític', 'Manteniment preventiu programat', 'Posada en marxa', 'Visita de diagnosi'])[floor(random() * 5 + 1)] AS priority
) AS p;

-- Actualitzem la data de resolució perquè tingui sentit
UPDATE INCIDENCE SET resolved_at = reported_at + (final_duration_min || ' minutes')::interval;

-- 5. Crear les visites (Ara sí que trobarà incidències)
INSERT INTO VISIT (contract_id, incidence_id, technician_id, visit_type, status, planned_date, address, postal_code, location_source, estimated_duration_min)
SELECT 
    (SELECT contract_id FROM CONTRACT WHERE charger_id = I.charger_id LIMIT 1),
    I.incidence_id,
    floor(random() * 20 + 1)::int,
    'Correctiva',
    'Completada',
    I.reported_at::date,
    'Adreça Hackato ' || I.incidence_id,
    '08001',
    'GPS',
    I.estimated_duration_min
FROM INCIDENCE I;