package jp.knowlsat.lstm.predict;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LSTM_Date {
    public String year;
    public String month;
    public String day;
    public String hour;
    public String minute;

    public LSTM_Date(String minute) {
        this.minute = minute;
    }

    public LSTM_Date(String year, String month, String day, String hour, String minute) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
    }
    
    public LSTM_Date getLstmDate(Calendar cl) {
        Date date = cl.getTime();
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd/HH");
        String strDate = df.format(date);
        String[] item = strDate.split("/");

        return new LSTM_Date(item[0], item[1], item[2], item[3], this.minute);
    }
}
