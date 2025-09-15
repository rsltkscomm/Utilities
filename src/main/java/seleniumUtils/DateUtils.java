package seleniumUtils;

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

/**
 * Utility class for date and time operations
 * Extracted from PageBase.java to improve code organization
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
    
    /**
     * Get current date in specified format
     */
    public static String getCurrentDate(String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String date = sdf.format(new Date());
            TestLogManager.dataInfo("Current date", date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current date", e);
            throw e;
        }
    }
    
    /**
     * Get current date in default format (dd/MM/yyyy)
     */
    public static String getCurrentDate() {
        return getCurrentDate("dd/MM/yyyy");
    }
    
    /**
     * Get current time in specified format
     */
    public static String getCurrentTime(String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String time = sdf.format(new Date());
            TestLogManager.dataInfo("Current time", time);
            return time;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current time", e);
            throw e;
        }
    }
    
    /**
     * Get current time in default format (HH:mm:ss)
     */
    public static String getCurrentTime() {
        return getCurrentTime("HH:mm:ss");
    }
    
    /**
     * Get current time without seconds
     */
    public static String getCurrentTimeWithoutSeconds() {
        return getCurrentTime("HH:mm");
    }
    
    /**
     * Get current year
     */
    public static String getCurrentYear() {
        try {
            Calendar cal = Calendar.getInstance();
            String year = String.valueOf(cal.get(Calendar.YEAR));
            TestLogManager.dataInfo("Current year", year);
            return year;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current year", e);
            throw e;
        }
    }
    
    /**
     * Add days to current date
     */
    public static String addDaysToCurrentDate(int days, String format) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, days);
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String date = sdf.format(cal.getTime());
            TestLogManager.dataInfo("Date after adding " + days + " days", date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to add days to current date", e);
            throw e;
        }
    }
    
    /**
     * Add days to current date in default format
     */
    public static String addDaysToCurrentDate(int days) {
        return addDaysToCurrentDate(days, "dd/MM/yyyy");
    }
    
    /**
     * Add time to name for unique identifiers
     */
    public static String addTimeToName() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
            String timestamp = sdf.format(new Date());
            TestLogManager.dataInfo("Generated timestamp", timestamp);
            return timestamp;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate timestamp", e);
            throw e;
        }
    }
    
    /**
     * Add time to name in short format
     */
    public static String addTimeToShort() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy_HHmm");
            String timestamp = sdf.format(new Date());
            TestLogManager.dataInfo("Generated short timestamp", timestamp);
            return timestamp;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate short timestamp", e);
            throw e;
        }
    }
    
    /**
     * Add alphabetic timestamp
     */
    public static String addTimeToAlpha() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
            String date = sdf.format(new Date());
            String alphabetic = convertToAlphabetic(date);
            TestLogManager.dataInfo("Generated alphabetic timestamp", alphabetic);
            return alphabetic;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate alphabetic timestamp", e);
            throw e;
        }
    }
    
    /**
     * Convert numeric string to alphabetic
     */
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
            return result.toString();
        } catch (Exception e) {
            TestLogManager.error("Failed to convert to alphabetic", e);
            throw e;
        }
    }
    
    /**
     * Add time to value
     */
    public static String addTimeToValue() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            TestLogManager.dataInfo("Generated value timestamp", timestamp);
            return timestamp;
        } catch (Exception e) {
            TestLogManager.error("Failed to generate value timestamp", e);
            throw e;
        }
    }
    
    /**
     * Parse date string with multiple patterns
     */
    public static Date parseDate(String dateStr) throws ParseException {
        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Date date = sdf.parse(dateStr);
                TestLogManager.dataInfo("Parsed date", dateStr + " -> " + date);
                return date;
            } catch (ParseException e) {
                // Continue to next pattern
            }
        }
        throw new ParseException("Unable to parse date: " + dateStr, 0);
    }
    
    /**
     * Truncate time from date
     */
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
            return truncatedDate;
        } catch (Exception e) {
            TestLogManager.error("Failed to truncate time", e);
            throw e;
        }
    }
    
    /**
     * Format date to string
     */
    public static String formatDate(Date date, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String formattedDate = sdf.format(date);
            TestLogManager.dataInfo("Formatted date", formattedDate);
            return formattedDate;
        } catch (Exception e) {
            TestLogManager.error("Failed to format date", e);
            throw e;
        }
    }
    
    /**
     * Get calendar schedule date
     */
    public static String calendarScheduleDate(int addDays) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, addDays);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String date = sdf.format(cal.getTime());
            TestLogManager.dataInfo("Calendar schedule date", date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to get calendar schedule date", e);
            throw e;
        }
    }
    
    /**
     * Get calendar schedule date with custom format
     */
    public static String calendarScheduleDate(int addDays, String format) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, addDays);
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            String date = sdf.format(cal.getTime());
            TestLogManager.dataInfo("Calendar schedule date", date);
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to get calendar schedule date", e);
            throw e;
        }
    }
    
    /**
     * Convert string to date
     */
    public static Date convertStringToDate(String dateStr, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Date date = sdf.parse(dateStr);
            TestLogManager.dataInfo("Converted string to date", dateStr + " -> " + date);
            return date;
        } catch (ParseException e) {
            TestLogManager.error("Failed to convert string to date", e);
            throw new RuntimeException("Failed to parse date: " + dateStr, e);
        }
    }
    
    /**
     * Get current date in LocalDate format
     */
    public static LocalDate getCurrentLocalDate() {
        try {
            LocalDate date = LocalDate.now();
            TestLogManager.dataInfo("Current LocalDate", date.toString());
            return date;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current LocalDate", e);
            throw e;
        }
    }
    
    /**
     * Get current time in LocalTime format
     */
    public static LocalTime getCurrentLocalTime() {
        try {
            LocalTime time = LocalTime.now();
            TestLogManager.dataInfo("Current LocalTime", time.toString());
            return time;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current LocalTime", e);
            throw e;
        }
    }
    
    /**
     * Get current date time in LocalDateTime format
     */
    public static LocalDateTime getCurrentLocalDateTime() {
        try {
            LocalDateTime dateTime = LocalDateTime.now();
            TestLogManager.dataInfo("Current LocalDateTime", dateTime.toString());
            return dateTime;
        } catch (Exception e) {
            TestLogManager.error("Failed to get current LocalDateTime", e);
            throw e;
        }
    }
    
    /**
     * Format LocalDate to string
     */
    public static String formatLocalDate(LocalDate date, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String formattedDate = date.format(formatter);
            TestLogManager.dataInfo("Formatted LocalDate", formattedDate);
            return formattedDate;
        } catch (Exception e) {
            TestLogManager.error("Failed to format LocalDate", e);
            throw e;
        }
    }
    
    /**
     * Format LocalTime to string
     */
    public static String formatLocalTime(LocalTime time, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String formattedTime = time.format(formatter);
            TestLogManager.dataInfo("Formatted LocalTime", formattedTime);
            return formattedTime;
        } catch (Exception e) {
            TestLogManager.error("Failed to format LocalTime", e);
            throw e;
        }
    }
    
    /**
     * Format LocalDateTime to string
     */
    public static String formatLocalDateTime(LocalDateTime dateTime, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            String formattedDateTime = dateTime.format(formatter);
            TestLogManager.dataInfo("Formatted LocalDateTime", formattedDateTime);
            return formattedDateTime;
        } catch (Exception e) {
            TestLogManager.error("Failed to format LocalDateTime", e);
            throw e;
        }
    }
    
    public String[] getRoundedTimes() {
        Calendar cal = Calendar.getInstance();
        int minute = cal.get(Calendar.MINUTE);
        int hour24 = cal.get(Calendar.HOUR_OF_DAY);
        int hour12 = cal.get(Calendar.HOUR);
        hour12 = (hour12 == 0) ? 12 : hour12; 
        String amPm = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
     
        String roundedTime12;
        String roundedTime24;
     
        if (minute >= 30) {
            hour12 = (hour12 % 12) + 1;
            hour24 = (hour24 + 1) % 24;
            roundedTime12 = hour12 + ":00 " + amPm;
            roundedTime24 = String.format("%02d:00", hour24);
        } else {
            roundedTime12 = hour12 + ":30 " + amPm;
            roundedTime24 = String.format("%02d:30", hour24);
        }
        return new String[] { roundedTime12, roundedTime24 };
    }
} 