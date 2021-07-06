import sys
from awsglue.transforms import *
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from pyspark.sql.functions import regexp_extract
from awsglue.context import GlueContext
from awsglue.job import Job
from awsglue.dynamicframe import DynamicFrameCollection
from awsglue.dynamicframe import DynamicFrame

def MyTransform(glueContext, dfc) -> DynamicFrameCollection:
    df = dfc.select(list(dfc.keys())[0]).toDF()

    from pyspark.sql.functions import input_file_name
    from pyspark.sql.functions import regexp_extract
    df = df.withColumn("filename",
                       input_file_name())  # non-null for 1st run (lots of files), null for 2nd run (few files)
    df = df.withColumn("submitteddatehour",
                       regexp_extract(df['filename'], r".+analytics-.+(202[01]-..-..-..).*parquet", 1))

    df = df.repartition("submitteddatehour")

    dyf = DynamicFrame.fromDF(df, glueContext, "submitteddatehour-extracted")

    return (DynamicFrameCollection({"CustomTransform0": dyf}, glueContext))


## @params: [JOB_NAME, SOURCE_BUCKET_URI, DESTINATION_BUCKET_URI]
args = getResolvedOptions(sys.argv, ['JOB_NAME', 'SOURCE_BUCKET_URI', 'DESTINATION_BUCKET_URI'])

sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
glueContext.sql("set spark.sql.parquet.mergeSchema=true")
job = Job(glueContext)
job.init(args['JOB_NAME'], args)
## @type: DataSource
## @args: [connection_type = "s3", format = "parquet", connection_options = {"paths": ["s3://te-load-test-analytics-submission-parquet/"], "recurse":True}, transformation_ctx = "DataSource0"]
## @return: DataSource0
## @inputs: []
DataSource0 = glueContext.create_dynamic_frame.from_options(connection_type = "s3", format = "parquet", connection_options = {"paths": [f"{args['SOURCE_BUCKET_URI']}/"], "recurse":True}, transformation_ctx = "DataSource0")
## @type: CustomCode
## @args: [dynamicFrameConstruction = DynamicFrameCollection({"DataSource0": DataSource0}, glueContext), className = MyTransform, transformation_ctx = "Transform0"]
## @return: Transform0
## @inputs: [dfc = DataSource0]
Transform0 = MyTransform(glueContext, DynamicFrameCollection({"DataSource0": DataSource0}, glueContext))
## @type: SelectFromCollection
## @args: [key = list(Transform0.keys())[0], transformation_ctx = "Transform1"]
## @return: Transform1
## @inputs: [dfc = Transform0]
Transform1 = SelectFromCollection.apply(dfc = Transform0, key = list(Transform0.keys())[0], transformation_ctx = "Transform1")
## @type: ApplyMapping
## @args: [mappings = [("submitteddatehour", "string", "submitteddatehour", "string"), ("startdate", "string", "startdate", "string"), ("enddate", "string", "enddate", "string"), ("postaldistrict", "string", "postaldistrict", "string"), ("localauthority", "string", "localauthority", "string"), ("devicemodel", "string", "devicemodel", "string"), ("latestapplicationversion", "string", "latestapplicationversion", "string"), ("operatingsystemversion", "string", "operatingsystemversion", "string"), ("cumulativedownloadbytes", "int", "cumulativedownloadbytes", "int"), ("cumulativeuploadbytes", "int", "cumulativeuploadbytes", "int"), ("cumulativecellulardownloadbytes", "int", "cumulativecellulardownloadbytes", "int"), ("cumulativecellularuploadbytes", "int", "cumulativecellularuploadbytes", "int"), ("cumulativewifidownloadbytes", "int", "cumulativewifidownloadbytes", "int"), ("cumulativewifiuploadbytes", "int", "cumulativewifiuploadbytes", "int"), ("checkedin", "int", "checkedin", "int"), ("canceledcheckin", "int", "canceledcheckin", "int"), ("receivedvoidtestresult", "int", "receivedvoidtestresult", "int"), ("isisolatingbackgroundtick", "int", "isisolatingbackgroundtick", "int"), ("hashadriskycontactbackgroundtick", "int", "hashadriskycontactbackgroundtick", "int"), ("receivedpositivetestresult", "int", "receivedpositivetestresult", "int"), ("receivednegativetestresult", "int", "receivednegativetestresult", "int"), ("hasselfdiagnosedpositivebackgroundtick", "int", "hasselfdiagnosedpositivebackgroundtick", "int"), ("completedquestionnaireandstartedisolation", "int", "completedquestionnaireandstartedisolation", "int"), ("encounterdetectionpausedbackgroundtick", "int", "encounterdetectionpausedbackgroundtick", "int"), ("completedquestionnairebutdidnotstartisolation", "int", "completedquestionnairebutdidnotstartisolation", "int"), ("totalbackgroundtasks", "int", "totalbackgroundtasks", "int"), ("runningnormallybackgroundtick", "int", "runningnormallybackgroundtick", "int"), ("completedonboarding", "int", "completedonboarding", "int"), ("includesmultipleapplicationversions", "boolean", "includesmultipleapplicationversions", "boolean"), ("receivedvoidtestresultenteredmanually", "int", "receivedvoidtestresultenteredmanually", "int"), ("receivedpositivetestresultenteredmanually", "int", "receivedpositivetestresultenteredmanually", "int"), ("receivednegativetestresultenteredmanually", "int", "receivednegativetestresultenteredmanually", "int"), ("receivedvoidtestresultviapolling", "int", "receivedvoidtestresultviapolling", "int"), ("receivedpositivetestresultviapolling", "int", "receivedpositivetestresultviapolling", "int"), ("receivednegativetestresultviapolling", "int", "receivednegativetestresultviapolling", "int"), ("hasselfdiagnosedbackgroundtick", "int", "hasselfdiagnosedbackgroundtick", "int"), ("hastestedpositivebackgroundtick", "int", "hastestedpositivebackgroundtick", "int"), ("isisolatingforselfdiagnosedbackgroundtick", "int", "isisolatingforselfdiagnosedbackgroundtick", "int"), ("isisolatingfortestedpositivebackgroundtick", "int", "isisolatingfortestedpositivebackgroundtick", "int"), ("isisolatingforhadriskycontactbackgroundtick", "int", "isisolatingforhadriskycontactbackgroundtick", "int"), ("receivedriskycontactnotification", "int", "receivedriskycontactnotification", "int"), ("startedisolation", "int", "startedisolation", "int"), ("receivedpositivetestresultwhenisolatingduetoriskycontact", "int", "receivedpositivetestresultwhenisolatingduetoriskycontact", "int"), ("receivedactiveipctoken", "int", "receivedactiveipctoken", "int"), ("haveactiveipctokenbackgroundtick", "int", "haveactiveipctokenbackgroundtick", "int"), ("selectedisolationpaymentsbutton", "int", "selectedisolationpaymentsbutton", "int"), ("launchedisolationpaymentsapplication", "int", "launchedisolationpaymentsapplication", "int"), ("receivedpositivelfdtestresultviapolling", "int", "receivedpositivelfdtestresultviapolling", "int"), ("receivednegativelfdtestresultviapolling", "int", "receivednegativelfdtestresultviapolling", "int"), ("receivedvoidlfdtestresultviapolling", "int", "receivedvoidlfdtestresultviapolling", "int"), ("receivedpositivelfdtestresultenteredmanually", "int", "receivedpositivelfdtestresultenteredmanually", "int"), ("receivednegativelfdtestresultenteredmanually", "int", "receivednegativelfdtestresultenteredmanually", "int"), ("receivedvoidlfdtestresultenteredmanually", "int", "receivedvoidlfdtestresultenteredmanually", "int"), ("hastestedlfdpositivebackgroundtick", "int", "hastestedlfdpositivebackgroundtick", "int"), ("isisolatingfortestedlfdpositivebackgroundtick", "int", "isisolatingfortestedlfdpositivebackgroundtick", "int"), ("totalexposurewindowsnotconsideredrisky", "int", "totalexposurewindowsnotconsideredrisky", "int"), ("totalexposurewindowsconsideredrisky", "int", "totalexposurewindowsconsideredrisky", "int"), ("acknowledgedstartofisolationduetoriskycontact", "int", "acknowledgedstartofisolationduetoriskycontact", "int"), ("hasriskycontactnotificationsenabledbackgroundtick", "int", "hasriskycontactnotificationsenabledbackgroundtick", "int"), ("totalriskycontactremindernotifications", "int", "totalriskycontactremindernotifications", "int"), ("receivedunconfirmedpositivetestresult", "int", "receivedunconfirmedpositivetestresult", "int"), ("isisolatingforunconfirmedtestbackgroundtick", "int", "isisolatingforunconfirmedtestbackgroundtick", "int"), ("launchedtestordering", "int", "launchedtestordering", "int"), ("didhavesymptomsbeforereceivedtestresult", "int", "didhavesymptomsbeforereceivedtestresult", "int"), ("didrememberonsetsymptomsdatebeforereceivedtestresult", "int", "didrememberonsetsymptomsdatebeforereceivedtestresult", "int"), ("didaskforsymptomsonpositivetestentry", "int", "didaskforsymptomsonpositivetestentry", "int"), ("declarednegativeresultfromdct", "int", "declarednegativeresultfromdct", "int"), ("receivedpositiveselfrapidtestresultviapolling", "int", "receivedpositiveselfrapidtestresultviapolling", "int"), ("receivednegativeselfrapidtestresultviapolling", "int", "receivednegativeselfrapidtestresultviapolling", "int"), ("receivedvoidselfrapidtestresultviapolling", "int", "receivedvoidselfrapidtestresultviapolling", "int"), ("receivedpositiveselfrapidtestresultenteredmanually", "int", "receivedpositiveselfrapidtestresultenteredmanually", "int"), ("receivednegativeselfrapidtestresultenteredmanually", "int", "receivednegativeselfrapidtestresultenteredmanually", "int"), ("receivedvoidselfrapidtestresultenteredmanually", "int", "receivedvoidselfrapidtestresultenteredmanually", "int"), ("isisolatingfortestedselfrapidpositivebackgroundtick", "int", "isisolatingfortestedselfrapidpositivebackgroundtick", "int"), ("hastestedselfrapidpositivebackgroundtick", "int", "hastestedselfrapidpositivebackgroundtick", "int"), ("receivedriskyvenuem1warning", "int", "receivedriskyvenuem1warning", "int"), ("receivedriskyvenuem2warning", "int", "receivedriskyvenuem2warning", "int"), ("hasreceivedriskyvenuem2warningbackgroundtick", "int", "hasreceivedriskyvenuem2warningbackgroundtick", "int"), ("totalalarmmanagerbackgroundtasks", "int", "totalalarmmanagerbackgroundtasks", "int"), ("missingpacketslast7days", "int", "missingpacketslast7days", "int"), ("consentedtosharevenuehistory", "int", "consentedtosharevenuehistory", "int"), ("askedtosharevenuehistory", "int", "askedtosharevenuehistory", "int")], transformation_ctx = "Transform2"]
## @return: Transform2
## @inputs: [frame = Transform1]
Transform2 = ApplyMapping.apply(frame=Transform1,
                                mappings=[("submitteddatehour", "string",
                                           "submitteddatehour", "string"),
                                          ("startdate", "string",
                                           "startdate", "string"),
                                          ("enddate", "string",
                                           "enddate", "string"),
                                          ("postaldistrict", "string",
                                           "postaldistrict", "string"),
                                          ("localauthority", "string",
                                           "localauthority", "string"),
                                          ("devicemodel", "string",
                                           "devicemodel", "string"),
                                          ("latestapplicationversion", "string",
                                           "latestapplicationversion", "string"),
                                          ("operatingsystemversion", "string",
                                           "operatingsystemversion", "string"),
                                          ("cumulativedownloadbytes", "int",
                                           "cumulativedownloadbytes", "int"),
                                          ("cumulativeuploadbytes", "int",
                                           "cumulativeuploadbytes", "int"),
                                          ("cumulativecellulardownloadbytes", "int",
                                           "cumulativecellulardownloadbytes","int"),
                                          ("cumulativecellularuploadbytes", "int",
                                           "cumulativecellularuploadbytes","int"),
                                          ("cumulativewifidownloadbytes", "int",
                                           "cumulativewifidownloadbytes", "int"),
                                          ("cumulativewifiuploadbytes", "int",
                                           "cumulativewifiuploadbytes", "int"),
                                          ("checkedin", "int",
                                           "checkedin", "int"),
                                          ("canceledcheckin", "int",
                                           "canceledcheckin", "int"),
                                          ("receivedvoidtestresult", "int",
                                           "receivedvoidtestresult", "int"),
                                          ("isisolatingbackgroundtick", "int",
                                           "isisolatingbackgroundtick", "int"),
                                          ("hashadriskycontactbackgroundtick", "int",
                                           "hashadriskycontactbackgroundtick","int"),
                                          ("receivedpositivetestresult", "int",
                                           "receivedpositivetestresult", "int"),
                                          ("receivednegativetestresult", "int",
                                           "receivednegativetestresult", "int"),
                                          ("hasselfdiagnosedpositivebackgroundtick", "int",
                                           "hasselfdiagnosedpositivebackgroundtick", "int"),
                                          ("completedquestionnaireandstartedisolation", "int",
                                           "completedquestionnaireandstartedisolation", "int"),
                                          ("encounterdetectionpausedbackgroundtick", "int",
                                           "encounterdetectionpausedbackgroundtick", "int"),
                                          ("completedquestionnairebutdidnotstartisolation", "int",
                                           "completedquestionnairebutdidnotstartisolation", "int"),
                                          ("totalbackgroundtasks", "int",
                                           "totalbackgroundtasks", "int"),
                                          ("runningnormallybackgroundtick", "int",
                                           "runningnormallybackgroundtick", "int"),
                                          ("completedonboarding", "int",
                                           "completedonboarding", "int"),
                                          ("includesmultipleapplicationversions", "boolean",
                                           "includesmultipleapplicationversions", "boolean"),
                                          ("receivedvoidtestresultenteredmanually", "int",
                                           "receivedvoidtestresultenteredmanually", "int"),
                                          ("receivedpositivetestresultenteredmanually", "int",
                                           "receivedpositivetestresultenteredmanually", "int"),
                                          ("receivednegativetestresultenteredmanually", "int",
                                           "receivednegativetestresultenteredmanually", "int"),
                                          ("receivedvoidtestresultviapolling", "int",
                                           "receivedvoidtestresultviapolling", "int"),
                                          ("receivedpositivetestresultviapolling", "int",
                                           "receivedpositivetestresultviapolling", "int"),
                                          ("receivednegativetestresultviapolling", "int",
                                           "receivednegativetestresultviapolling", "int"),
                                          ("hasselfdiagnosedbackgroundtick", "int",
                                           "hasselfdiagnosedbackgroundtick", "int"),
                                          ("hastestedpositivebackgroundtick", "int",
                                           "hastestedpositivebackgroundtick", "int"),
                                          ("isisolatingforselfdiagnosedbackgroundtick", "int",
                                           "isisolatingforselfdiagnosedbackgroundtick", "int"),
                                          ("isisolatingfortestedpositivebackgroundtick", "int",
                                           "isisolatingfortestedpositivebackgroundtick", "int"),
                                          ("isisolatingforhadriskycontactbackgroundtick", "int",
                                           "isisolatingforhadriskycontactbackgroundtick", "int"),
                                          ("receivedriskycontactnotification", "int",
                                           "receivedriskycontactnotification", "int"),
                                          ("startedisolation", "int",
                                           "startedisolation", "int"),
                                          ("receivedpositivetestresultwhenisolatingduetoriskycontact", "int",
                                           "receivedpositivetestresultwhenisolatingduetoriskycontact", "int"),
                                          ("receivedactiveipctoken", "int",
                                           "receivedactiveipctoken", "int"),
                                          ("haveactiveipctokenbackgroundtick", "int",
                                           "haveactiveipctokenbackgroundtick", "int"),
                                          ("selectedisolationpaymentsbutton", "int",
                                           "selectedisolationpaymentsbutton", "int"),
                                          ("launchedisolationpaymentsapplication", "int",
                                           "launchedisolationpaymentsapplication", "int"), 
                                          ("receivedpositivelfdtestresultviapolling", "int",
                                           "receivedpositivelfdtestresultviapolling", "int"), 
                                          ("receivednegativelfdtestresultviapolling", "int",
                                           "receivednegativelfdtestresultviapolling", "int"), 
                                          ("receivedvoidlfdtestresultviapolling", "int",
                                           "receivedvoidlfdtestresultviapolling", "int"), 
                                          ("receivedpositivelfdtestresultenteredmanually", "int",
                                           "receivedpositivelfdtestresultenteredmanually", "int"), 
                                          ("receivednegativelfdtestresultenteredmanually", "int",
                                           "receivednegativelfdtestresultenteredmanually", "int"), 
                                          ("receivedvoidlfdtestresultenteredmanually", "int",
                                           "receivedvoidlfdtestresultenteredmanually", "int"), 
                                          ("hastestedlfdpositivebackgroundtick", "int",
                                           "hastestedlfdpositivebackgroundtick", "int"), 
                                          ("isisolatingfortestedlfdpositivebackgroundtick", "int",
                                           "isisolatingfortestedlfdpositivebackgroundtick", "int"), 
                                          ("totalexposurewindowsnotconsideredrisky", "int",
                                           "totalexposurewindowsnotconsideredrisky", "int"), 
                                          ("totalexposurewindowsconsideredrisky", "int",
                                           "totalexposurewindowsconsideredrisky", "int"), 
                                          ("acknowledgedstartofisolationduetoriskycontact", "int",
                                           "acknowledgedstartofisolationduetoriskycontact", "int"), 
                                          ("hasriskycontactnotificationsenabledbackgroundtick", "int",
                                           "hasriskycontactnotificationsenabledbackgroundtick", "int"), 
                                          ("totalriskycontactremindernotifications", "int",
                                           "totalriskycontactremindernotifications", "int"), 
                                          ("receivedunconfirmedpositivetestresult", "int",
                                           "receivedunconfirmedpositivetestresult", "int"), 
                                          ("isisolatingforunconfirmedtestbackgroundtick", "int",
                                           "isisolatingforunconfirmedtestbackgroundtick", "int"),
                                          ("launchedtestordering", "int",
                                           "launchedtestordering", "int"),
                                          ("didhavesymptomsbeforereceivedtestresult", "int",
                                           "didhavesymptomsbeforereceivedtestresult", "int"), 
                                          ("didrememberonsetsymptomsdatebeforereceivedtestresult", "int",
                                           "didrememberonsetsymptomsdatebeforereceivedtestresult", "int"), 
                                          ("didaskforsymptomsonpositivetestentry", "int",
                                           "didaskforsymptomsonpositivetestentry", "int"), 
                                          ("declarednegativeresultfromdct", "int",
                                           "declarednegativeresultfromdct", "int"),
                                          ("receivedpositiveselfrapidtestresultviapolling", "int",
                                           "receivedpositiveselfrapidtestresultviapolling", "int"),
                                          ("receivednegativeselfrapidtestresultviapolling", "int",
                                           "receivednegativeselfrapidtestresultviapolling", "int"), 
                                          ("receivedvoidselfrapidtestresultviapolling", "int",
                                           "receivedvoidselfrapidtestresultviapolling", "int"), 
                                          ("receivedpositiveselfrapidtestresultenteredmanually", "int",
                                           "receivedpositiveselfrapidtestresultenteredmanually", "int"), 
                                          ("receivednegativeselfrapidtestresultenteredmanually", "int",
                                           "receivednegativeselfrapidtestresultenteredmanually", "int"), 
                                          ("receivedvoidselfrapidtestresultenteredmanually", "int",
                                           "receivedvoidselfrapidtestresultenteredmanually", "int"), 
                                          ("isisolatingfortestedselfrapidpositivebackgroundtick", "int",
                                           "isisolatingfortestedselfrapidpositivebackgroundtick", "int"), 
                                          ("hastestedselfrapidpositivebackgroundtick", "int",
                                           "hastestedselfrapidpositivebackgroundtick", "int"),
                                          ("receivedriskyvenuem1warning", "int",
                                           "receivedriskyvenuem1warning", "int"),
                                          ("receivedriskyvenuem2warning", "int",
                                           "receivedriskyvenuem2warning", "int"),
                                          ("hasreceivedriskyvenuem2warningbackgroundtick", "int",
                                           "hasreceivedriskyvenuem2warningbackgroundtick", "int"), 
                                          ("totalalarmmanagerbackgroundtasks", "int",
                                           "totalalarmmanagerbackgroundtasks", "int"),
                                          ("missingpacketslast7days", "int",
                                           "missingpacketslast7days", "int"),
                                          ("consentedtosharevenuehistory", "int",
                                           "consentedtosharevenuehistory", "int"),
                                          ("askedtosharevenuehistory", "int",
                                           "askedtosharevenuehistory", "int"),
                                          ("askedtoshareexposurekeysintheinitialflow", "int",
                                           "askedtoshareexposurekeysintheinitialflow", "int"),
                                          ("consentedtoshareexposurekeysintheinitialflow", "int",
                                           "consentedtoshareexposurekeysintheinitialflow", "int"),
                                          ("totalshareexposurekeysremindernotifications", "int",
                                           "totalshareexposurekeysremindernotifications", "int"),
                                          ("consentedtoshareexposurekeysinreminderscreen", "int",
                                           "consentedtoshareexposurekeysinreminderscreen", "int"),
                                          ("successfullysharedexposurekeys", "int",
                                           "successfullysharedexposurekeys", "int"),
                                          ("didSendLocalInfoNotification", "int",
                                           "didSendLocalInfoNotification", "int"),
                                          ("didAccessLocalInfoScreenViaNotification", "int",
                                           "didAccessLocalInfoScreenViaNotification", "int"),
                                          ("didAccessLocalInfoScreenViaBanner", "int",
                                           "didAccessLocalInfoScreenViaBanner", "int"),
                                          ("isDisplayingLocalInfoBackgroundTick", "int",
                                           "isDisplayingLocalInfoBackgroundTick", "int"),
                                          ("positiveLabResultAfterPositiveLFD", "int",
                                           "positiveLabResultAfterPositiveLFD", "int"),
                                          ("negativeLabResultAfterPositiveLFDWithinTimeLimit", "int",
                                           "negativeLabResultAfterPositiveLFDWithinTimeLimit", "int"),
                                          ("negativeLabResultAfterPositiveLFDOutsideTimeLimit", "int",
                                           "negativeLabResultAfterPositiveLFDOutsideTimeLimit", "int"),
                                          ("positiveLabResultAfterPositiveSelfRapidTest", "int",
                                           "positiveLabResultAfterPositiveSelfRapidTest", "int"),
                                          ("negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit", "int",
                                           "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit", "int"),
                                          ("negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit", "int",
                                           "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit", "int")],
                                transformation_ctx="Transform2")
## @type: DataSink
## @args: [connection_type = "s3", format = "parquet", connection_options = {"path": "s3://te-staging-analytics-consolidated-submission-parquet/", "partitionKeys": ["submitteddatehour"]}, transformation_ctx = "DataSink0"]
## @return: DataSink0
## @inputs: [frame = Transform2]
DataSink0 = glueContext.write_dynamic_frame.from_options(frame=Transform2, connection_type="s3", format="parquet",
                                                      connection_options={
                                                          "path": f"{args['DESTINATION_BUCKET_URI']}/",
                                                          "partitionKeys": ["submitteddatehour"]},
                                                      transformation_ctx="DataSink0")
job.commit()
