FROM public.ecr.aws/lambda/python:3.10
COPY requirements.txt ${LAMBDA_TASK_ROOT}
RUN pip install --upgrade pip
RUN pip install -r requirements.txt
COPY incremental_learning.py ${LAMBDA_TASK_ROOT}
CMD [ "incremental_learning.lambda_handler" ]