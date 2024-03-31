package jp.knowlsat.lstm.predict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class LSTM_PrevStatistics {
	public static final String STATISTICS_PATH = "output\\statistics";

	public int WindowSize;
	public String minute;

	public String[] z_coDatetimes;
	public LSTM_Date lstm_date;

	public ArrayList<String> prev_date;
	public ArrayList<Double> prev_inv_p;
	public ArrayList<Double> prev_ammonia;
	public ArrayList<Boolean> prev_incident;

	public LSTM_PrevStatistics(int WindowSize, String minute) {
		this.WindowSize = WindowSize;
		this.minute = minute;

		z_coDatetimes = new String[WindowSize];
		prev_date = new ArrayList<String>();
		prev_inv_p = new ArrayList<Double>();
		prev_ammonia = new ArrayList<Double>();
		prev_incident = new ArrayList<Boolean>();
	}

	public int set(String[] z_coDatetimes) {
		String year = z_coDatetimes[WindowSize - 1].substring(0, 4);
		String month = z_coDatetimes[WindowSize - 1].substring(5, 7);
		String day = z_coDatetimes[WindowSize - 1].substring(8, 10);
		String hour = z_coDatetimes[WindowSize - 1].substring(11, 13);
		lstm_date = new LSTM_Date(year, month, day, hour, minute);

		return 0;
	}

	public int allLoad() {
		String strDate = lstm_date.year + "/" + lstm_date.month + "/" + lstm_date.day + " " + lstm_date.hour + ":"
				+ lstm_date.minute + ":00";
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date;

		try {
			date = sdFormat.parse(strDate);
		} catch (Exception e) {
			System.out.println("Failed to convert date:[" + strDate + "]. " + e.getMessage());
			return -1;
		}

		Calendar cl = Calendar.getInstance();
		cl.setTime(date);
		cl.add(Calendar.HOUR_OF_DAY, -1);
		LSTM_Date lstm_date_prev = lstm_date.getLstmDate(cl);
		String prev_strDate = lstm_date_prev.year + "/" + lstm_date_prev.month + "/" + lstm_date_prev.day + " "
				+ lstm_date_prev.hour + ":" + lstm_date_prev.minute + ":00";
		Date prev_date;

		try {
			prev_date = sdFormat.parse(prev_strDate);
		} catch (Exception e) {
			System.out.println("Failed to convert date:[" + prev_strDate + "]. " + e.getMessage());
			return -2;
		}

		Calendar cl_prev = Calendar.getInstance();
		cl_prev.setTime(prev_date);
		int offset = 1 - WindowSize;
		cl_prev.add(Calendar.HOUR_OF_DAY, offset);
		load(lstm_date_prev.getLstmDate(cl_prev), offset);

		for (int i = offset + 1; i <= 0; i++) {
			cl_prev.add(Calendar.HOUR_OF_DAY, 1);
			load(lstm_date_prev.getLstmDate(cl_prev), i);
		}

		return 0;
	}

	public int load(LSTM_Date date, int offset) {
		String statistics_path_str = STATISTICS_PATH + "\\" + date.year + "\\" + date.month + "\\" + date.day;
		String startFiles = statistics_path_str + "\\" + "S_" + date.year + date.month + date.day + date.hour
				+ date.minute
				+ "_";

		ArrayList<File> csvFiles = new ArrayList<File>();
		File dir = new File(statistics_path_str);
		File[] files = dir.listFiles();

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];

				if (file.toString().startsWith(startFiles)) {
					csvFiles.add(file);
				}
			}

			int fileIndex = -1;
			Long maxDate = 19700101000000L;
			int len_ext = 4;
			int len_yyyyMMddHHmmss = 14;

			for (int i = 0; i < csvFiles.size(); i++) {
				String fileName = csvFiles.get(i).toString();
				int length = fileName.length();
				String fileOutputDate = fileName.substring(length - (len_yyyyMMddHHmmss + len_ext), length - len_ext);
				Long num_date = Long.parseLong(fileOutputDate);

				if (fileName.startsWith(startFiles) && num_date > maxDate) {
					maxDate = num_date;
					fileIndex = i;
				}
			}

			if (fileIndex != -1) {
				Path statistics_path = Path.of(csvFiles.get(fileIndex).toString());
				File file = statistics_path.toFile();
				FileReader fileReader;
				BufferedReader bufferedReader;
				String[] items;

				try {
					fileReader = new FileReader(file);
					bufferedReader = new BufferedReader(fileReader);

					bufferedReader.readLine();
					items = bufferedReader.readLine().split(",");
				} catch (IOException e) {
					System.out.println("Failed to open file. " + e.getMessage());
					return -1;
				}

				prev_date.add(items[0]); // 前回時刻を格納
				prev_inv_p.add(Double.parseDouble(items[1])); // 前回予測値を格納
				prev_ammonia.add(Double.parseDouble(items[items.length - 2])); // 前回正規化前アンモニアを格納
				prev_incident.add(Boolean.parseBoolean(items[items.length - 1])); // 前回インシデントを格納

				try {
					bufferedReader.close();
				} catch (IOException e) {
					System.out.println("Failed to close file. " + e.getMessage());
					return -2;
				}
			}
		}

		return 0;
	}

}
