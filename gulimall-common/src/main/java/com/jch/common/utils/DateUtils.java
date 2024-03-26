package com.jch.common.utils;

import com.jch.common.constant.DateConstant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    /**
     * 当前时间
     * @return
     */
    public static String startTime() {
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);

        //格式化时间
        String startFormat = start.format(DateTimeFormatter.ofPattern(DateConstant.DATE_FORMAT));
        return startFormat;
    }

    /**
     * 结束时间
     * @return
     */
    public static String endTime() {
        LocalDate now = LocalDate.now();
        LocalDate plus = now.plusDays(2);
        LocalTime max = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(plus, max);

        //格式化时间
        String endFormat = end.format(DateTimeFormatter.ofPattern(DateConstant.DATE_FORMAT));
        return endFormat;
    }
}
