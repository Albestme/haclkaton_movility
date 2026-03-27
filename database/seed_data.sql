-- ======================================================================
-- NETEJA PRÈVIA DE DADES (Per evitar duplicats)
-- ======================================================================
TRUNCATE TABLE VISIT CASCADE;
TRUNCATE TABLE INCIDENCE CASCADE;
TRUNCATE TABLE CONTRACT CASCADE;
TRUNCATE TABLE CHARGER CASCADE;
TRUNCATE TABLE TECHNICIAN CASCADE;

-- ======================================================================
-- GENERADOR DE MOCK DATA PER A HACKATÓ (POSTGRESQL) - CORREGIT
-- ======================================================================

-- 1. Crear 20 Tècnics
INSERT INTO TECHNICIAN (name, zone, latitude, longitude)
SELECT 
    'Tècnic Hackato ' || i,
    (ARRAY['Barcelona Centre', 'Tarragona Sud', 'Girona i Nord', 'Lleida i Ponent', 'Àrea Metropolitana'])[floor(random() * 5 + 1)::int],
    41.0 + random() * 1.5,
    0.5 + random() * 2.5
FROM generate_series(1, 20) AS i;

-- 2. Crear 100 Carregadors (Amb els teus paràmetres exactes)
INSERT INTO CHARGER (name, serial_number, brand, model, number_of_plugs, connector_types, has_rfid, environment, power_type, phase_type, max_power_kw, ocpp_version, telecom_provider, installation_date, latitude, longitude, active)
SELECT 
    'Charger HCK-' || i,
    'SN-' || LPAD(i::text, 5, '0'),
    (ARRAY['Circutor', 'Wallbox', 'Kempower'])[floor(random() * 3 + 1)::int],
    (ARRAY['Bàsic', 'Avançat', 'Ultra-ràpid'])[floor(random() * 3 + 1)::int],
    floor(random() * 5 + 1)::int,
    (ARRAY['Tipus 2', 'CCS2', 'CHAdeMO'])[floor(random() * 3 + 1)::int],
    random() > 0.5,
    (ARRAY['EdRSR', 'EdRR', 'EdRUR'])[floor(random() * 3 + 1)::int],
    (ARRAY['AC', 'DC', 'Mixta'])[floor(random() * 3 + 1)::int],
    (ARRAY['Monofásico', 'Trifásico', 'No aplica/DC'])[floor(random() * 3 + 1)::int],
    (ARRAY[3.7, 7.4, 11, 22, 50, 150, 350, 400])[floor(random() * 8 + 1)::int],
    (ARRAY['OCPP 1.5', 'OCPP 1.6', 'OCPP 2.0.1'])[floor(random() * 3 + 1)::int],
    (ARRAY['Movistar', 'Vodafone', 'Orange'])[floor(random() * 3 + 1)::int],
    DATE '2015-01-01' + (random() * (current_date - DATE '2015-01-01'))::int,
    41.0 + random() * 1.5,
    0.5 + random() * 2.5,
    random() > 0.1 -- 90% dels carregadors actius
FROM generate_series(1, 100) AS i;

-- 3. Crear 100 Contractes (Un per carregador)
INSERT INTO CONTRACT (type, client_id, charger_id, domain_id, start_date, end_date, number_of_visits, frequency, status)
SELECT 
    (ARRAY['Manteniment Integral', 'SLA Premium', 'Bàsic'])[floor(random() * 3 + 1)::int],
    floor(random() * 50 + 100)::int,
    i,
    floor(random() * 5 + 1)::int * 10,
    DATE '2020-01-01' + (random() * 1000)::int,
    DATE '2025-01-01' + (random() * 1000)::int,
    floor(random() * 4 + 1)::int,
    (ARRAY['Mensual', 'Trimestral', 'Semestral', 'Anual'])[floor(random() * 4 + 1)::int],
    'Actiu'
FROM generate_series(1, 100) AS i;

WITH RankedChargers AS (
    SELECT charger_id, model, row_number() OVER (ORDER BY charger_id) as rn
    FROM CHARGER
),
GeneratedIncidences AS (
    SELECT 
        i,
        (ARRAY['Correctiu crític', 'Correctiu no crític', 'Manteniment preventiu programat', 'Posada en marxa', 'Visita de diagnosi'])[floor(random() * 5 + 1)::int] AS priority,
        (ARRAY['Tancada', 'Tancada', 'Tancada', 'En curs', 'Pendent', 'Pendent'])[floor(random() * 6 + 1)::int] AS status,
        floor(random() * (SELECT count(*) FROM CHARGER) + 1)::int AS charger_rn
    FROM generate_series(1, 500) AS i
)
INSERT INTO INCIDENCE (charger_id, reported_at, priority, status, description, estimated_duration_min, final_duration_min)
SELECT 
    c.charger_id,
    TIMESTAMP '2024-01-01 00:00:00' + (random() * (current_timestamp - TIMESTAMP '2024-01-01 00:00:00')),
    g.priority,
    (ARRAY['Tancada', 'Pendent', 'En curs'])[floor(random() * 3 + 1)::int] AS status,
    (ARRAY[
        'La pantalla està completament en negre i no respon als tocs.',
        'El client reporta que el cable s''ha quedat bloquejat al vehicle.',
        'Pèrdua de connexió amb el servidor central (OCPP timeout).',
        'Salt de les proteccions elèctriques (diferencial) en iniciar la sessió.',
        'El lector RFID no emet cap so ni reconeix les targetes dels usuaris.',
        'Carcassa exterior danyada per impacte d''un vehicle.',
        'Error intern de l''inversor de potència reportat per telemetria.',
        'Revisió periòdica i preventiva segons l''acord de nivell de servei (SLA).',
        'La potència de càrrega està limitada i no passa de 11kW.',
        'S''ha detectat un error en el sistema de ventilació/refrigeració.',
        'Punt de recàrrega fora de línia des de fa més de 24 hores.',
        'El connector Tipus 2 presenta signes de cremades o desgast extrem.'
    ])[floor(random() * 12 + 1)::int],
    floor(random() * 120 + 30)::int,
    -- LÒGICA: Aquí definim el que el model ha d'aprendre
    CASE 
        WHEN g.priority = 'Correctiu crític' THEN 180 + (random() * 60)
        WHEN g.priority = 'Correctiu no crític' THEN 90 + (random() * 45)
        WHEN g.priority = 'Posada en marxa' THEN 120 + (random() * 30)
        WHEN g.priority = 'Visita de diagnosi' THEN 45 + (random() * 20)
        ELSE 30 + (random() * 15)
    END + (CASE WHEN c.model = 'Ultra-ràpid' THEN 40 ELSE 0 END)
FROM GeneratedIncidences g
JOIN RankedChargers c ON c.rn = g.charger_rn;

-- Actualitzem la data de resolució NOMÉS per aquelles que estan tancades
UPDATE INCIDENCE 
SET resolved_at = reported_at + (final_duration_min || ' minutes')::interval
WHERE status = 'Tancada';

-- Si no estan tancades, el temps final no s'hauria de conèixer encara
UPDATE INCIDENCE 
SET final_duration_min = NULL
WHERE status != 'Tancada';

-- 5. Crear les visites (Amb la prioritat completament randomitzada)
INSERT INTO VISIT (contract_id, incidence_id, technician_id, priority, status, planned_date, address, postal_code, location_source, estimated_duration_min)
SELECT 
    (SELECT contract_id FROM CONTRACT WHERE charger_id = I.charger_id LIMIT 1),
    I.incidence_id,
    floor(random() * 20 + 1)::int,
    -- Randomitzem els 5 valors exactes que m'has demanat
    (ARRAY['Correctiu crític', 'Correctiu no crític', 'Manteniment preventiu programat', 'Posada en marxa', 'Visita de diagnosi'])[floor(random() * 5 + 1)::int],
    (ARRAY['Completada', 'Pendent', 'En curs'])[floor(random() * 3 + 1)::int],
    I.reported_at::date,
    'Adreça Hackato ' || I.incidence_id,
    '08001',
    (ARRAY['GPS', 'Client', 'Manual'])[floor(random() * 3 + 1)::int],
    I.estimated_duration_min
FROM INCIDENCE I;