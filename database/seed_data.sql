-- ============================================================
-- 1. TECHNICIAN
-- ============================================================
INSERT INTO TECHNICIAN (technician_id, name, zone) VALUES
(1,  'Marc Solà',         'Tarragona Centre'),
(2,  'Laura Ferrer',      'Tarragona Nord'),
(3,  'Josep Puig',        'Reus'),
(4,  'Anna Martínez',     'Tarragona Sud'),
(5,  'Carles Vidal',      'Tarragona Centre'),
(6,  'Marta Rius',        'Reus'),
(7,  'Jordi Navarro',     'Tarragona Nord'),
(8,  'Elena Casals',      'Tarragona Sud'),
(9,  'Pere Valls',        'Tarragona Centre'),
(10, 'Núria Font',        'Reus');

-- ============================================================
-- 2. CHARGER (sin latitude/longitude - esas columnas están en VISIT)
-- ============================================================
INSERT INTO CHARGER (charger_id, name, serial_number, brand, model, number_of_plugs, connector_types, has_rfid, environment, power_type, phase_type, max_power_kw, ocpp_version, telecom_provider, installation_date) VALUES
(1,  'Cargador Plaça Imperial',      'SN-IMP-001', 'ABB',      'Terra 53',     2, 'CCS,CHAdeMO', true,  'outdoor', 'DC', 'three-phase', 50.0,  '1.6', 'Movistar',   '2023-01-15'),
(2,  'Cargador Avda. Catalunya',     'SN-CAT-002', 'Siemens',  'VersiCharge',  1, 'Type2',        false, 'outdoor', 'AC', 'single-phase', 22.0,  '1.5', 'Orange',     '2023-02-20'),
(3,  'Cargador Estació Nord',        'SN-EST-003', 'Schneider','EVlink',       2, 'Type2',        true,  'indoor',  'AC', 'three-phase', 22.0,  '1.6', 'Vodafone',   '2023-03-10'),
(4,  'Cargador Polígon Industrial',  'SN-POL-004', 'Circontrol','eNext',       2, 'CCS,Type2',    true,  'outdoor', 'DC', 'three-phase', 150.0, '1.6', 'Movistar',   '2023-04-05'),
(5,  'Cargador Zona Esports',        'SN-ZON-005', 'Wallbox',  'Pulsar Plus',  1, 'Type2',        false, 'outdoor', 'AC', 'single-phase', 7.4,   '1.5', 'Orange',     '2023-05-12'),
(6,  'Cargador Hospital',            'SN-HOS-006', 'ABB',      'Terra 54',     2, 'CCS,CHAdeMO', true,  'indoor',  'DC', 'three-phase', 50.0,  '1.6', 'Movistar',   '2023-06-18'),
(7,  'Cargador Centre Comercial',    'SN-COM-007', 'Siemens',  'SICHARGE',     2, 'CCS,Type2',    true,  'indoor',  'DC', 'three-phase', 150.0, '1.6', 'Vodafone',   '2023-07-22'),
(8,  'Cargador Universitat',         'SN-UNI-008', 'Schneider','EVlink',       1, 'Type2',        false, 'outdoor', 'AC', 'single-phase', 22.0,  '1.5', 'Orange',     '2023-08-30'),
(9,  'Cargador Port',                'SN-POR-009', 'Circontrol','eHome',       1, 'Type2',        false, 'outdoor', 'AC', 'single-phase', 7.4,   '1.5', 'Movistar',   '2023-09-14'),
(10, 'Cargador Aeroport',            'SN-AER-010', 'ABB',      'Terra 53',     2, 'CCS,CHAdeMO', true,  'indoor',  'DC', 'three-phase', 50.0,  '1.6', 'Orange',     '2023-10-01');

-- ============================================================
-- 3. CONTRACT
-- ============================================================
INSERT INTO CONTRACT (contract_id, type, client_id, charger_id, domain_id, start_date, end_date, number_of_visits, frequency, status) VALUES
(1,  'preventiu', 101, 1,  1, '2023-01-01', '2024-12-31', 4, 'trimestral',  'active'),
(2,  'preventiu', 102, 2,  2, '2023-02-01', '2024-01-31', 12, 'mensual',     'active'),
(3,  'correctiu', 103, 3,  3, '2023-03-01', '2024-02-28', 0, 'sota demanda', 'active'),
(4,  'preventiu', 104, 4,  4, '2023-04-01', '2024-03-31', 4, 'trimestral',  'active'),
(5,  'preventiu', 105, 5,  5, '2023-05-01', '2024-04-30', 2, 'semestral',   'active'),
(6,  'correctiu', 106, 6,  6, '2023-06-01', '2024-05-31', 0, 'sota demanda', 'active'),
(7,  'preventiu', 107, 7,  7, '2023-07-01', '2024-06-30', 4, 'trimestral',  'active'),
(8,  'preventiu', 108, 8,  8, '2023-08-01', '2024-07-31', 12, 'mensual',     'active'),
(9,  'preventiu', 109, 9,  9, '2023-09-01', '2024-08-31', 2, 'semestral',   'active'),
(10, 'correctiu', 110, 10, 10, '2023-10-01', '2024-09-30', 0, 'sota demanda', 'active');

-- ============================================================
-- 4. INCIDENCE (corregido: escapando apóstrofes)
-- ============================================================
INSERT INTO INCIDENCE (incidence_id, charger_id, reported_at, priority, status, description, estimated_duration_min, final_duration_min, resolved_at) VALUES
(1,  5,  '2024-01-08 09:15:00', 1, 'resolved',    'El cargador no respon. Possible fallo d''alimentació',        60,  45, '2024-01-08 10:00:00'),
(2,  2,  '2024-01-15 14:30:00', 2, 'in_progress', 'Error de comunicació OCPP. El carregador no es connecta a la xarxa', 120, NULL, NULL),
(3,  8,  '2024-01-20 11:00:00', 1, 'resolved',    'Conector danyat per mal ús. No es pot connectar el vehicle',  90,  85, '2024-01-20 12:25:00'),
(4,  1,  '2024-02-01 08:45:00', 3, 'resolved',    'Actualització de firmware necessària',                        30,  25, '2024-02-01 09:10:00'),
(5,  4,  '2024-02-10 16:20:00', 2, 'pending',     'Problema amb el lector RFID. No detecta targetes',            45, NULL, NULL),
(6,  7,  '2024-02-18 10:00:00', 1, 'resolved',    'Salt de potència. El carregador s''ha reiniciat',              30,  35, '2024-02-18 10:35:00'),
(7,  3,  '2024-03-05 13:15:00', 2, 'in_progress', 'Pantalla tàctil no respon',                                     90, NULL, NULL),
(8,  6,  '2024-03-12 09:30:00', 1, 'resolved',    'Cable de càrrega danyat',                                       60,  55, '2024-03-12 10:25:00'),
(9,  9,  '2024-03-20 15:45:00', 3, 'resolved',    'Actualització de programari',                                   45,  40, '2024-03-20 16:25:00'),
(10, 10, '2024-04-01 08:00:00', 2, 'pending',     'Problema de connexió a Internet. No es pot monitoritzar',      120, NULL, NULL);

-- ============================================================
-- 5. VISIT (con latitude/longitude)
-- ============================================================
INSERT INTO VISIT (visit_id, contract_id, incidence_id, technician_id, visit_type, status, planned_date, address, postal_code, latitude, longitude, location_source, estimated_duration_min) VALUES
(1,  1, NULL, 1, 'preventiu', 'completed', '2023-03-15', 'Plaça Imperial, 1', '43003', 41.11550000, 1.24950000, 'manual', 120),
(2,  2, NULL, 2, 'preventiu', 'completed', '2023-03-20', 'Avda. Catalunya, 45', '43005', 41.12000000, 1.25500000, 'manual', 90),
(3,  3, 1,   3, 'correctiu', 'completed', '2024-01-08', 'Zona Esports, s/n', '43201', 41.10500000, 1.23000000, 'gps',    60),
(4,  4, NULL, 4, 'preventiu', 'completed', '2023-04-10', 'Polígon Industrial, C/ Electrònica, 5', '43204', 41.09500000, 1.21500000, 'manual', 150),
(5,  5, NULL, 5, 'preventiu', 'scheduled', '2024-05-15', 'C/ Tarragona, 23', '43202', 41.11200000, 1.24200000, 'manual', 60),
(6,  6, 2,   6, 'correctiu', 'in_progress', '2024-01-16', 'Hospital Universitari, s/n', '43007', 41.12500000, 1.26500000, 'gps',    120),
(7,  7, NULL, 7, 'preventiu', 'completed', '2023-07-25', 'Centre Comercial, Planta baixa', '43205', 41.10800000, 1.23800000, 'manual', 180),
(8,  8, 3,   8, 'correctiu', 'completed', '2024-01-21', 'Universitat, Campus Nord', '43002', 41.11800000, 1.25200000, 'gps',    90),
(9,  9, NULL, 9, 'preventiu', 'scheduled', '2024-06-10', 'Port, Molla de Llevant', '43004', 41.10200000, 1.24500000, 'manual', 120),
(10, 10, 4,   10, 'correctiu', 'completed', '2024-02-02', 'Aeroport, Terminal T1', '43890', 41.09800000, 1.26000000, 'gps',    60);

-- ============================================================
-- 6. REPORT
-- ============================================================
INSERT INTO REPORT (report_id, visit_id, report_type, status, created_at) VALUES
(1,  1,  'preventiu', 'completed', '2023-03-15 11:30:00'),
(2,  2,  'preventiu', 'completed', '2023-03-20 10:15:00'),
(3,  3,  'correctiu', 'completed', '2024-01-08 14:20:00'),
(4,  4,  'preventiu', 'completed', '2023-04-10 16:45:00'),
(5,  6,  'correctiu', 'draft',    '2024-01-16 12:00:00'),
(6,  7,  'preventiu', 'completed', '2023-07-25 09:30:00'),
(7,  8,  'correctiu', 'completed', '2024-01-21 13:15:00'),
(8,  10, 'correctiu', 'completed', '2024-02-02 11:00:00');