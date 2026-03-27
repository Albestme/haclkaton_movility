import os
import boto3
import psycopg2
import pandas as pd
import joblib
import io
from xgboost import XGBRegressor
from sklearn.feature_extraction.text import TfidfVectorizer

# --- CONFIGURACIÓ ---
s3_client = boto3.client('s3')
BUCKET_NAME = os.environ.get('prediccio')

# Fitxers locals temporals
LOCAL_MODEL = '/tmp/model_xgboost_duracio.json'
LOCAL_COLUMNS = '/tmp/columnes_xgboost.pkl'
LOCAL_TFIDF = '/tmp/tfidf_vectorizer.pkl'

def descarregar_artefactes():
    """Baixa el model actual i els traductors de S3"""
    s3_client.download_file(BUCKET_NAME, 'model_xgboost_duracio.json', LOCAL_MODEL)
    s3_client.download_file(BUCKET_NAME, 'columnes_xgboost.pkl', LOCAL_COLUMNS)
    s3_client.download_file(BUCKET_NAME, 'tfidf_vectorizer.pkl', LOCAL_TFIDF)

def preprocessar_dades(df, tfidf_vec, columnes_esperades):
    """Aplica exactament el mateix preprocessat que al Notebook"""
    
    # 1. Tractament de Text (TF-IDF)
    df['description'] = df['description'].fillna('')
    tfidf_matrix = tfidf_vec.transform(df['description'])
    tfidf_df = pd.DataFrame(
        tfidf_matrix.toarray(),
        columns=[f"desc_tfidf_{word}" for word in tfidf_vec.get_feature_names_out()],
        index=df.index
    )
    df = pd.concat([df.drop(columns=['description']), tfidf_df], axis=1)

    # 2. Variables Booleanes i Ordinals
    df['has_rfid'] = df['has_rfid'].astype(int)
    model_mapping = {"Bàsic": 0, "Avançat": 1, "Ultra-ràpid": 2}
    ocpp_mapping = {"OCPP 1.5": 0, "OCPP 1.6": 1, "OCPP 2.0.1": 2}
    df['model'] = df['model'].map(model_mapping)
    df['ocpp_version'] = df['ocpp_version'].map(ocpp_mapping)

    # 3. Any d'instal·lació
    df['installation_date'] = pd.to_datetime(df['installation_date'], errors='coerce')
    df['install_year'] = df['installation_date'].dt.year
    df = df.drop(columns=['installation_date'])

    # 4. One-Hot Encoding (Variables Nominals)
    nominals = ['brand', 'connector_types', 'environment', 'power_type', 'phase_type', 'telecom_provider', 'priority']
    df = pd.get_dummies(df, columns=nominals, drop_first=True, dtype=int)

    # 5. Alineació de columnes (Crucial!)
    # Reindexem perquè tingui les mateixes columnes que el model original
    X = df.reindex(columns=columnes_esperades, fill_value=0)
    return X

def lambda_handler(event, context):
    try:
        # 1. Descarregar model actual
        descarregar_artefactes()
        columnes_model = joblib.load(LOCAL_COLUMNS)
        tfidf_vec = joblib.load(LOCAL_TFIDF)

        # 2. Connectar a la BD i treure dades NOVES (incidències ja resoltes)
        conn = psycopg2.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            password=os.environ.get('DB_PASSWORD'),
            database=os.environ.get('DB_NAME'),
            sslmode='require'
        )
        
        # Query que agafa dades ja resoltes (que tenen durada real registrada)
        # Pots afegir un filtre de data (ex: darrers 7 dies) per no reentrenar-ho tot sempre
        query = """
            SELECT c.*, i.priority, i.description, i.estimated_duration_min
            FROM INCIDENCE i
            INNER JOIN CHARGER c ON i.charger_id = c.charger_id
            WHERE i.estimated_duration_min IS NOT NULL
        """
        df_nous_casos = pd.read_sql(query, conn)
        conn.close()

        if df_nous_casos.empty:
            return {"statusCode": 200, "body": "No hi ha dades noves per entrenar."}

        # 3. Preprocessar les dades
        y_nou = df_nous_casos['estimated_duration_min']
        X_nou = preprocessar_dades(df_nous_casos.drop(columns=['estimated_duration_min']), tfidf_vec, columnes_model)

        # 4. Aprenentatge Incremental
        model = XGBRegressor()
        # El paràmetre 'xgb_model' és la clau: carrega el passat i afegeix arbres nous
        model.fit(X_nou, y_nou, xgb_model=LOCAL_MODEL)

        # 5. Guardar i Pujar el model actualitzat a S3
        model.save_model(LOCAL_MODEL)
        s3_client.upload_file(LOCAL_MODEL, BUCKET_NAME, 'model_xgboost_duracio.json')

        print(f"✅ Model actualitzat amb {len(df_nous_casos)} casos nous.")
        return {"statusCode": 200, "body": "Aprenentatge incremental finalitzat amb èxit."}

    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return {"statusCode": 500, "body": str(e)}