package uk.nhs.nhsx.circuitbreakerstats;

import com.amazonaws.services.logs.model.ResultField;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class CircuitBreakerStats {
    public final String startOfHour;
    public final int exposureNotificationCBCount;
    public final int iOSExposureNotificationCBCount;
    public final int androidExposureNotificationCBCount;
    public final int uniqueRequestIds;


    @JsonCreator
    public CircuitBreakerStats(String startOfHour, int exposureNotificationCBCount, int iOSExposureNotificationCBCount, int androidExposureNotificationCBCount, int uniqueRequestIds) {
        this.startOfHour = startOfHour;
        this.exposureNotificationCBCount = exposureNotificationCBCount;
        this.iOSExposureNotificationCBCount = iOSExposureNotificationCBCount;
        this.androidExposureNotificationCBCount = androidExposureNotificationCBCount;
        this.uniqueRequestIds = uniqueRequestIds;
    }

    public static List<CircuitBreakerStats> transformFrom(List<List<ResultField>> logRows) {
        return logRows.stream().map(CircuitBreakerStats::convert).collect(toList());

    }

    static CircuitBreakerStats convert(List<ResultField> row) {
        Map<String, String> map = listOfColumnsToMap(row);
        return new CircuitBreakerStats(
            map.get("start_of_hour"),
            Integer.parseInt(map.getOrDefault("exposure_notification_cb_count", "0")),
            Integer.parseInt(map.getOrDefault("iOS_exposure_notification_cb_count", "0")),
            Integer.parseInt(map.getOrDefault("android_exposure_notification_cb_count", "0")),
            Integer.parseInt(map.getOrDefault("unique_request_ids", "0")));
    }

    private static Map<String, String> listOfColumnsToMap(List<ResultField> row) {
        Map<String, String> map = new HashMap<>();
        for (ResultField resultField : row) {
            if (map.put(resultField.getField(), resultField.getValue()) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return map;
    }
}
