import psycopg2
from psycopg2.extras import RealDictCursor
import os
from dotenv import load_dotenv

# Load environment variables from the .env file
load_dotenv()

# Get credentials securely
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_USER = os.environ.get('DB_USER')

def get_db_connection():
    """
    Establishes and returns a connection to the PostgreSQL database
    using credentials from the .env file.
    """
    if not DB_PASSWORD or not DB_USER:
        raise ValueError("Database credentials are missing! Check your .env file.")

    try:
        conn = psycopg2.connect(
            host='database-1.cveau0o428yi.us-east-1.rds.amazonaws.com',
            port=5432,
            database='postgres',
            user=DB_USER,
            password=DB_PASSWORD,
            sslmode='require'
        )
        return conn
    except Exception as e:
        print(f"Error connecting to the database: {e}")
        raise