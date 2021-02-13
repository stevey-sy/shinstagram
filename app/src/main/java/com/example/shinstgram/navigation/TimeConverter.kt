package com.example.shinstgram.navigation

import android.annotation.SuppressLint
import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class TimeConverter {

    companion object {
        const val _SEC : Int = 60;
        const val _MIN : Int = 60;
        const val _HOUR : Int = 24;
        const val _DAY : Int = 7;

        @SuppressLint("SimpleDateFormat")
        fun CreateDataWithCheck(dataString: String?): String? {
            val format = SimpleDateFormat("yyyyMMddHHmmss")
            var date: Date? = null

                if (dataString != null) {
                    date = format.parse(dataString)
                    Log.d("로그 date", dataString)
                }

            val curTime = System.currentTimeMillis()
            Log.d("로그 date2", date.toString())

            val regTime = date?.time
            var diffTime = (curTime - regTime!!) / 1000
            var msg: String
            if (diffTime < _SEC) {
                msg = "방금 전"
            } else if (_SEC.let { diffTime /= it; diffTime } < _MIN) {
                msg = diffTime.toString() + "분 전"
            } else if (_MIN.let { diffTime /= it; diffTime } < _HOUR) {
                msg = diffTime.toString() + "시간 전"
            } else if (_HOUR.let { diffTime /= it; diffTime } < _DAY) {
                msg = diffTime.toString() + "일 전"
            } else {
                val aformat = SimpleDateFormat("yyyy-MM-dd")
                msg = aformat.format(date)
            }
            return msg
        }
    }
}