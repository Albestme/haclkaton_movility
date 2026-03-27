import psycopg2
import os
from dotenv import load_dotenv

load_dotenv()

# Configuració
DB_SCHEMA = './database/schema.sql'
DB_SEED = './database/seed_data.sql'

def run_sql_file(cursor, file_path):
    print(f"📖 Llegint {file_path}...")
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    cursor.execute(content)

try:
    print("🔌 Conectant a AWS RDS...")
    conn = psycopg2.connect(
        host='database-1.cveau0o428yi.us-east-1.rds.amazonaws.com',
        port=5432,
        database='postgres',
        user=os.environ.get('DB_USER'),
        password=os.environ.get('DB_PASSWORD'),
        sslmode='require'
    )
    cur = conn.cursor()

    # 1. Executar Esquema (Borra i Crea taules)
    print("\n🏗️  Generant estructura...")
    run_sql_file(cur, DB_SCHEMA)
    conn.commit()
    print("✅ Esquema creat.")

    # 2. Executar Seed (Inserta dades)
    print("\n🌱 Inserint dades inicials...")
    run_sql_file(cur, DB_SEED)
    conn.commit()
    print("✅ Dades carregades.")

    # 3. Verificació ràpida
    cur.execute("SELECT COUNT(*) FROM incidence;")
    count = cur.fetchone()[0]
    print(f"\n🚀 Llest! Tens {count} incidències per entrenar l'XGBoost.")

    cur.close()

except Exception as e:
    print(f"\n❌ ERROR: {e}")
    if conn: conn.rollback()
finally:
    if conn:
        conn.close()
        print("\n🔌 Conexió tancada.")