# cargar_seed_correcto.py
import psycopg2
import os
from dotenv import load_dotenv

load_dotenv()

password = os.environ.get('DB_PASSWORD')
user = os.environ.get('DB_USER')

# Ruta al seed que QUIERES (el segundo, con muchos registros)
sql_file_path = './database/seed_data.sql'  # Cambia al nombre correcto

conn = None
try:
    print("🔌 Conectando a la base de datos...")
    conn = psycopg2.connect(
        host='database-1.cveau0o428yi.us-east-1.rds.amazonaws.com',
        port=5432,
        database='postgres',
        user=user,
        password=password,
        sslmode='require'
    )
    
    cur = conn.cursor()
    
    # 1. Limpiar todas las tablas
    print("🧹 Limpiando datos existentes...")
    cur.execute("TRUNCATE TABLE report, visit, incidence, contract, charger, technician CASCADE;")
    conn.commit()
    print("✅ Datos eliminados")
    
    # 2. Cargar el nuevo seed
    print(f"\n📁 Cargando seed: {sql_file_path}")
    with open(sql_file_path, 'r', encoding='utf-8') as file:
        sql_content = file.read()
    
    # Ejecutar el archivo completo
    cur.execute(sql_content)
    conn.commit()
    print("✅ Datos cargados correctamente")
    
    # 3. Verificar resultados
    print("\n📊 VERIFICACIÓN:")
    tables = ['technician', 'charger', 'contract', 'incidence', 'visit', 'report']
    for table in tables:
        cur.execute(f'SELECT COUNT(*) FROM "{table}";')
        count = cur.fetchone()[0]
        print(f"  • {table}: {count} registros")
    
    # Mostrar algunos ejemplos
    print("\n🔍 EJEMPLOS DE DATOS CARGADOS:")
    cur.execute("SELECT charger_id, name FROM charger LIMIT 5;")
    print("\n  Primeros 5 cargadores:")
    for row in cur.fetchall():
        print(f"    ID {row[0]}: {row[1]}")
    
    cur.close()
    
except Exception as e:
    print(f"❌ Error: {e}")
    if conn:
        conn.rollback()
    raise
finally:
    if conn:
        conn.close()
        print("\n🔌 Conexión cerrada.")