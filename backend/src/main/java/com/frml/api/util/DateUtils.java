package com.frml.api.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateUtils {
    public static Date timestampToDate(long timestamp) {
        // 转换为 Instant 对象（秒级时间戳）
        Instant instant = Instant.ofEpochSecond(timestamp); //若为 毫秒级，使用 Instant.ofEpochMilli(timestamp)。
        // 2. 指定时区为Asia/Shanghai，得到正确的时间点
        ZonedDateTime beijingTime = instant.atZone(ZoneId.of("Asia/Shanghai"));
        // 3. 将ZonedDateTime转回Instant（自动调整时间戳）
        Instant adjustedInstant = beijingTime.toInstant();
        // 4. 转换为Date对象
        Date date = Date.from(adjustedInstant);
        return date;
    }
}
