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

public class LSTM_Input {
	public static final String INPUT_PATH = "output\\input";

	public int WindowSize;
	public int DataType;
	public int nOut;
	public int targetIndex;
	public int ammoniaIndex;
	public String minute;

	public double[][] z_train;
	public double[][] z_target;
	public boolean[] z_flag;
	public String[] z_datetimes;
	public String[] z_coDatetimes;
	public boolean incident;
	public LSTM_Date lstm_date;

	public LSTM_Input(int WindowSize, int DataType, int nOut, int targetIndex, int ammoniaIndex, String minute) {
		this.WindowSize = WindowSize;
		this.DataType = DataType;
		this.nOut = nOut;
		this.targetIndex = targetIndex;
		this.ammoniaIndex = ammoniaIndex;
		this.minute = minute;

		z_train = new double[WindowSize][DataType];
		z_target = new double[WindowSize][nOut];
		z_flag = new boolean[WindowSize];
		z_datetimes = new String[WindowSize];
		z_coDatetimes = new String[WindowSize];
	}

	public int set(double[][] z_train, double[][] z_target, boolean[] z_flag, String[] z_datetimes,
			String[] z_coDatetimes, boolean incident) {
		this.z_train = z_train;
		this.z_target = z_target;
		this.z_flag = z_flag;
		this.z_datetimes = z_datetimes;
		this.z_coDatetimes = z_coDatetimes;
		this.incident = incident;
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
		String input_path_str = INPUT_PATH + "\\" + date.year + "\\" + date.month + "\\" + date.day;
		String startFiles = input_path_str + "\\" + "I_" + date.year + date.month + date.day + date.hour + date.minute
				+ "_";

		ArrayList<File> csvFiles = new ArrayList<File>();
		File dir = new File(input_path_str);
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
				Path input_path = Path.of(csvFiles.get(fileIndex).toString());
				File file = input_path.toFile();
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

				z_train[WindowSize - 1 + offset][targetIndex] = Double.parseDouble(items[1]); // 正規化中次亜予測値を格納
				z_train[WindowSize - 1 + offset][this.ammoniaIndex] = Double.parseDouble(items[3]); // 正規化アンモニアを格納
				z_flag[WindowSize - 1 + offset] = Boolean.parseBoolean(items[2]); // 取水状態フラグを格納

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
