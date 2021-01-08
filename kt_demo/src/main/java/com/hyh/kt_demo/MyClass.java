package com.hyh.kt_demo;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MyClass {


    public static void main(String[] args) {

        long timeMillis = System.currentTimeMillis() + 5 * 60 * 60 * 1000;
        Date date = new Date(timeMillis);


        Calendar currentCalendar = getCalendar();
        currentCalendar.setTime(date);

        Calendar hkCalendarBegin = getCalendar();
        hkCalendarBegin.setTime(date);


        hkCalendarBegin.set(Calendar.HOUR_OF_DAY, 8);
        hkCalendarBegin.set(Calendar.MINUTE, 30);

        Calendar hkCalendarEnd = getCalendar();
        hkCalendarEnd.setTime(date);

        hkCalendarEnd.set(Calendar.HOUR_OF_DAY, 18);
        hkCalendarEnd.set(Calendar.MINUTE, 00);

        if ((currentCalendar.compareTo(hkCalendarBegin) >= 0) && (currentCalendar.compareTo(hkCalendarEnd) < 0)) {
            System.out.println("HK");
        } else {
            System.out.println("US");
        }


    }

    private static Calendar getCalendar() {
        Calendar instance = Calendar.getInstance();
        instance.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return instance;
    }

}


