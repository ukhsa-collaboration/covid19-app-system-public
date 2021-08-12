import argparse
import csv
import uuid
from datetime import date, datetime, timedelta

import boto3
import pandas
import pyarrow as pa
import pyarrow.parquet as pq

LOCATIONS = {
    'wales': {
        'local_authority': 'W06000015',
        'postal_district': 'CF24'
    },
    'england': {
        'local_authority': 'E09000001',
        'postal_district': 'E1'
    }
}


def make_record(start_date, location):
    end_date = start_date + timedelta(days=1)
    return {
        'startdate': f'{datetime(year=start_date.year, month=start_date.month, day=start_date.day).isoformat()}',
        'enddate': f'{datetime(year=end_date.year, month=end_date.month, day=end_date.day).isoformat()}',
        'postaldistrict': location['postal_district'],
        'localauthority': location['local_authority'],
        'devicemodel': 'iPhone10,4',
        'latestapplicationversion': '4.10',
        'operatingsystemversion': '14.4',
        'cumulativedownloadbytes': 1,
        'cumulativeuploadbytes': 1,
        'cumulativecellulardownloadbytes': 1,
        'cumulativecellularuploadbytes': 1,
        'cumulativewifidownloadbytes': 1,
        'cumulativewifiuploadbytes': 1,
        'checkedin': 2,
        'canceledcheckin': 1,
        'receivedvoidtestresult': 1,
        'isisolatingbackgroundtick': 1,
        'hashadriskycontactbackgroundtick': 1,
        'receivedpositivetestresult': 1,
        'receivednegativetestresult': 1,
        'hasselfdiagnosedpositivebackgroundtick': 1,
        'completedquestionnaireandstartedisolation': 1,
        'encounterdetectionpausedbackgroundtick': 1,
        'completedquestionnairebutdidnotstartisolation': 1,
        'totalbackgroundtasks': 1,
        'runningnormallybackgroundtick': 1,
        'completedonboarding': 1,
        'includesmultipleapplicationversions': True,
        'receivedvoidtestresultenteredmanually': 1,
        'receivedpositivetestresultenteredmanually': 1,
        'receivednegativetestresultenteredmanually': 1,
        'receivedvoidtestresultviapolling': 1,
        'receivedpositivetestresultviapolling': 1,
        'receivednegativetestresultviapolling': 1,
        'hasselfdiagnosedbackgroundtick': 1,
        'hastestedpositivebackgroundtick': 1,
        'isisolatingforselfdiagnosedbackgroundtick': 1,
        'isisolatingfortestedpositivebackgroundtick': 1,
        'isisolatingforhadriskycontactbackgroundtick': 1,
        'receivedriskycontactnotification': 1,
        'startedisolation': 1,
        'receivedpositivetestresultwhenisolatingduetoriskycontact': 1,
        'receivedactiveipctoken': 1,
        'haveactiveipctokenbackgroundtick': 1,
        'selectedisolationpaymentsbutton': 1,
        'launchedisolationpaymentsapplication': 1,
        'receivedpositivelfdtestresultviapolling': 1,
        'receivednegativelfdtestresultviapolling': 1,
        'receivedvoidlfdtestresultviapolling': 1,
        'receivedpositivelfdtestresultenteredmanually': 1,
        'receivednegativelfdtestresultenteredmanually': 1,
        'receivedvoidlfdtestresultenteredmanually': 1,
        'hastestedlfdpositivebackgroundtick': 1,
        'isisolatingfortestedlfdpositivebackgroundtick': 1,
        'totalexposurewindowsnotconsideredrisky': 1,
        'totalexposurewindowsconsideredrisky': 1,
        'acknowledgedstartofisolationduetoriskycontact': 1,
        'hasriskycontactnotificationsenabledbackgroundtick': 1,
        'totalriskycontactremindernotifications': 1,
        'receivedunconfirmedpositivetestresult': 1,
        'isisolatingforunconfirmedtestbackgroundtick': 1,
        'launchedtestordering': 1,
        'didhavesymptomsbeforereceivedtestresult': 1,
        'didrememberonsetsymptomsdatebeforereceivedtestresult': 1,
        'didaskforsymptomsonpositivetestentry': 1,
        'declarednegativeresultfromdct': 1,
        'receivedpositiveselfrapidtestresultviapolling': 1,
        'receivednegativeselfrapidtestresultviapolling': 1,
        'receivedvoidselfrapidtestresultviapolling': 1,
        'receivedpositiveselfrapidtestresultenteredmanually': 1,
        'receivednegativeselfrapidtestresultenteredmanually': 1,
        'receivedvoidselfrapidtestresultenteredmanually': 1,
        'isisolatingfortestedselfrapidpositivebackgroundtick': 1,
        'hastestedselfrapidpositivebackgroundtick': 1,
        'receivedriskyvenuem1warning': 1,
        'receivedriskyvenuem2warning': 1,
        'hasreceivedriskyvenuem2warningbackgroundtick': 1,
        'totalalarmmanagerbackgroundtasks': 1,
        'missingpacketslast7days': 1,
        'consentedtosharevenuehistory': 1,
        'askedtosharevenuehistory': 1,
        'askedtoshareexposurekeysintheinitialflow': 1,
        'consentedtoshareexposurekeysintheinitialflow': 1,
        'totalshareexposurekeysremindernotifications': 1,
        'consentedtoshareexposurekeysinreminderscreen': 1,
        'successfullysharedexposurekeys': 1,
        'didsendlocalinfonotification': 1,
        'didaccesslocalinfoscreenvianotification': 1,
        'didaccesslocalinfoscreenviabanner': 1,
        'isdisplayinglocalinfobackgroundtick': 1
    }


def make_and_upload_parquet_files(bucket, date_range, location):
    for start_date in date_range:
        record = make_record(start_date=start_date, location=location)
        df = pandas.DataFrame(record, index=[0])
        pq.write_table(pa.Table.from_pandas(df), 'out/new.parquet')
        s3 = boto3.resource('s3')
        s3.meta.client.upload_file('out/new.parquet',
                                   bucket,
                                   f'{start_date.strftime("%Y/%m/%d")}/23/{uuid.uuid4()}-synthetic.parquet')


def make_and_upload_consolidated_parquet_files(bucket, date_range, location, num_records_per_file):
    for start_date in date_range:
        records = [make_record(start_date=start_date, location=location) for _ in range(num_records_per_file)]
        df = pandas.DataFrame(records)
        pq.write_table(pa.Table.from_pandas(df), 'out/new.parquet')
        s3 = boto3.resource('s3')
        s3.meta.client.upload_file('out/new.parquet',
                                   bucket,
                                   f'submitteddatehour={start_date.strftime("%Y-%m-%d")}-23/{uuid.uuid4()}-synthetic.parquet')


def make_and_upload_app_store_csv_data(bucket, date_range):
    with open('out/app-store-data.csv', 'w', newline='') as csvfile:
        fieldnames = [
            'date',
            'platform',
            'averagerating',
            'downloads',
            'deletes',
            'opt_in_proportion'
        ]
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)

        writer.writeheader()

        for d in date_range:
            writer.writerow({
                'date': d.isoformat(),
                'platform': 'Android',
                'averagerating': 3.24,
                'downloads': 4113,
                'deletes': 426,
                'opt_in_proportion': 1
            })
            writer.writerow({
                'date': d.isoformat(),
                'platform': 'Apple',
                'averagerating': 3.24,
                'downloads': 4113,
                'deletes': 426,
                'opt_in_proportion': 1
            })
            writer.writerow({
                'date': d.isoformat(),
                'platform': 'Website',
                'averagerating': 3.24,
                'downloads': 4113,
                'deletes': 426,
                'opt_in_proportion': 1
            })

    s3 = boto3.resource('s3')
    s3.meta.client.upload_file('out/app-store-data.csv',
                               bucket,
                               'app-store/app-store-data.csv')


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generates dummy parquet files")
    parser.add_argument("--submission_parquet_bucket",
                        required=True,
                        help="Submission parquet s3 bucket")
    parser.add_argument("--consolidated_submission_parquet_bucket",
                        required=True,
                        help="Submission consolidated parquet s3 bucket")
    parser.add_argument("--app_store_data_bucket",
                        required=True,
                        help="App store data s3 bucket")
    args = parser.parse_args()

    submission_parquet_bucket = args.submission_parquet_bucket
    consolidated_submission_parquet_bucket = args.consolidated_submission_parquet_bucket
    app_store_data_bucket = args.app_store_data_bucket

    date_range_days = 90
    num_records_per_parquet_file = 1000
    today = date.today()
    date_list = [today - timedelta(days=x) for x in range(date_range_days)]

    s3_resource = boto3.resource('s3')

    s3_resource.Bucket(submission_parquet_bucket).objects.all().delete()
    make_and_upload_parquet_files(bucket=submission_parquet_bucket, date_range=date_list, location=LOCATIONS['england'])
    make_and_upload_parquet_files(bucket=submission_parquet_bucket, date_range=date_list, location=LOCATIONS['wales'])

    s3_resource.Bucket(consolidated_submission_parquet_bucket).objects.all().delete()
    make_and_upload_consolidated_parquet_files(bucket=consolidated_submission_parquet_bucket,
                                               date_range=date_list,
                                               location=LOCATIONS['england'],
                                               num_records_per_file=num_records_per_parquet_file)
    make_and_upload_consolidated_parquet_files(bucket=consolidated_submission_parquet_bucket,
                                               date_range=date_list,
                                               location=LOCATIONS['wales'],
                                               num_records_per_file=num_records_per_parquet_file)

    s3_resource.Bucket(app_store_data_bucket).objects.all().delete()
    make_and_upload_app_store_csv_data(bucket=app_store_data_bucket, date_range=date_list)

    print('done')
