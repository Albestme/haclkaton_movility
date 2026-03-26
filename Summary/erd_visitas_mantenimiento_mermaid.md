erDiagram
CONTRACT {
bigint id PK
string type
bigint client_id
bigint charger_id
bigint domain_id
date start_date
date end_date
int number_of_visits
string frequency
string status
}

INCIDENCE {
bigint id PK
bigint charger_id
bigint domain_id
string status
string priority
boolean auto_create_visit
datetime created_at
}

TECHNICIAN {
bigint id PK
string name
string zone
}

VISIT {
bigint id PK
bigint contract_id FK
bigint incidence_id FK
bigint technician_id FK
string visit_type
string status
datetime planned_date

    %% ubicación (snapshot del cargador)
    string address
    string postal_code
    float latitude
    float longitude
    string location_source
    %% charger_snapshot | manual_override

    int estimated_duration

}

REPORT {
bigint id PK
bigint visit_id FK
string report_type
string status
datetime created_at
}

CONTRACT ||--o{ VISIT : "generates"
INCIDENCE ||--o{ VISIT : "triggers"
TECHNICIAN ||--o{ VISIT : "assigned"
VISIT ||--o| REPORT : "produces"
