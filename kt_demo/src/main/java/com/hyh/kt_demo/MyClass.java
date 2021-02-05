package com.hyh.kt_demo;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MyClass {


    public static void main(String[] args) {



        String s = "4000.00000";



        String regex1 = "(1?[0-9]{1})\\.[0-9]+";
        String regex2 = "[2-9][0-9]+\\.[0-9]+";//大于20

        //455 + 191


        System.out.println("");






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


    public static String formatCount(String d) {
        try {
            BigDecimal count = new BigDecimal(d);
            DecimalFormat countFormat = new DecimalFormat("###,###", new DecimalFormatSymbols(Locale.CHINA));
            return countFormat.format(count);
        } catch (Exception e) {
        }
        return "";
    }


    public static String formatCountKeepDecimal(String d) {
        try {
            BigDecimal count = new BigDecimal(d);
            BigDecimal noZeros = count.stripTrailingZeros();
            String plainString = noZeros.toPlainString();
            int index = plainString.indexOf('.');
            String integer;
            String decimal;
            if (index > 0) {
                integer = plainString.substring(0, index);
                decimal = plainString.substring(index + 1);
            } else {
                integer = plainString;
                decimal = null;
            }
            String formatInteger = formatCount(integer);
            if (decimal != null) {
                return formatInteger + "." + decimal;
            } else {
                return formatInteger;
            }
        } catch (Exception e) {
        }
        return "";
    }


    public static String formatCount2(String d) {
        try {
            BigDecimal count = new BigDecimal(d);
            BigDecimal noZeros = count.stripTrailingZeros();
            return noZeros.toPlainString();
        } catch (Exception e) {
        }
        return "";
    }

    private static Calendar getCalendar() {
        Calendar instance = Calendar.getInstance();
        instance.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return instance;
    }

    public static boolean isDigitsOnly(CharSequence str) {
        final int len = str.length();
        for (int cp, i = 0; i < len; i += Character.charCount(cp)) {
            cp = Character.codePointAt(str, i);
            if (!Character.isDigit(cp)) {
                return false;
            }
        }
        return true;
    }

}


