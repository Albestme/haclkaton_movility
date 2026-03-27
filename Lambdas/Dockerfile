# 1. Utilitzem la imatge base oficial d'AWS Lambda per a Python 3.10
FROM public.ecr.aws/lambda/python:3.10

# 2. Copiem el fitxer de requeriments a l'arrel de la Lambda
COPY requirements.txt ${LAMBDA_TASK_ROOT}

# 3. Instal·lem les llibreries pesades (Pandas, XGBoost, Scikit-Learn, etc.)
RUN pip install -r requirements.txt

# 4. Copiem el teu codi (la Lambda) a l'arrel
COPY lambda_function.py ${LAMBDA_TASK_ROOT}

# 5. Li diem a AWS quin és el punt d'entrada (NomDelFitxer.NomDeLaFunció)
CMD [ "lambda_function.lambda_handler" ]