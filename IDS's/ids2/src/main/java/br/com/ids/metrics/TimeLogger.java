package br.com.ids.metrics;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class TimeLogger {
    private static final DateTimeFormatter BR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Map<String, Instant> startStep = new HashMap<>();
    private static final Map<String, Instant> endStep = new HashMap<>();

    public static void start(String step) {
        Instant start = Instant.now();
        startStep.put(step, start);
        LocalDateTime startTime = LocalDateTime.ofInstant(start, ZoneId.systemDefault());
        System.out.println("\t[" + step + "] Start Time: " + startTime.format(BR_FORMAT));
    }

    public static void stop(String step) {
        Instant end = Instant.now();
        endStep.put(step, end);
        LocalDateTime endTime = LocalDateTime.ofInstant(end, ZoneId.systemDefault());
        System.out.println("\t[" + step + "] End Time: " + endTime.format(BR_FORMAT));
//        printDuration(step, ChronoUnit.MILLIS);
    }

    private void printDuration(String step, ChronoUnit timeUnit) {
        long duration = getDuration(step, timeUnit);
        if (duration >= 0) {
            System.out.println("\t[" + step + "] Total Duration: " + duration + " " + timeUnit.toString().toLowerCase());
        }
    }

    public static Instant getStart(String step) {
        return startStep.get(step);
    }

    public Instant getEnd(String step) {
        return endStep.get(step);
    }

    public static long getDuration(String step, ChronoUnit timeUnit) {
        Instant start = startStep.get(step);
        Instant end = endStep.get(step);
        if (start != null && end != null) {
            return timeUnit.between(start, end);
        }
        return -1;
    }
}

