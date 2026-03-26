import psycopg2
import os
from dotenv import load_dotenv

load_dotenv()

password = os.environ.get('DB_PASSWORD')
user = os.environ.get('DB_USER')

# Rutas a los archivos SQL
schema_file_path = './database/schema.sql'  # Archivo para crear tablas
seed_file_path = './database/seed_data.sql'  # Archivo para insertar datos

def execute_sql_file(conn, file_path, description):
    """Ejecuta un archivo SQL manejando errores correctamente"""
    cur = conn.cursor()
    
    print(f"\n{'='*80}")
    print(f"📁 Ejecutando {description}: {file_path}")
    print(f"{'='*80}")
    
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            sql_content = file.read()
        
        # Dividir en sentencias individuales
        statements = []
        current_statement = []
        in_string = False
        string_char = None
        
        for line in sql_content.split('\n'):
            # Verificar si estamos dentro de un string para no dividir por ; dentro de strings
            for char in line:
                if char in ("'", '"') and not in_string:
                    in_string = True
                    string_char = char
                elif char == string_char and in_string:
                    in_string = False
            
            current_statement.append(line)
            
            # Si la línea termina con ; y no estamos dentro de un string
            if line.strip().endswith(';') and not in_string:
                statements.append('\n'.join(current_statement))
                current_statement = []
        
        # Ejecutar cada sentencia
        print(f"Encontradas {len(statements)} sentencias SQL")
        
        successful = 0
        failed = 0
        
        for i, statement in enumerate(statements, 1):
            if statement.strip():
                try:
                    print(f"  Ejecutando sentencia {i}...", end=' ')
                    cur.execute(statement)
                    print("✓ OK")
                    successful += 1
                except psycopg2.Error as e:
                    print(f"✗ ERROR")
                    print(f"    Error: {e}")
                    print(f"    Sentencia: {statement[:150]}...")
                    failed += 1
                    # No continuar si hay error en creación de tablas
                    if description == "creación de tablas":
                        raise
        
        conn.commit()
        print(f"\n✅ {description}: {successful} exitosas, {failed} fallidas")
        
    except Exception as e:
        conn.rollback()
        print(f"\n❌ Error en {description}: {e}")
        raise
    finally:
        cur.close()

def fix_sql_file(input_file, output_file):
    """Corrige el archivo SQL escapando apóstrofes"""
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Escapar apóstrofes dentro de strings
    # Algoritmo simple: buscar texto entre comillas simples y escapar apóstrofes internos
    result = []
    in_string = False
    i = 0
    
    while i < len(content):
        if content[i] == "'" and not in_string:
            in_string = True
            result.append(content[i])
            i += 1
        elif content[i] == "'" and in_string:
            # Verificar si es un apóstrofe escapado
            if i + 1 < len(content) and content[i+1] == "'":
                result.append(content[i])
                i += 1
            else:
                in_string = False
                result.append(content[i])
                i += 1
        elif in_string and content[i] == "'":
            # Escapar apóstrofes dentro del string
            result.append("''")
            i += 1
        else:
            result.append(content[i])
            i += 1
    
    fixed_content = ''.join(result)
    
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(fixed_content)
    
    return output_file

def check_tables_exist(conn):
    """Verifica qué tablas existen"""
    cur = conn.cursor()
    cur.execute("""
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_type = 'BASE TABLE'
        ORDER BY table_name;
    """)
    tables = [row[0] for row in cur.fetchall()]
    cur.close()
    return tables

def main():
    conn = None
    try:
        # Conectar a la base de datos
        print("🔌 Conectando a la base de datos...")
        conn = psycopg2.connect(
            host='database-1.cveau0o428yi.us-east-1.rds.amazonaws.com',
            port=5432,
            database='postgres',
            user=user,
            password=password,
            sslmode='require'
        )
        print("✅ Conexión establecida")
        
        # Verificar tablas existentes
        existing_tables = check_tables_exist(conn)
        print(f"\n📊 Tablas existentes: {existing_tables if existing_tables else 'Ninguna'}")
        
        # Preguntar si crear tablas
        if not existing_tables:
            print("\n⚠️  No hay tablas en la base de datos.")
            response = input("¿Deseas crear las tablas desde schema.sql? (y/n): ")
            if response.lower() == 'y':
                if os.path.exists(schema_file_path):
                    execute_sql_file(conn, schema_file_path, "creación de tablas")
                else:
                    print(f"❌ No se encuentra el archivo: {schema_file_path}")
                    return
        else:
            print(f"\n✅ Ya existen {len(existing_tables)} tablas en la base de datos")
            print(f"   Tablas: {', '.join(existing_tables)}")
        
        # Insertar datos semilla
        if os.path.exists(seed_file_path):
            # Crear versión corregida del archivo seed
            fixed_seed_file = './database/seed_data_fixed.sql'
            print(f"\n🔧 Corrigiendo archivo seed_data.sql...")
            fix_sql_file(seed_file_path, fixed_seed_file)
            print(f"✅ Archivo corregido guardado en: {fixed_seed_file}")
            
            # Ejecutar el archivo corregido
            execute_sql_file(conn, fixed_seed_file, "inserción de datos")
        else:
            print(f"❌ No se encuentra el archivo: {seed_file_path}")
        
        print("\n" + "="*80)
        print("🎉 INSTALACIÓN COMPLETADA CON ÉXITO")
        print("="*80)
        
        # Mostrar resumen final
        final_tables = check_tables_exist(conn)
        print(f"\n📊 Tablas en la base de datos:")
        for table in final_tables:
            cur = conn.cursor()
            cur.execute(f'SELECT COUNT(*) FROM "{table}";')
            count = cur.fetchone()[0]
            cur.close()
            print(f"  - {table}: {count} registros")
        
    except Exception as e:
        print(f"\n❌ Error general: {e}")
        raise
    finally:
        if conn:
            conn.close()
            print("\n🔌 Conexión cerrada.")

if __name__ == "__main__":
    main()