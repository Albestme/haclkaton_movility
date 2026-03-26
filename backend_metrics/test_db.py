import psycopg2
import os
from dotenv import load_dotenv

load_dotenv()

password = os.environ.get('DB_PASSWORD')
user = os.environ.get('DB_USER')

def show_database_summary():
    conn = None
    try:
        conn = psycopg2.connect(
            host='database-1.cveau0o428yi.us-east-1.rds.amazonaws.com',
            port=5432,
            database='postgres',
            user=user,
            password=password,
            sslmode='require'
        )
        
        cur = conn.cursor()
        
        print("\n" + "="*80)
        print("RESUMEN DE BASE DE DATOS")
        print("="*80)
        
        # Información general
        cur.execute("SELECT version();")
        version = cur.fetchone()[0]
        print(f"\n📌 PostgreSQL Version: {version}")
        
        # Tamaño de la base de datos
        cur.execute("""
            SELECT pg_database_size(current_database()) / 1024 / 1024 as size_mb;
        """)
        size = cur.fetchone()[0]
        print(f"📦 Tamaño de la BD: {size} MB")
        
        # Tablas y datos
        cur.execute("""
            SELECT 
                table_name,
                (SELECT count(*) FROM information_schema.columns WHERE table_name=t.table_name) as columns,
                (SELECT count(*) FROM information_schema.table_constraints 
                 WHERE table_name=t.table_name AND constraint_type='PRIMARY KEY') as has_pk
            FROM information_schema.tables t
            WHERE table_schema = 'public' 
            AND table_type = 'BASE TABLE'
            ORDER BY table_name;
        """)
        
        tables = cur.fetchall()
        print(f"\n📊 TABLAS ENCONTRADAS: {len(tables)}")
        print(f"{'Tabla':<30} {'Columnas':<10} {'Registros':<15} {'PK'}")
        print(f"{'-'*70}")
        
        total_records = 0
        for table_name, columns, has_pk in tables:
            cur.execute(f'SELECT COUNT(*) FROM "{table_name}";')
            row_count = cur.fetchone()[0]
            total_records += row_count
            pk_mark = "✓" if has_pk else "✗"
            print(f"{table_name:<30} {columns:<10} {row_count:<15} {pk_mark}")
        
        print(f"{'-'*70}")
        print(f"TOTAL REGISTROS: {total_records}")
        
        cur.close()
        
    except Exception as e:
        print(f"Error: {e}")
        raise
    finally:
        if conn:
            conn.close()

if __name__ == "__main__":
    show_database_summary()