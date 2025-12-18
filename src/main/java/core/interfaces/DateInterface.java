package core.interfaces;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public interface DateInterface {

    Date parseUnknownFormat(String dateStr);

    String getCurrentDate(String format);
    String getCurrentDate();

    String getCurrentTime(String format);
    String getCurrentTime();
    String getCurrentTimeWithoutSeconds();

    String getCurrentYear();

    String addDaysToCurrentDate(int days, String format);
    String addDaysToCurrentDate(int days);

    String addTimeToName();
    String addTimeToShort();
    String addTimeToAlpha();
    String addTimeToValue();
    String addTimeToValueShort();

    Date parseDate(String dateStr) throws ParseException;

    Date truncateTime(Date date);
    String formatDate(Date date, String format);

    String calendarScheduleDate(int addDays);
    String calendarScheduleDate(int addDays, String format);

    Date convertStringToDate(String dateStr, String pattern);

    LocalDate getCurrentLocalDate();
    LocalTime getCurrentLocalTime();
    LocalDateTime getCurrentLocalDateTime();

    String formatLocalDate(LocalDate date, String pattern);
    String formatLocalTime(LocalTime time, String pattern);
    String formatLocalDateTime(LocalDateTime dateTime, String pattern);

    String[] getRoundedTimes();

    String currentDateAndTime(String format);

    Date removeTime(Date date);
}
