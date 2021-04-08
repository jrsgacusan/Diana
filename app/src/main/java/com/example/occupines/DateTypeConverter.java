package com.example.occupines;


import androidx.room.TypeConverter;

import java.util.Date;


public class DateTypeConverter {

    @TypeConverter
    public Date LongToDateConverter(Long date) {
        return new Date(date);
    }

    @TypeConverter
    public Long DateToLongConverter(Date date) {
        return date.getTime();
    }

}
