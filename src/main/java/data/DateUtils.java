package data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import reporting.TestLogManager;
import reporting.ExtentManager;

/**
 * Utility class for date and time operations with ExtentManager logging
 */
public class DateUtils {

    private static final List<String> DATE_PATTERNS = Arrays.asList(
        "E, dd MMM, yyyy hh:mm a", 
        "E, dd MMM, yyyy", 
        "dd MMM yyyy", 
        "yyyy-MM-dd", 
        "dd/MM/yyyy", 
        "MM/dd/yyyy"
    );

    public static String getCurrentDate(String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String date = sdf.format(new Date());
            TestLogManager.dataInfo("Current date", date);
            ExtentManager.infoTest("Current date: " + date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current date", e);
            ExtentManager.failTest("Failed to get current date: " + e.getMessage());
            throw e;
        }
    }

    public static String getCurrentDate() {
        return getCurrentDate("dd/MM/yyyy");
    }

    public static String getCurrentTime(String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String time = sdf.format(new Date());
            TestLogManager.dataInfo("Current time", time);
            ExtentManager.infoTest("Current time: " + time);
            return time;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current time", e);
            ExtentManager.failTest("Failed to get current time: " + e.getMessage());
            throw e;
        }
    }

    public static String getCurrentTime() {
        return getCurrentTime("HH:mm:ss");
    }

    public static String getCurrentTimeWithoutSeconds() {
        return getCurrentTime("HH:mm");
    }

    public static String getCurrentYear() {
        try {
            Calendar cal = Calendar.getInstance();
            String year = String.valueOf(cal.get(Calendar.YEAR));
            TestLogManager.dataInfo("Current year", year);
            ExtentManager.infoTest("Current year: " + year);
            return year;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current year", e);
            ExtentManager.failTest("Failed to get current year: " + e.getMessage());
            throw e;
        }
    }

    public static String addDaysToCurrentDate(int days, String format) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, days);
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String date = sdf.format(cal.getTime());
            TestLogManager.dataInfo("Date after adding " + days + " days", date);
            ExtentManager.infoTest("Date after adding " + days + " days: " + date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to add days to current date", e);
            ExtentManager.failTest("Failed to add days to current date: " + e.getMessage());
            throw e;
        }
    }

    public static String addDaysToCurrentDate(int days) {
        return addDaysToCurrentDate(days, "dd/MM/yyyy");
    }

    public static String addTimeToName() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
            String timestamp = sdf.format(new Date());
            TestLogManager.dataInfo("Generated timestamp", timestamp);
            ExtentManager.infoTest("Generated timestamp: " + timestamp);
            return timestamp;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate timestamp", e);
            ExtentManager.failTest("Failed to generate timestamp: " + e.getMessage());
            throw e;
        }
    }

    public static String addTimeToShort() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy_HHmm");
            String timestamp = sdf.format(new Date());
            TestLogManager.dataInfo("Generated short timestamp", timestamp);
            ExtentManager.infoTest("Generated short timestamp: " + timestamp);
            return timestamp;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate short timestamp", e);
            ExtentManager.failTest("Failed to generate short timestamp: " + e.getMessage());
            throw e;
        }
    }

    public static String addTimeToAlpha() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
            String date = sdf.format(new Date());
            String alphabetic = convertToAlphabetic(date);
            TestLogManager.dataInfo("Generated alphabetic timestamp", alphabetic);
            ExtentManager.infoTest("Generated alphabetic timestamp: " + alphabetic);
            return alphabetic;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate alphabetic timestamp", e);
            ExtentManager.failTest("Failed to generate alphabetic timestamp: " + e.getMessage());
            throw e;
        }
    }

    public static String convertToAlphabetic(String input) {
        try {
            StringBuilder result = new StringBuilder();
            for (char c : input.toCharArray()) {
                if (Character.isDigit(c)) {
                    int digit = Character.getNumericValue(c);
                    result.append((char) ('A' + digit));
                } else {
                    result.append(c);
                }
            }
            ExtentManager.infoTest("Converted to alphabetic: " + result.toString());
            return result.toString();
        } catch (Exception e) {
            TestLogManager.error("Failed to convert to alphabetic", e);
            ExtentManager.failTest("Failed to convert to alphabetic: " + e.getMessage());
            throw e;
        }
    }

    public static String addTimeToValue() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            TestLogManager.dataInfo("Generated value timestamp", timestamp);
            ExtentManager.infoTest("Generated value timestamp: " + timestamp);
            return timestamp;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate value timestamp", e);
            ExtentManager.failTest("Failed to generate value timestamp: " + e.getMessage());
            throw e;
        }
    }

    public static Date parseDate(String dateStr) throws ParseException {
        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Date date = sdf.parse(dateStr);
                TestLogManager.dataInfo("Parsed date", dateStr + " -> " + date);
                ExtentManager.infoTest("Parsed date: " + dateStr + " -> " + date);
                return date;
            } catch (ParseException e) {
                // Continue to next pattern
            }
        }
        ExtentManager.failTest("Unable to parse date: " + dateStr);
        throw new ParseException("Unable to parse date: " + dateStr, 0);
    }

    public static Date truncateTime(Date date) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date truncatedDate = cal.getTime();
            TestLogManager.dataInfo("Truncated date", truncatedDate.toString());
            ExtentManager.infoTest("Truncated date: " + truncatedDate);
            return truncatedDate;
        } catch (Exception e) {
            TestLogManager.error("Failed to truncate time", e);
            ExtentManager.failTest("Failed to truncate time: " + e.getMessage());
            throw e;
        }
    }

    public static String formatDate(Date date, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String formattedDate = sdf.format(date);
            TestLogManager.dataInfo("Formatted date", formattedDate);
            ExtentManager.infoTest("Formatted date: " + formattedDate);
            return formattedDate;
        } catch (Exception e) {
            TestLogManager.error("Failed to format date", e);
            ExtentManager.failTest("Failed to format date: " + e.getMessage());
            throw e;
        }
    }

    public static String calendarScheduleDate(int addDays) {
        return calendarScheduleDate(addDays, "dd/MM/yyyy");
    }

    public static String calendarScheduleDate(int addDays, String format) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, addDays);
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String date = sdf.format(cal.getTime());
            TestLogManager.dataInfo("Calendar schedule date", date);
            ExtentManager.infoTest("Calendar schedule date: " + date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to get calendar schedule date", e);
            ExtentManager.failTest("Failed to get calendar schedule date: " + e.getMessage());
            throw e;
        }
    }

    public static Date convertStringToDate(String dateStr, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Date date = sdf.parse(dateStr);
            TestLogManager.dataInfo("Converted string to date", dateStr + " -> " + date);
            ExtentManager.infoTest("Converted string to date: " + dateStr + " -> " + date);
            return date;
        } catch (ParseException e) {
            TestLogManager.error("Failed to convert string to date", e);
            ExtentManager.failTest("Failed to convert string to date: " + e.getMessage());
            throw new RuntimeException("Failed to parse date: " + dateStr, e);
        }
    }

    public static LocalDate getCurrentLocalDate() {
        try {
            LocalDate date = LocalDate.now();
            TestLogManager.dataInfo("Current LocalDate", date.toString());
            ExtentManager.infoTest("Current LocalDate: " + date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current LocalDate", e);
            ExtentManager.failTest("Failed to get current LocalDate: " + e.getMessage());
            throw e;
        }
    }

    public static LocalTime getCurrentLocalTime() {
        try {
            LocalTime time = LocalTime.now();
            TestLogManager.dataInfo("Current LocalTime", time.toString());
            ExtentManager.infoTest("Current LocalTime: " + time);
            return time;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current LocalTime", e);
            ExtentManager.failTest("Failed to get current LocalTime: " + e.getMessage());
            throw e;
        }
    }

    public static LocalDateTime getCurrentLocalDateTime() {
        try {
            LocalDateTime dateTime = LocalDateTime.now();
            TestLogManager.dataInfo("Current LocalDateTime", dateTime.toString());
            ExtentManager.infoTest("Current LocalDateTime: " + dateTime);
            return dateTime;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current LocalDateTime", e);
            ExtentManager.failTest("Failed to get current LocalDateTime: " + e.getMessage());
            throw e;
        }
    }

    public static String formatLocalDate(LocalDate date, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String formattedDate = date.format(formatter);
            TestLogManager.dataInfo("Formatted LocalDate", formattedDate);
            ExtentManager.infoTest("Formatted LocalDate: " + formattedDate);
            return formattedDate;
        } catch (Exception e) {
            TestLogManager.error("Failed to format LocalDate", e);
            ExtentManager.failTest("Failed to format LocalDate: " + e.getMessage());
            throw e;
        }
    }

    public static String formatLocalTime(LocalTime time, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String formattedTime = time.format(formatter);
            TestLogManager.dataInfo("Formatted LocalTime", formattedTime);
            ExtentManager.infoTest("Formatted LocalTime: " + formattedTime);
            return formattedTime;
        } catch (Exception e) {
            TestLogManager.error("Failed to format LocalTime", e);
            ExtentManager.failTest("Failed to format LocalTime: " + e.getMessage());
            throw e;
        }
    }

    public static String formatLocalDateTime(LocalDateTime dateTime, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String formattedDateTime = dateTime.format(formatter);
            TestLogManager.dataInfo("Formatted LocalDateTime", formattedDateTime);
            ExtentManager.infoTest("Formatted LocalDateTime: " + formattedDateTime);
            return formattedDateTime;
        } catch (Exception e) {
            TestLogManager.error("Failed to format LocalDateTime", e);
            ExtentManager.failTest("Failed to format LocalDateTime: " + e.getMessage());
            throw e;
        }
    }

    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
            TestLogManager.info("Slept for " + milliseconds + " milliseconds");
            ExtentManager.infoTest("Slept for " + milliseconds + " milliseconds");
        } catch (InterruptedException e) {
            TestLogManager.error("Sleep interrupted", e);
            ExtentManager.failTest("Sleep interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
