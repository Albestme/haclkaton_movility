import psycopg2
import pandas as pd
import os
from dotenv import load_dotenv

load_dotenv()

password = os.environ.get('DB_PASSWORD')
user = os.environ.get('DB_USER')
print(password)
def get_prediction_data():
    """Query data and return as pandas DataFrame"""

    conn = psycopg2.connect(
        host='database-1.cveau0o428yi.us-east-1.rds.amazonaws.com',
        port=5432,
        database='postgres',
        user=user,
        password=password,
        sslmode='require'
    )

    query = """
    SELECT *
    FROM INCIDENCE i
    """
    
    df = pd.read_sql(query, conn)

    df.to_csv('datos_incidencia.csv', index=False, encoding='utf-8')

    print("Archivo CSV creado con éxito.")
get_prediction_data()