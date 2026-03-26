# verificar_seed_grande.py
import psycopg2
import os
from dotenv import load_dotenv

load_dotenv()

conn = psycopg2.connect(
    host='database-1.cveau0o428yi.us-east-1.rds.amazonaws.com',
    port=5432,
    database='postgres',
    user=os.environ.get('DB_USER'),
    password=os.environ.get('DB_PASSWORD'),
    sslmode='require'
)

cur = conn.cursor()

print("="*70)
print("VERIFICACIÓN DE SEED CARGADO")
print("="*70)

# 1. CONTAR REGISTROS POR TABLA
print("\n📊 1. CONTEO DE REGISTROS:")
tables = ['technician', 'charger', 'contract', 'incidence', 'visit', 'report']
results = {}

for table in tables:
    cur.execute(f'SELECT COUNT(*) FROM "{table}";')
    count = cur.fetchone()[0]
    results[table] = count
    print(f"  • {table:12}: {count:4} registros")

# 2. DETERMINAR QUÉ SEED ESTÁ CARGADO
print("\n🔍 2. ANÁLISIS:")
print("  " + "-"*50)

# Características del seed GRANDE (470 registros)
if results['charger'] > 100:
    print("  ✅ Seed GRANDE detectado (más de 100 cargadores)")
    print(f"     • Cargadores: {results['charger']} registros")
    print(f"     • Contratos: {results['contract']} registros")
    print(f"     • Visitas: {results['visit']} registros")
    
    # Verificar nombres de cargadores (seed grande tiene nombres de municipios)
    cur.execute("SELECT name FROM charger WHERE name LIKE '%Ajuntament%' OR name LIKE '%Constantí%' LIMIT 3;")
    ejemplos = cur.fetchall()
    if ejemplos:
        print("\n  📌 Ejemplos de cargadores (seed GRANDE):")
        for ex in ejemplos:
            print(f"     • {ex[0][:60]}...")
    
# Características del seed PEQUEÑO (10 registros)
elif results['charger'] <= 10:
    print("  📌 Seed PEQUEÑO detectado (solo 10 cargadores)")
    print("     • Cargadores con nombres descriptivos como 'Cargador Plaça Imperial'")
    
    cur.execute("SELECT name FROM charger WHERE name LIKE '%Cargador%' LIMIT 3;")
    ejemplos = cur.fetchall()
    if ejemplos:
        print("\n  📌 Ejemplos de cargadores (seed PEQUEÑO):")
        for ex in ejemplos:
            print(f"     • {ex[0]}")

else:
    print("  ❓ No se pudo determinar claramente")

# 3. VERIFICAR ESTRUCTURA (diferencia clave entre seeds)
print("\n🔬 3. DIFERENCIAS CLAVE:")

# Seed GRANDE tiene muchos registros con "Unknown" en el nombre
cur.execute("SELECT COUNT(*) FROM charger WHERE name = 'Unknown' OR name LIKE '%Unknown%';")
unknown_count = cur.fetchone()[0]
print(f"  • Cargadores con nombre 'Unknown': {unknown_count}")

# Seed GRANDE tiene contratos SIN domain_id
cur.execute("SELECT COUNT(*) FROM contract WHERE domain_id IS NULL;")
contracts_without_domain = cur.fetchone()[0]
print(f"  • Contratos SIN domain_id: {contracts_without_domain}")

# Seed GRANDE tiene visitas SIN postal_code
cur.execute("SELECT COUNT(*) FROM visit WHERE postal_code IS NULL;")
visits_without_postal = cur.fetchone()[0]
print(f"  • Visitas SIN postal_code: {visits_without_postal}")

# 4. MOSTRAR PRIMEROS REGISTROS
print("\n📋 4. PRIMEROS REGISTROS DE CADA TABLA:")

# Técnicos
cur.execute("SELECT technician_id, name, zone FROM technician LIMIT 3;")
print("\n  TÉCNICOS:")
for row in cur.fetchall():
    print(f"    ID {row[0]}: {row[1]} - {row[2]}")

# Cargadores
cur.execute("SELECT charger_id, name FROM charger LIMIT 5;")
print("\n  CARGADORES (primeros 5):")
for row in cur.fetchall():
    print(f"    ID {row[0]}: {row[1][:60]}")

# Contratos
cur.execute("SELECT contract_id, charger_id FROM contract LIMIT 5;")
print("\n  CONTRATOS (primeros 5):")
for row in cur.fetchall():
    print(f"    ID {row[0]}: charger_id = {row[1]}")

# Incidencias
cur.execute("SELECT incidence_id, charger_id, description FROM incidence LIMIT 3;")
print("\n  INCIDENCIAS (primeras 3):")
for row in cur.fetchall():
    print(f"    ID {row[0]}: charger {row[1]} - {row[2][:50]}...")

# 5. CONCLUSIÓN
print("\n" + "="*70)
print("✅ CONCLUSIÓN:")
if results['charger'] > 100:
    print("  Tienes cargado el SEED GRANDE (con ~470 registros por tabla)")
    print(f"  • Total de cargadores: {results['charger']}")
    print(f"  • Es el seed que tenía nombres como 'Ajuntament de Cabrera de Mar'")
elif results['charger'] == 10:
    print("  Tienes cargado el SEED PEQUEÑO (con 10 registros por tabla)")
    print("  • Cargadores con nombres descriptivos como 'Cargador Plaça Imperial'")
else:
    print("  Datos inconsistentes. Verifica la carga.")
print("="*70)

cur.close()
conn.close()