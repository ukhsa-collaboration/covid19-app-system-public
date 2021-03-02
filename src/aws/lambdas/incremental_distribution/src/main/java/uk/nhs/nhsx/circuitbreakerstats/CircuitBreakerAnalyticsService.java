package uk.nhs.nhsx.circuitbreakerstats;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.GetQueryResultsRequest;
import com.amazonaws.services.logs.model.GetQueryResultsResult;
import com.amazonaws.services.logs.model.ResultField;
import com.amazonaws.services.logs.model.StartQueryRequest;
import com.amazonaws.services.logs.model.StartQueryResult;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ByteArraySource;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.events.Events;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

public class CircuitBreakerAnalyticsService {
    private static final LocalTime START_WINDOW = LocalTime.of(1, 0);
    private static final LocalTime END_WINDOW = LocalTime.of(1, 10);

    private final Supplier<Instant> systemClock;
    private final AWSLogs client;
    private final String logGroup;
    private final S3Storage s3Storage;
    private final String bucketName;
    private final ObjectKeyNameProvider objectKeyNameProvider;
    private final boolean shouldAbortIfOutsideWindow;
    private final Events events;

    public CircuitBreakerAnalyticsService(Supplier<Instant> clock,
                                          AWSLogs client,
                                          String logGroup,
                                          S3Storage s3Storage,
                                          String bucketName,
                                          ObjectKeyNameProvider objectKeyNameProvider,
                                          Boolean shouldAbortIfOutsideWindow,
                                          Events events) {
        this.systemClock = clock;
        this.client = client;
        this.logGroup = logGroup;
        this.s3Storage = s3Storage;
        this.bucketName = bucketName;
        this.objectKeyNameProvider = objectKeyNameProvider;
        this.shouldAbortIfOutsideWindow = shouldAbortIfOutsideWindow;
        this.events = events;
    }

    public static boolean isInWindow(Instant time) {
        LocalTime current = time.atZone(ZoneOffset.UTC).toLocalTime();
        return (current.equals(START_WINDOW) || current.isAfter(START_WINDOW)) && current.isBefore(END_WINDOW);
    }

    public void generateStatsAndUploadToS3(String queryString) {
        List<List<ResultField>> logs = executeCloudWatchInsightQuery(queryString);
        if (logs.isEmpty()) {
            events.emit(getClass(), new EmptyCircuitBreakerAnalyticsLogs());
        } else {
            uploadToS3(convertToJSON(logs));
        }
    }

    public List<List<ResultField>> executeCloudWatchInsightQuery(String queryString) {
        ServiceWindow window = new ServiceWindow(systemClock.get());
        if (!isInWindow(systemClock.get())) {
            if (shouldAbortIfOutsideWindow) {
                throw new IllegalStateException("CloudWatch Event triggered Lambda at wrong time.");
            }
        }

        StartQueryRequest startQueryRequest = new StartQueryRequest()
            .withQueryString(queryString)
            .withLogGroupName(logGroup)
            .withStartTime(window.queryStart())
            .withEndTime(window.queryEnd());

        StartQueryResult startQueryResult = client.startQuery(startQueryRequest);
        return getQueryResults(startQueryResult.getQueryId()).getResults();
    }

    private GetQueryResultsResult getQueryResults(String queryId) {
        GetQueryResultsRequest queryResultsRequest = new GetQueryResultsRequest().withQueryId(queryId);
        GetQueryResultsResult getQueryResult = client.getQueryResults(queryResultsRequest);

        while (getQueryResult.getStatus().equals("Running")) {
            try {
                Thread.sleep(1000L);
                getQueryResult = client.getQueryResults(queryResultsRequest);
            } catch (InterruptedException e) {
                events.emit(getClass(), new CircuitBreakerAnalyticsPollingFailed(e));
            }
        }
        return getQueryResult;
    }

    public String convertToJSON(List<List<ResultField>> logs) {
        StringBuilder json = new StringBuilder();
        List<CircuitBreakerStats> stats = CircuitBreakerStats.transformFrom(logs);
        for (CircuitBreakerStats stat : stats) {
            json.append(Jackson.toJson(stat)).append("\n");
        }
        return json.toString();
    }

    public void uploadToS3(String json) {
        DateTimeFormatter DATE_TIME_FORMATTER_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.systemDefault());
        var yesterdayDateSlash = DATE_TIME_FORMATTER_SLASH.format(systemClock.get().minus(1, ChronoUnit.DAYS));
        var objectKey = ObjectKey.of(yesterdayDateSlash + "/").append(String.valueOf(objectKeyNameProvider.generateObjectKeyName())).append(".json");
        s3Storage.upload(
            Locator.of(BucketName.of(bucketName), objectKey),
            ContentType.APPLICATION_JSON,
            ByteArraySource.fromUtf8String(json)
        );
    }

}
