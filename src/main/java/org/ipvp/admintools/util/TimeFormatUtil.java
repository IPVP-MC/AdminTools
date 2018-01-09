package org.ipvp.admintools.util;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public class TimeFormatUtil {

    // The possible formattable units
    private final static ChronoUnit[] formattableUnit =
            new ChronoUnit[]{ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.WEEKS, ChronoUnit.DAYS,
                    ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS};


    private static class DateFormatter {
        /**
         * Formats a unit of time into an english string
         *
         * @param timeUnit The type of unit to format
         * @param units The number of units of time
         * @return A string formatted
         */
        public String format(ChronoUnit timeUnit, long units) {
            return String.format("%d %s", units, nameOf(timeUnit, units == 1));
        }

        private String nameOf(ChronoUnit unit, boolean singular) {
            String name = unit.name().toLowerCase();
            return singular ? name.substring(0, name.length() - 1) : name;
        }
    }

    private static class ShortenedDateFormatter extends DateFormatter {
        @Override
        public String format(ChronoUnit timeUnit, long units) {
            String prefix = timeUnit != ChronoUnit.MONTHS
                    ? Character.toString(Character.toLowerCase(timeUnit.name().charAt(0)))
                    : "mo";
            return String.format("%d%s", units, prefix);
        }
    }

    /**
     * Parses a textual representation of a duration into it's millisecond
     * value. The provided text must be in the format of
     * {@code <#1>x1<#2>x2...<#n>xn} where the # represents a valid integer
     * value, and x is a valid duration specifier.
     * <p>
     * Valid duration specifiers are:
     * <ul>
     *     <li>d for Days
     *     <li>h for Hours
     *     <li>m for Minutes
     *     <li>s for Seconds
     * </ul>
     *
     * @param duration Duration provided
     * @return The miilliseconds represented by the duration
     */
    public static long parseIntoMilliseconds(String duration) {
        String suffixes = "dhms";
        TimeUnit[] formattableUnit =
                new TimeUnit[] { TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS };
        List<Object> tokens = Collections.list(new StringTokenizer(duration, suffixes, true));
        long time = 0;

        if (tokens.size() <= 1) {
            return -1L;
        }

        for (int i = 1; i < tokens.size(); i += 2) {
            String token = (String) tokens.get(i);
            int suffixIndex = suffixes.indexOf(token.charAt(0));

            try {
                long value = Long.parseLong((String) tokens.get(i - 1));
                time += MILLISECONDS.convert(value, formattableUnit[suffixIndex]);
            } catch (NumberFormatException e) {
                return -1L;
            }
        }

        return time;
    }

    /**
     * Parses a future UNIX time stamp and outputs a detailed text
     * representation of the amount of time left until that date.
     * <p>
     * Text output will result in:
     * <code>{time-unit} time-unit(s)</code>
     * appended for each time-unit until the date is reached.
     *
     * For example: "1 year 2 months 5 days"
     *
     * @param futureDate A UNIX date set in the future
     * @return A formatted timestamp
     */
    public static String toDetailedDate(long futureDate) {
        return toDetailedDate(futureDate, false);
    }

    /**
     * Parses a future UNIX time stamp and outputs a detailed text
     * representation of the amount of time left until that date.
     * <p>
     * Text output will result in <code>{time-unit} time-unit(s)</code>
     * being appended for each time-unit until the date is reached.
     * <p>
     * For example: "1 year 2 months 5 days"
     *
     * @param futureDate A UNIX date set in the future
     * @param shortened Whether or not to shorten the time-unit into a 1 letter representation
     * @return A formatted timestamp
     */
    public static String toDetailedDate(long futureDate, boolean shortened) {
        return toDetailedDate(System.currentTimeMillis(), futureDate + 50, shortened);
    }

    /**
     * Parses a future UNIX time stamp and outputs a detailed text
     * representation of the amount of time left until that date.
     * <p>
     * Text output will result in <code>{time-unit} time-unit(s)</code>
     * being appended for each time-unit until the date is reached.
     * <p>
     * For example: "1 year 2 months 5 days"
     *
     * @param startDate The beginning time to calculate from
     * @param endDate The end time to calculate to
     * @return A formatted timestamp
     * @throws IllegalArgumentException If the end date provided is after the start date
     */
    public static String toDetailedDate(long startDate, long endDate) {
        return toDetailedDate(startDate, endDate, false);
    }

    /**
     * Parses a future UNIX time stamp and outputs a detailed text
     * representation of the amount of time left until that date.
     * <p>
     * Text output will result in <code>{time-unit} time-unit(s)</code>
     * being appended for each time-unit until the date is reached.
     * <p>
     * For example: "1 year 2 months 5 days"
     *
     * @param startDate The beginning time to calculate from
     * @param endDate The end time to calculate to
     * @param shortened Whether or not to shorten the time-unit into a 1 letter representation
     * @return A formatted timestamp
     * @throws IllegalArgumentException If the end date provided is after the start date
     */
    public static String toDetailedDate(long startDate, long endDate, boolean shortened) {
        DateFormatter formatter = shortened ? new ShortenedDateFormatter() : new DateFormatter();
        long timeLeft = endDate - startDate;

        if (endDate < startDate) {
            throw new IllegalArgumentException("End time (" + endDate + ") must come after start time (" + startDate + ")");
        }

        StringBuilder output = new StringBuilder();
        for (ChronoUnit unit : formattableUnit) {
            long minimumTime = unit.getDuration().toMillis();
            if (minimumTime > timeLeft) {
                continue;
            }
            long units = timeLeft / minimumTime; // Get the number of units for this time type
            timeLeft -= units * minimumTime; // Remove the time that has been formatted

            // Format and append to output
            String formatted = formatter.format(unit, units);
            output.append(formatted).append(' ');
        }

        String result = output.toString();
        if (result.isEmpty()) {
            return formatter.format(ChronoUnit.SECONDS, 0);
        }

        return result.trim(); // Remove excess white space
    }
}
