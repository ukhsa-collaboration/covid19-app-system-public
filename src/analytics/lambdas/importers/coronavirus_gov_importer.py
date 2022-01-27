from requests import get
from json import dumps, loads
import csv
import os
import boto3

ENDPOINT = "https://api.coronavirus.data.gov.uk/v1/data"
AREA_TYPE = "nation"


def request_data(area_name):
    filters = [
        f"areaType={AREA_TYPE}",
        f"areaName={area_name}",
    ]

    structure = {"date": "date", "areaName": "areaName", "newCasesByPublishDate": "newCasesByPublishDate",
                 "newCasesBySpecimenDate": "newCasesBySpecimenDate",
                 "newCasesLFDConfirmedPCRBySpecimenDate": "newCasesLFDConfirmedPCRBySpecimenDate",
                 "newCasesLFDOnlyBySpecimenDate": "newCasesLFDOnlyBySpecimenDate",
                 "newCasesPCROnlyBySpecimenDate": "newCasesPCROnlyBySpecimenDate",
                 "newLFDTests": "newLFDTestsBySpecimenDate", "newPCRTestsByPublishDate": "newPCRTestsByPublishDate",
                 "newTestsByPublishDate": "newTestsByPublishDate", "newVirusTests": "newVirusTestsByPublishDate"}

    api_params = {
        "filters": str.join(";", filters),
        "structure": dumps(structure, separators=(",", ":")),
    }

    formats = [
        "json"
    ]

    for fmt in formats:
        api_params["format"] = fmt
        response = get(ENDPOINT, params=api_params, timeout=100)
        assert response.status_code == 200, f"Failed request for {fmt}: {response.text}"
        loading = loads(response.content.decode())
        del loading["data"][0]
        for item in loading["data"]:
            for key, value in item.items():
                if value is None:
                    item[key] = 0

        csv_columns = [key for key in structure]
        csv_file = f"government-data-lookup-{AREA_TYPE}-{area_name}.csv"
        lambda_path = "/tmp/" + csv_file
        try:
            with open(lambda_path, 'w') as csvfile:
                writer = csv.DictWriter(csvfile, fieldnames=csv_columns)
                writer.writeheader()
                print(loading["data"][0])
                for data in loading["data"]:
                    writer.writerow(data)

            with open(lambda_path) as f:
                string = f.read()
                encoded_string = string.encode("utf-8")

            env_name = os.environ["env"]
            bucket_name = env_name + "-analytics-coronavirus-gov-lookup-" + area_name
            s3_path = csv_file
            s3 = boto3.resource("s3")
            s3.Bucket(bucket_name).put_object(Key=s3_path, Body=encoded_string)

        except IOError as e:
            print(e)


def handler(event, context):
    request_data("england")
    request_data("wales")
