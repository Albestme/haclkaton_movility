erDiagram
    CONTRACT {
        bigint contract_id PK
        string type
        bigint client_id
        bigint charger_id FK
        bigint domain_id
        date start_date
        date end_date
        int number_of_visits
        string frequency
        string status
    }

    CHARGER {
        bigint charger_id PK
        string name 
        string serial_number
        string brand 
        string model 
        int number_of_plugs 
        string connector_types 
        boolean has_rfid 
        string environment 
        string power_type 
        string phase_type 
        float max_power_kw 
        string ocpp_version 
        string telecom_provider 
        date installation_date 
        float latitude 
        float longitude 
        string status 
    }

    INCIDENCE {
        bigint incidence_id PK
        bigint charger_id FK
        datetime reported_at 
        smallint priority 
        string status 
        text description
        int estimated_duration_min 
        int final_duration_min 
        datetime resolved_at 
    }

    TECHNICIAN {
        bigint technician_id PK
        string name
        string zone
    }

    VISIT {
        bigint visit_id PK
        bigint contract_id FK "Pot ser NULL si és només correctiu"
        bigint incidence_id FK "Pot ser NULL si és només preventiu"
        bigint technician_id FK
        string visit_type
        string status
        datetime planned_date
        string address
        string postal_code
        float latitude
        float longitude
        string location_source "Ex: charger_snapshot | manual_override"
        int estimated_duration_min
    }

    REPORT {
        bigint report_id PK
        bigint visit_id FK
        string report_type
        string status
        datetime created_at
    }

    %% Relacions
    CHARGER ||--o{ INCIDENCE : "té"
    CHARGER ||--o{ CONTRACT : "cobert_per"
    CONTRACT ||--o{ VISIT : "genera_preventius"
    INCIDENCE ||--o{ VISIT : "desencadena_correctius"
    TECHNICIAN ||--o{ VISIT : "assignat_a"
    VISIT ||--o| REPORT : "produeix"