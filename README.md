# MontePilot - Copilot d'IA per al manteniment en mobilitat elèctrica

**MontePilot** és un sistema d'assistència intel·ligent dissenyat per optimitzar la planificació i execució d'intervencions tècniques en xarxes de punts de recàrrega de vehicles elèctrics. Aquest projecte ha estat desenvolupat en el marc de la **Hackató SmAIrt Mobility** per a l'empresa **Etecnic**.

L'objectiu principal és oferir un "copilot" que ajudi a estructurar, prioritzar i executar operacions de camp de manera eficient, viable i sostenible, integrant restriccions reals com l'autonomia dels vehicles, la prioritat del treball i la jornada laboral.

## Característiques Principals

El sistema presenta un doble enfocament obligatori per cobrir tota la cadena operativa:

### 1. Assistència al Departament d'Operacions (Web)
Interfície centralitzada per a la gestió global que permet:
* **Identificació i Priorització**: Gestió de correctius crítics, no crítics, manteniment preventiu i posades en marxa.
* **Planificació Intel·ligent**: Agrupació d'intervencions per optimitzar desplaçaments i gestionar rutes de jornades completes.
* **Gestió d'Imprevistos**: Eina per al replantejament dinàmic davant incidències de trànsit o canvis d'última hora.

### 2. Autogestió Assistida del Tècnic (Mobile App)
Aplicació per als operaris de camp que facilita:
* **Visualització de Tasques**: Accés al calendari setmanal i a les intervencions assignades.
* **Rutes Realistes**: Planificació tenint en compte el punt d'inici, l'autonomia del vehicle elèctric i els temps de recàrrega.
* **Digitalització d'Evidències**: Formulari per generar informes tècnics (correctius, preventius i posades en marxa) amb dades i validacions.

## 🛠️ Stack Tecnològic

El projecte és una solució multi-plataforma i modular:
* **Web Dashboard**: Desenvolupat amb **Next.js** i Tailwind CSS.
* **Mobile App**: Construïda amb **Kotlin Multiplatform (KMP)** per a Android i iOS.
* **Backend & Intel·ligència**:
    * **Python (FastAPI)** per a la gestió de mètriques i dades.
    * **Model Predictor**: Algorisme XGBoost per predir la durada de les intervencions.
    * **Algorisme de Planificació**: Lògica de *rescheduling* dinàmic per a tasques crítiques.
* **Infraestructura Cloud**: Integració amb **AWS Lambdas** i buckets S3 per a la gestió d'incidències i evidències.

## 📊 Mètriques i Observabilitat

El sistema monitoritza indicadors clau per avaluar l'eficiència operativa:
* Intervencions completades vs. pendents.
* Retards i les seves causes documentades.
* Quilòmetres recorreguts i costos de desplaçament.
* Hores efectives de treball i indicadors de benestar del tècnic.

## 📁 Estructura del Repositori

* `/web`: Aplicació de panell de control (Next.js).
* `/OperaryMovilityApp`: Aplicació mòbil (Kotlin Multiplatform).
* `/planner`: Algorismes de planificació i criteris de priorització.
* `/predictor`: Models d'IA per a la predicció de temps de treball.
* `/backend_metrics`: API per a la gestió de base de dades i KPIs.
* `/database`: Esquemes SQL (PostgreSQL) i dades llavor (*seed data*) per al prototip.
* `/Lambdas`: Codi per al desplegament de funcions en el núvol.