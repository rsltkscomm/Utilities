package core.playwright;

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
import java.util.TimeZone;

import base.DriverContext;
import core.interfaces.DateInterface;
import reporting.TestLogManager;

public class PlaywrightDateUtil extends PlaywrightLocatorUtil implements DateInterface {

    protected final DriverContext driverContext;

    public PlaywrightDateUtil(DriverContext driverContext) {
    	super(driverContext);
        this.driverContext = driverContext;
    }

    private final List<String> DATE_PATTERNS = Arrays.asList(
            "E, dd MMM, yyyy hh:mm a",
            "E, dd MMM, yyyy",
            "dd MMM yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
    );

    public Date parseUnknownFormat(String dateStr) {
        String[] possibleFormats = {
                "MM-dd-yyyy", "dd-MM-yyyy", "yyyy-MM-dd",
                "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd"
        };

        for (String format : possibleFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                return sdf.parse(dateStr);
            } catch (ParseException ignored) {}
        }
        throw new IllegalArgumentException("Unsupported date format: " + dateStr);
    }

    public String getCurrentDate(String format) {
        String date = new SimpleDateFormat(format).format(new Date());
        TestLogManager.dataInfo("Current date", date);
        return date;
    }

    public String getCurrentDate() {
        return getCurrentDate("dd/MM/yyyy");
    }

    public String getCurrentTime(String format) {
        String time = new SimpleDateFormat(format).format(new Date());
        TestLogManager.dataInfo("Current time", time);
        return time;
    }

    public String getCurrentTime() {
        return getCurrentTime("HH:mm:ss");
    }

    public String getCurrentTimeWithoutSeconds() {
        return getCurrentTime("HH:mm");
    }

    public String getCurrentYear() {
        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        TestLogManager.dataInfo("Current year", year);
        return year;
    }

    public String addDaysToCurrentDate(int days, String format) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, days);
        String date = new SimpleDateFormat(format).format(cal.getTime());
        TestLogManager.dataInfo("Date after adding days", date);
        return date;
    }

    public String addDaysToCurrentDate(int days) {
        return addDaysToCurrentDate(days, "dd/MM/yyyy");
    }

    public String addTimeToName() {
        return new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
    }

    public String addTimeToShort() {
        return new SimpleDateFormat("ddMMyy_HHmm").format(new Date());
    }

    public String addTimeToAlpha() {
        return convertToAlphabetic(
                new SimpleDateFormat("ddMMyyyy").format(new Date()));
    }

    public String convertToAlphabetic(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            result.append(Character.isDigit(c)
                    ? (char) ('A' + Character.getNumericValue(c))
                    : c);
        }
        return result.toString();
    }

    public String addTimeToValue() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    public String addTimeToValueShort() {
        SimpleDateFormat sdf = new SimpleDateFormat("M-d-HHmmSS");
        sdf.setTimeZone(TimeZone.getTimeZone("IST"));
        return sdf.format(new Date());
    }

    public Date parseDate(String dateStr) throws ParseException {
        for (String pattern : DATE_PATTERNS) {
            try {
                return new SimpleDateFormat(pattern).parse(dateStr);
            } catch (ParseException ignored) {}
        }
        throw new ParseException("Unable to parse date: " + dateStr, 0);
    }

    public Date truncateTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    public String calendarScheduleDate(int addDays) {
        return calendarScheduleDate(addDays, "dd/MM/yyyy");
    }

    public String calendarScheduleDate(int addDays, String format) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, addDays);
        return new SimpleDateFormat(format).format(cal.getTime());
    }

    public Date convertStringToDate(String dateStr, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse date: " + dateStr, e);
        }
    }

    public LocalDate getCurrentLocalDate() {
        return LocalDate.now();
    }

    public LocalTime getCurrentLocalTime() {
        return LocalTime.now();
    }

    public LocalDateTime getCurrentLocalDateTime() {
        return LocalDateTime.now();
    }

    public String formatLocalDate(LocalDate date, String pattern) {
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public String formatLocalTime(LocalTime time, String pattern) {
        return time.format(DateTimeFormatter.ofPattern(pattern));
    }

    public String formatLocalDateTime(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public String[] getRoundedTimes() {
        Calendar cal = Calendar.getInstance();
        int minute = cal.get(Calendar.MINUTE);
        int hour24 = cal.get(Calendar.HOUR_OF_DAY);
        int hour12 = cal.get(Calendar.HOUR);
        hour12 = (hour12 == 0) ? 12 : hour12;
        String amPm = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";

        if (minute >= 30) {
            return new String[]{
                    ((hour12 % 12) + 1) + ":00 " + amPm,
                    String.format("%02d:00", (hour24 + 1) % 24)
            };
        }
        return new String[]{
                hour12 + ":30 " + amPm,
                String.format("%02d:30", hour24)
        };
    }

    public String currentDateAndTime(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }

    public Date removeTime(Date date) {
        return truncateTime(date);
    }
}
