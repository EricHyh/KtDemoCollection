package com.hyh.kt_demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MyClass {


    public static void main(String[] args) {

        String s5 = testSwitch(1);
        String s4 = testSwitch(2);

        String numStr1 = "0";
        String numStr2 = "-0.00000000000";
        String numStr3 = "+0.00000000000";

        double num1 = parseDouble(numStr1, 0.0);
        double num2 = parseDouble(numStr2, 0.0);
        double num3 = parseDouble(numStr3, 0.0);

        int compare1 = Double.compare(num1, 0.0);
        int compare2 = Double.compare(num2, 0.0);
        int compare3 = Double.compare(num3, 0.0);

        int colorFromThree1 = getColorFromThree(num1, 0.0);
        int colorFromThree2 = getColorFromThree(num2, 0.0);
        int colorFromThree3 = getColorFromThree(num3, 0.0);

        String signum1 = getSignum(num1);
        String signum2 = getSignum(num2);
        String signum3 = getSignum(num3);

        String s1 = formatAssetsValue(numStr1);
        String s2 = formatAssetsValue(numStr2);
        String s3 = formatAssetsValue(numStr3);


        DecimalFormat propertyUSFormat = new DecimalFormat("###,##0.00##", new DecimalFormatSymbols(Locale.CHINA));
        String format = propertyUSFormat.format(new BigDecimal("33605.695999"));

        System.out.println("");

        double xx1 = 0.0;
        double xx2 = -0.0;

        System.out.println(xx1 == xx2);


        String s = "4000.00000";


        String regex1 = "(1?[0-9]{1})\\.[0-9]+";
        String regex2 = "[2-9][0-9]+\\.[0-9]+";//大于20

        //455 + 191


        System.out.println("()");


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


    private static String testSwitch(int value)
    {
        String result = "default";
        switch (value)
        {
            case 1:
            {
                result = "1";
                break;
            }
        }
        return result;
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

    public static double parseDouble(String value, double defaultVal) {
        double result = defaultVal;
        try {
            result = Double.parseDouble(value);
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public static String getSignum(double value) {
        String signum = "";
        if (value > 0) {
            signum = "+";
        }
        return signum;
    }

    public static String formatAssetsValue(String d) {
        DecimalFormat assetsValueFormat = new DecimalFormat("###,##0.00", new DecimalFormatSymbols(Locale.CHINA));
        assetsValueFormat.setRoundingMode(RoundingMode.DOWN);

        try {
            BigDecimal roundValue = new BigDecimal(d);
            return assetsValueFormat.format(roundValue);
        } catch (Exception e) {
            return "";
        }
    }

    public static int getColorFromThree(double v1, double v2) {
        int color;
        if (Double.compare(v1, v2) == 0) {
            color = 1;
        } else if (v1 > v2) {
            color = 0;
        } else {
            color = 2;
        }
        return color;
    }
}


