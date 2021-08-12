import csv
import os
import boto3

DATA_SET_ID = "data_set_id"
DATA_SET_NAME = "name"
DATA_SET_LAST_REFRESH = "last_refresh"
DATA_SET_RELATIONAL_TABLE_DATA_SOURCE_ID = "relational_table_data_source_id"
DATA_SET_CUSTOM_SQL_DATA_SOURCE_ID = "custom_sql_data_source_id"
DATA_SET_CUSTOM_SQL_QUERY = "custom_sql_query"
DATA_SET_S3_DATA_SOURCE_ID = "s3_data_source_id"
DATA_SET_COLUMNS = [DATA_SET_ID,
                    DATA_SET_NAME,
                    DATA_SET_LAST_REFRESH,
                    DATA_SET_RELATIONAL_TABLE_DATA_SOURCE_ID,
                    DATA_SET_CUSTOM_SQL_DATA_SOURCE_ID,
                    DATA_SET_CUSTOM_SQL_QUERY,
                    DATA_SET_S3_DATA_SOURCE_ID]

INGESTION_DATA_SET_ID = "data_set_id"
INGESTION_ID = "ingestion_id"
INGESTION_STATUS = "status"
INGESTION_CREATED_TIME = "created_time"
INGESTION_TIME_IN_SECONDS = "time_in_seconds"
INGESTION_COLUMNS = [INGESTION_DATA_SET_ID,
                     INGESTION_ID,
                     INGESTION_STATUS,
                     INGESTION_CREATED_TIME,
                     INGESTION_TIME_IN_SECONDS]

DASHBOARD_ID = "dashboard_id"
DASHBOARD_NAME = "name"
DASHBOARD_COLUMNS = [DASHBOARD_ID,
                     DASHBOARD_NAME]

DASHBOARDS_FOR_DATA_SETS_COLUMNS = [DASHBOARD_ID,
                                    DATA_SET_ID]

DATA_SOURCE_ID = "data_source_id"
DATA_SOURCE_NAME = "name"
DATA_SOURCE_COLUMNS = [DATA_SOURCE_ID,
                       DATA_SOURCE_NAME]

ANALYSIS_ID = "analysis_id"
ANALYSIS_NAME = "name"
ANALYSIS_COLUMNS = [ANALYSIS_ID,
                    ANALYSIS_NAME]

ANALYSES_FOR_DATA_SETS_COLUMNS = [ANALYSIS_ID,
                                  DATA_SET_ID]

USER_ARN = "user_arn"

USERS_FOR_DATA_SETS_COLUMNS = [USER_ARN,
                               DATA_SET_ID]

def get_data_sets(account_id):
    quicksight = boto3.client("quicksight")
    response = quicksight.list_data_sets(AwsAccountId=account_id)
    data_sets = response["DataSetSummaries"]
    while response.get("NextToken", None) is not None:
        response = quicksight.list_data_sets(AwsAccountId=account_id, NextToken=response["NextToken"])
        data_sets = data_sets + response["DataSetSummaries"]
    return data_sets

def get_dashboards(account_id):
    quicksight = boto3.client("quicksight")
    response = quicksight.list_dashboards(AwsAccountId=account_id)
    dashboards = response["DashboardSummaryList"]
    while response.get("NextToken", None) is not None:
        response = quicksight.list_dashboards(AwsAccountId=account_id, NextToken=response["NextToken"])
        dashboards = dashboards + response["DashboardSummaryList"]
    return dashboards

def get_data_sources(account_id):
    quicksight = boto3.client("quicksight")
    response = quicksight.list_data_sources(AwsAccountId=account_id)
    data_sources = response["DataSources"]
    while response.get("NextToken", None) is not None:
        response = quicksight.list_data_sources(AwsAccountId=account_id, NextToken=response["NextToken"])
        data_sources = data_sources + response["DataSources"]
    return data_sources

def get_analyses(account_id):
    quicksight = boto3.client("quicksight")
    response = quicksight.list_analyses(AwsAccountId=account_id)
    analyses = response["AnalysisSummaryList"]
    while response.get("NextToken", None) is not None:
        response = quicksight.list_analyses(AwsAccountId=account_id, NextToken=response["NextToken"])
        analyses = analyses + response["AnalysisSummaryList"]
    return analyses

def get_users(account_id):
    quicksight = boto3.client("quicksight")
    response = quicksight.list_users(AwsAccountId=account_id, Namespace="default")
    users = response["UserList"]
    while response.get("NextToken", None) is not None:
        response = quicksight.list_users(AwsAccountId=account_id, Namespace="default", NextToken=response["NextToken"])
        users = users + response["UserList"]
    return users

def get_data_set_descriptions(account_id, data_sets):
    quicksight = boto3.client("quicksight")
    data_set_descriptions = {}
    for data_set in data_sets:
        data_set_id = data_set["DataSetId"]
        try:
            data_set_description = quicksight.describe_data_set(AwsAccountId=account_id, DataSetId=data_set_id)
            data_set_descriptions[data_set_id] = data_set_description["DataSet"]
        except Exception as error:
            print("Describe dataset for DataSetId=" + data_set_id + " Name=\"" + data_set["Name"] + "\" returns error " + str(error))
    return data_set_descriptions

def get_dashboard_descriptions(account_id, dashboards):
    quicksight = boto3.client("quicksight")
    dashboard_descriptions = {}
    for dashboard in dashboards:
        dashboard_id = dashboard["DashboardId"]
        try:
            dashboard_description = quicksight.describe_dashboard(AwsAccountId=account_id, DashboardId=dashboard_id)
            dashboard_descriptions[dashboard_id] = dashboard_description["Dashboard"]
        except Exception as error:
            print("Describe dashboard for DashboardId=" + dashboard_id + " Name=\"" + dashboard["Name"] + "\" returns error " + str(error))
    return dashboard_descriptions

def get_analysis_descriptions(account_id, analyses):
    quicksight = boto3.client("quicksight")
    analysis_descriptions = {}
    for analysis in analyses:
        analysis_id = analysis["AnalysisId"]
        try:
            analysis_description = quicksight.describe_analysis(AwsAccountId=account_id, AnalysisId=analysis_id)
            analysis_descriptions[analysis_id] = analysis_description["Analysis"]
        except Exception as error:
            print("Describe analysis for AnalysisId=" + analysis_id + " Name=\"" + analysis["Name"] + "\" returns error " + str(error))
    return analysis_descriptions

def get_data_set_permissions(account_id, data_sets):
    quicksight = boto3.client("quicksight")
    data_set_permissions = {}
    for data_set in data_sets:
        data_set_id = data_set["DataSetId"]
        try:
            data_set_permission = quicksight.describe_data_set_permissions(AwsAccountId=account_id, DataSetId=data_set["DataSetId"])
            data_set_permissions[data_set_id] = data_set_permission
        except Exception as error:
            print("Describe dataset permissions for DataSetId=" + data_set_id + " Name=\"" + data_set["Name"] + "\" returns error " + str(error))
    return data_set_permissions

def get_data_set_ingestions(account_id, data_sets):
    quicksight = boto3.client("quicksight")
    ingestions = {}
    for data_set in data_sets:
        data_set_id = data_set["DataSetId"]
        try:
            response = quicksight.list_ingestions(AwsAccountId=account_id, DataSetId=data_set_id)
            ingestions_for_data_set = response["Ingestions"]
            while response.get("NextToken", None) is not None:
                response = quicksight.list_ingestions(AwsAccountId=account_id, DataSetId=data_set_id, NextToken=response["NextToken"])
                ingestions_for_data_set = ingestions_for_data_set + response["Ingestions"]
            ingestions[data_set_id] = ingestions_for_data_set
        except Exception as error:
            print("List ingestions for DataSetId=" + data_set_id + " Name=\"" + data_set["Name"] + "\" returns error " + str(error))
    return ingestions

def convert_data_sets(data_sets, data_set_descriptions, data_sources):
    data_source_id_by_arn = {}
    for data_source in data_sources:
        data_source_id_by_arn[data_source["Arn"]] = data_source["DataSourceId"]

    converted_data_sets = []
    for data_set in data_sets:
        data_set_id = data_set["DataSetId"]
        if data_set_id in data_set_descriptions:
            relational_table_data_source_id = None
            custom_sql_data_source_id = None
            custom_sql_query = None
            s3_source_data_source_id = None
            data_set_description = data_set_descriptions[data_set_id]
            if "PhysicalTableMap" in data_set_description:
                for physical_table in data_set_description["PhysicalTableMap"].values():
                    if "RelationalTable" in physical_table:
                        data_source_arn = physical_table["RelationalTable"]["DataSourceArn"]
                        if data_source_arn in data_source_id_by_arn:
                            relational_table_data_source_id = data_source_id_by_arn[data_source_arn]
                        else:
                            print("Data set DataSetId=" + data_set_id + " Name=\"" + data_set["Name"] + "\" has reference to non-existent relational table data source " + str(data_source_arn))
                    if "CustomSql" in physical_table:
                        data_source_arn = physical_table["CustomSql"]["DataSourceArn"]
                        if data_source_arn in data_source_id_by_arn:
                            custom_sql_data_source_id = data_source_id_by_arn[data_source_arn]
                            custom_sql_query = physical_table["CustomSql"]["SqlQuery"].replace('\n', ' ')
                        else:
                            print("Data set DataSetId=" + data_set_id + " Name=\"" + data_set["Name"] + "\" has reference to non-existent custom SQL data source " + str(data_source_arn))
                    if "S3Source" in physical_table:
                        data_source_arn = physical_table["S3Source"]["DataSourceArn"]
                        if data_source_arn in data_source_id_by_arn:
                            s3_source_data_source_id = data_source_id_by_arn[data_source_arn]
                        else:
                            print("Data set DataSetId=" + data_set_id + " Name=\"" + data_set["Name"] + "\" has reference to non-existent S3 data source " + str(data_source_arn))
            converted_data_set = {DATA_SET_ID: data_set_id,
                                  DATA_SET_NAME: data_set.get("Name", None),
                                  DATA_SET_LAST_REFRESH: data_set_description.get("LastUpdatedTime", None),
                                  DATA_SET_RELATIONAL_TABLE_DATA_SOURCE_ID: relational_table_data_source_id,
                                  DATA_SET_CUSTOM_SQL_DATA_SOURCE_ID: custom_sql_data_source_id,
                                  DATA_SET_CUSTOM_SQL_QUERY: custom_sql_query,
                                  DATA_SET_S3_DATA_SOURCE_ID: s3_source_data_source_id}
            converted_data_sets.append(converted_data_set)
    return converted_data_sets

def convert_ingestions(data_sets, ingestions):
    converted_ingestions = []
    for data_set in data_sets:
        data_set_id = data_set["DataSetId"]
        ingestion_list = ingestions[data_set_id]
        for ingestion in ingestion_list:
            converted_ingestion = {INGESTION_DATA_SET_ID: data_set_id,
                                   INGESTION_ID: ingestion["IngestionId"],
                                   INGESTION_STATUS: ingestion.get("IngestionStatus", None),
                                   INGESTION_CREATED_TIME: ingestion.get("CreatedTime", None),
                                   INGESTION_TIME_IN_SECONDS: ingestion.get("IngestionTimeInSeconds", None)}
            converted_ingestions.append(converted_ingestion)
    return converted_ingestions

def convert_dashboards(dashboards, dashboard_descriptions):
    converted_dashboards = []
    for dashboard in dashboards:
        dashboard_id = dashboard["DashboardId"]
        if dashboard_id in dashboard_descriptions:
            converted_dashboard = {DASHBOARD_ID: dashboard_id,
                                   DASHBOARD_NAME: dashboard.get("Name", None)}
            converted_dashboards.append(converted_dashboard)
    return converted_dashboards

def convert_dashboards_for_data_sets(data_sets, dashboard_descriptions):
    data_set_id_by_arn = {}
    for data_set in data_sets:
        data_set_id_by_arn[data_set["Arn"]] = data_set["DataSetId"]

    converted_dashboards_for_data_sets = []
    for dashboard_description in dashboard_descriptions.values():
        for data_set_arn in dashboard_description["Version"]["DataSetArns"]:
            dashboard_id = dashboard_description["DashboardId"]
            if data_set_arn in data_set_id_by_arn:
                converted_dashboard_for_data_set = {DATA_SET_ID: data_set_id_by_arn[data_set_arn],
                                                    DASHBOARD_ID: dashboard_id}
                converted_dashboards_for_data_sets.append(converted_dashboard_for_data_set)
            else:
                print("Dashboard DashboardId=" + dashboard_id + " Name=\"" + dashboard_description["Name"] + "\" has reference to non-existent data set " + str(data_set_arn))
    return converted_dashboards_for_data_sets

def convert_data_sources(data_sources):
    converted_data_sources = []
    for data_source in data_sources:
        converted_data_source = {DATA_SOURCE_ID: data_source["DataSourceId"],
                                 DATA_SOURCE_NAME: data_source.get("Name", None)}
        converted_data_sources.append(converted_data_source)
    return converted_data_sources

def convert_analyses(analyses):
    converted_analyses = []
    for analysis in analyses:
        converted_analysis = {ANALYSIS_ID: analysis["AnalysisId"],
                              ANALYSIS_NAME: analysis.get("Name", None)}
        converted_analyses.append(converted_analysis)
    return converted_analyses

def convert_analyses_for_data_sets(data_sets, analysis_descriptions):
    data_set_id_by_arn = {}
    for data_set in data_sets:
        data_set_id_by_arn[data_set["Arn"]] = data_set["DataSetId"]

    converted_analyses_for_data_sets = []
    for analysis_description in analysis_descriptions.values():
        for data_set_arn in analysis_description["DataSetArns"]:
            analysis_id = analysis_description["AnalysisId"]
            if data_set_arn in data_set_id_by_arn:
                converted_analysis_for_data_set = {DATA_SET_ID: data_set_id_by_arn[data_set_arn],
                                                   ANALYSIS_ID: analysis_id}
                converted_analyses_for_data_sets.append(converted_analysis_for_data_set)
            else:
                print("Analysis AnalysisId=" + analysis_id + " Name=\"" + analysis_description["Name"] + "\" has reference to non-existent data set " + str(data_set_arn))
    return converted_analyses_for_data_sets

def convert_users_for_data_sets(data_set_permissions, users):
    user_by_arn = {}
    for user in users:
        user_by_arn[user["Arn"]] = user

    converted_users_for_data_sets = []
    for data_set_permission in data_set_permissions.values():
        for permission in data_set_permission["Permissions"]:
            data_set_id = data_set_permission["DataSetId"]
            principal = permission["Principal"]
            if principal in user_by_arn:
                converted_user_for_data_set = {DATA_SET_ID: data_set_id,
                                               USER_ARN: principal}
                converted_users_for_data_sets.append(converted_user_for_data_set)
            else:
                print("Data set permission DataSetId=" + data_set_id + " has reference to non-existent principal " + str(principal))
    return converted_users_for_data_sets

def write_data(type, columns, rows):
    csv_file_name = "analytics-quicksight-" + type + ".csv"
    file_path = "/tmp/" + csv_file_name

    try:
        with open(file_path, 'w') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=columns)
            writer.writeheader()
            for row in rows:
                data = {}
                for column in columns:
                    data[column] = row.get(column, "")
                writer.writerow(data)

        with open(file_path) as f:
            string = f.read()
            encoded_string = string.encode("utf-8")

        env_name = os.environ["env"]
        bucket_name = env_name + "-analytics-quicksight-" + type
        s3_path = csv_file_name
        s3 = boto3.resource("s3")
        s3.Bucket(bucket_name).put_object(Key=s3_path, Body=encoded_string)

    except IOError as error:
        print(error)

def handler(event, context):
    sts = boto3.client('sts')

    account_id = sts.get_caller_identity()["Account"]

    data_sets = get_data_sets(account_id)
    dashboards = get_dashboards(account_id)
    data_sources = get_data_sources(account_id)
    analyses = get_analyses(account_id)
    users = get_users(account_id)
    data_set_descriptions = get_data_set_descriptions(account_id, data_sets)
    dashboard_descriptions = get_dashboard_descriptions(account_id, dashboards)
    analysis_descriptions = get_analysis_descriptions(account_id, analyses)
    data_set_permissions = get_data_set_permissions(account_id, data_sets)
    ingestions = get_data_set_ingestions(account_id, data_sets)

    converted_data_sets = convert_data_sets(data_sets, data_set_descriptions, data_sources)
    converted_ingestions = convert_ingestions(data_sets, ingestions)
    converted_dashboards = convert_dashboards(dashboards, dashboard_descriptions)
    converted_dashboards_for_data_sets = convert_dashboards_for_data_sets(data_sets, dashboard_descriptions)
    converted_data_sources = convert_data_sources(data_sources)
    converted_analyses = convert_analyses(analyses)
    converted_analyses_for_data_sets = convert_analyses_for_data_sets(data_sets, analysis_descriptions)
    converted_users_for_data_sets = convert_users_for_data_sets(data_set_permissions, users)

    write_data("data-sets", DATA_SET_COLUMNS, converted_data_sets)
    write_data("ingestions", INGESTION_COLUMNS, converted_ingestions)
    write_data("dashboards", DASHBOARD_COLUMNS, converted_dashboards)
    write_data("dashboards-for-data-sets", DASHBOARDS_FOR_DATA_SETS_COLUMNS, converted_dashboards_for_data_sets)
    write_data("data-sources", DATA_SOURCE_COLUMNS, converted_data_sources)
    write_data("analyses", ANALYSIS_COLUMNS, converted_analyses)
    write_data("analyses-for-data-sets", ANALYSES_FOR_DATA_SETS_COLUMNS, converted_analyses_for_data_sets)
    write_data("users-for-data-sets", USERS_FOR_DATA_SETS_COLUMNS, converted_users_for_data_sets)

handler("", "")
