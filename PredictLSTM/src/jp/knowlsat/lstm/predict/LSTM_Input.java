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
	public String minute;

	public double[][] z_train;
	public double[][] z_target;
	public boolean[] z_flag;
	public String[] z_datetimes;
	public String[] z_coDatetimes;
	public boolean incident;

	public LSTM_Date lstm_date;
	public String input_path_str;
	public Path input_path; 

	public LSTM_Input(int WindowSize, int DataType, int nOut, int targetIndex, String minute) {
		this.WindowSize = WindowSize;
		this.DataType = DataType;
		this.nOut = nOut;
		this.targetIndex = targetIndex;
		this.minute = minute;

		z_train = new double[WindowSize][DataType];
		z_target = new double[WindowSize][nOut];
		z_flag = new boolean[WindowSize];
		z_datetimes = new String[WindowSize];
		z_coDatetimes = new String[WindowSize];
	}

	public int set(double[][] z_train, double[][] z_target, boolean[] z_flag, String[] z_datetimes, String[] z_coDatetimes, boolean incident) {
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
		String strDate = lstm_date.year + "/" + lstm_date.month + "/" + lstm_date.day + " " + lstm_date.hour + ":" + lstm_date.minute + ":00";
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date;

		try {
			date = sdFormat.parse(strDate);
		} catch (Exception e) {
			System.out.println("Failed to convert date:[" + strDate + "]. " + e.getMessage());
			return -1;
		}

		int offset = 1 - WindowSize;
		Calendar cl = Calendar.getInstance();
		cl.setTime(date);
		cl.add(Calendar.HOUR, offset);
		load(lstm_date.getLstmDate(cl));

		for (int i = offset; i < 0; i++) {
			cl.add(Calendar.HOUR, 1);
			load(lstm_date.getLstmDate(cl));
		}

		return 0;
	}

	public int load(LSTM_Date date) {
        this.input_path_str = INPUT_PATH + "\\" + date.year + "\\" + date.month + "\\" + date.day;
		String startFiles = input_path_str + "\\" + "I_" + date.year + date.month + date.day + date.hour + date.minute + "_";

		ArrayList<File> csvFiles = new ArrayList<File>();
		File dir = new File(input_path_str);
		File[] files = dir.listFiles();

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
	
				if (file.toString().startsWith(startFiles)){
					csvFiles.add(file);
				}
			}
	
			int fileIndex = -1;
			Long minDate = 99991231235959L;
			int len_ext = 4;
			int len_yyyyMMddHHmmss = 14;
	
			for (int i = 0; i < csvFiles.size(); i++) {
				int length = csvFiles.get(i).toString().length();
				String fileDate = csvFiles.get(i).toString().substring(length - (len_yyyyMMddHHmmss + len_ext), length - len_ext);
				Long num_date = Long.parseLong(fileDate);
	
				if (num_date < minDate) {
					minDate = num_date;
					fileIndex = i;
				}
			}
	
			if (fileIndex != -1) {
				input_path = Path.of(csvFiles.get(fileIndex).toString());
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
	
				z_train[WindowSize - 1][targetIndex] = Double.parseDouble(items[0]);
	
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
