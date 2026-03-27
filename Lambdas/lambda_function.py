import os
import boto3
import psycopg2
import pandas as pd
import joblib
from xgboost import XGBRegressor
# Nota: Necessitaràs scikit-learn instal·lat per carregar el TF-IDF!
import os

# --- 1. CONFIGURACIÓ I DESCARREGA DES DE S3 (Cold Start) ---
s3_client = boto3.client('s3')
BUCKET_NAME = os.environ.get('prediccio')

LOCAL_MODEL_PATH = '/tmp/model_xgboost_duracio.json'
LOCAL_COLUMNS_PATH = '/tmp/columnes_xgboost.pkl'
LOCAL_TFIDF_PATH = '/tmp/tfidf_vectorizer.pkl' # <--- NOVA RUTA

def descarregar_fitxers_s3():
    if not os.path.exists(LOCAL_MODEL_PATH):
        print("📥 Descarregant model XGBoost...")
        s3_client.download_file(BUCKET_NAME, 'model_xgboost_duracio.json', LOCAL_MODEL_PATH)
        
    if not os.path.exists(LOCAL_COLUMNS_PATH):
        print("📥 Descarregant llista de columnes...")
        s3_client.download_file(BUCKET_NAME, 'columnes_xgboost.pkl', LOCAL_COLUMNS_PATH)
        
    if not os.path.exists(LOCAL_TFIDF_PATH):
        print("📥 Descarregant vectoritzador TF-IDF...")
        s3_client.download_file(BUCKET_NAME, 'tfidf_vectorizer.pkl', LOCAL_TFIDF_PATH)

print("🚀 Inicialitzant Lambda i carregant models...")
descarregar_fitxers_s3()

# Càrrega a memòria
model_predict = XGBRegressor()
model_predict.load_model(LOCAL_MODEL_PATH)
columnes_model = joblib.load(LOCAL_COLUMNS_PATH)
tfidf_vectorizer = joblib.load(LOCAL_TFIDF_PATH) # <--- CARREGUEM EL TF-IDF


# --- 2. LÒGICA DE PREDICCIÓ ---
def predir_durada(df_input):
    # 1. Antiguitat
    if 'installation_date' in df_input.columns:
        df_input['installation_date'] = pd.to_datetime(df_input['installation_date'])
        df_input['charger_age_days'] = (pd.Timestamp.now() - df_input['installation_date']).dt.days
        df_input = df_input.drop(columns=['installation_date'], errors='ignore')

    # 2. PROCESSAR EL TEXT AMB TF-IDF <--- NOU PAS
    if 'description' in df_input.columns:
        # Omplim els nuls amb text buit per evitar errors
        textos = df_input['description'].fillna('')
        
        # Transformem el text (retorna una matriu 'sparse')
        tfidf_matrix = tfidf_vectorizer.transform(textos)
        
        # Convertim la matriu a un DataFrame amb els noms de les paraules/features
        tfidf_df = pd.DataFrame(
            tfidf_matrix.toarray(), 
            columns=tfidf_vectorizer.get_feature_names_out(), # Usa get_feature_names() si tens una versió vella de scikit-learn
            index=df_input.index
        )
        
        # Unim el nou DataFrame de text amb l'original, i eliminem la columna 'description'
        df_input = pd.concat([df_input.drop(columns=['description']), tfidf_df], axis=1)

    # 3. One-Hot Encoding de la resta (variables categòriques)
    df_input = pd.get_dummies(df_input)

    # 4. RE-ALINEAR COLUMNES (Crucial!)
    df_final = df_input.reindex(columns=columnes_model, fill_value=0)

    # 5. Predicció
    prediccio = model_predict.predict(df_final)
    return float(prediccio[0])

def lambda_handler(event, context):
    # Obtenir l'ID de la incidència des de l'event del trigger
    incidence_id = event.get('incidence_id')
    
    if not incidence_id:
        return {"statusCode": 400, "body": "No s'ha proporcionat cap incidence_id"}

    # Credencials des de Variables d'Entorn
    host = os.environ.get('DB_HOST', 'database-1.cveau0o428yi.us-east-1.rds.amazonaws.com')
    user = os.environ.get('DB_USER')
    password = os.environ.get('DB_PASSWORD')
    database = os.environ.get('DB_NAME', 'postgres')

    conn = None
    try:
        # 1. Connectar a la base de dades
        conn = psycopg2.connect(
            host=host, port=5432, database=database, user=user, password=password, sslmode='require'
        )
        
        # 2. Descarregar NOMÉS la incidència nova
        query_select = "SELECT * FROM INCIDENCE WHERE id = %s"
        df_input = pd.read_sql(query_select, conn, params=(incidence_id,))
        
        if df_input.empty:
            return {"statusCode": 404, "body": f"No s'ha trobat la incidència {incidence_id}"}
        
        # 3. Fer la predicció
        minuts_estimats = predir_durada(df_input)
        print(f"⏱️ Predicció de temps per a ID {incidence_id}: {minuts_estimats:.2f} minuts")
        
        # 4. Actualitzar la base de dades amb la predicció
        cursor = conn.cursor()
        query_update = """
            UPDATE INCIDENCE 
            SET estimated_duration_min = %s 
            WHERE id = %s
        """
        cursor.execute(query_update, (minuts_estimats, incidence_id))
        conn.commit()
        cursor.close()

        return {
            "statusCode": 200,
            "body": f"Incidència {incidence_id} actualitzada amb estimació de {minuts_estimats:.2f} minuts"
        }

    except Exception as e:
        print(f"❌ Error: {str(e)}")
        if conn:
            conn.rollback() # Important fer rollback si hi ha error
        return {"statusCode": 500, "body": f"Error intern: {str(e)}"}
        
    finally:
        # Assegurar-nos que la connexió es tanca sempre
        if conn:
            conn.close()