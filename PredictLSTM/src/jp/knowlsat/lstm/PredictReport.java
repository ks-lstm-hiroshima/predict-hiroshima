package jp.knowlsat.lstm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

import jp.knowlsat.lstm.predict.DataRealtimeCSV;

public class PredictReport {
	public static final String ReportPath = "report\\";
	public static final String SearchPath = "output\\statistics";

	public static void main(String[] args) {
		LocalDateTime t_date = LocalDateTime.now();
		DateTimeFormatter t_df = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
		String date_str = t_df.format(t_date);

		PredictReport pr = new PredictReport();
		pr.generateStatistics(date_str);
		pr.generateReport(date_str);
	}

	public int generateStatistics(String date_str) {
		FileSearch fs = new FileSearch();
		File[] files = fs.listFiles(SearchPath, "*.csv");
		ArrayList<File> lastFiles = getLast(files);

		String fileName = "AllRecords_" + date_str + ".csv";
		Path path = Path.of(ReportPath + fileName);

		File file;
		FileWriter fileWriter;

		try {
			file = Files.createFile(path).toFile();
		} catch (IOException e) {
			System.out.println("Failed to create file. " + e.getMessage());
			return -1;
		}

		try {
			fileWriter = new FileWriter(file);
		} catch (IOException e) {
			System.out.println("Failed to create file writer. " + e.getMessage());
			return -2;
		}

		try {
			if (lastFiles.size() != 0) {
				fileWriter.write(getHeader(lastFiles.get(0)));
				fileWriter.write("\n");

				for (int i = 0; i < lastFiles.size(); i++) {
					fileWriter.write(getRecords(lastFiles.get(i)));
					fileWriter.write("\n");
				}

				fileWriter.flush();
			}
		} catch (IOException e) {
			System.out.println("Failed to write. " + e.getMessage());
		}

		try {
			fileWriter.close();
		} catch (Exception e) {
			System.out.println("Failed to close file writer. " + e.getMessage());
			return -3;
		}

		return 0;
	}

	public int generateReport(String date_str) {
		String fileName = "PredictReport_" + date_str + ".txt";
		Path path = Path.of(ReportPath + fileName);

		File file;
		FileWriter fileWriter;

		try {
			file = Files.createFile(path).toFile();
		} catch (IOException e) {
			System.out.println("Failed to create file. " + e.getMessage());
			return -1;
		}

		try {
			fileWriter = new FileWriter(file);
		} catch (IOException e) {
			System.out.println("Failed to create file writer. " + e.getMessage());
			return -2;
		}

		DataResult dr = new DataResult();

		try {
			if (dr.result.size() != 0) {
				fileWriter.write("date,predict,real,error,squareError,per\n");

				for (int i = 0; i < dr.result.size(); i++) {
					fileWriter.write(dr.result.get(i)[0]);
					fileWriter.write(",");
					fileWriter.write(dr.result.get(i)[1]);
					fileWriter.write(",");
					fileWriter.write(dr.result.get(i)[2]);
					fileWriter.write(",");
					fileWriter.write(dr.result.get(i)[3]);
					fileWriter.write(",");
					fileWriter.write(dr.result.get(i)[4]);
					fileWriter.write(",");
					fileWriter.write(dr.result.get(i)[5]);
					fileWriter.write("\n");
				}

				fileWriter.flush();
			}
		} catch (IOException e) {
			System.out.println("Failed to write. " + e.getMessage());
		}

		try {
			fileWriter.close();
		} catch (Exception e) {
			System.out.println("Failed to close file writer. " + e.getMessage());
			return -3;
		}

		String summaryFileName = "PredictSummary_" + date_str + ".txt";
		Path summaryPath = Path.of(ReportPath + summaryFileName);

		File summaryFile;
		FileWriter summaryFileWriter;

		try {
			summaryFile = Files.createFile(summaryPath).toFile();
		} catch (IOException e) {
			System.out.println("Failed to create file. " + e.getMessage());
			return -1;
		}

		try {
			summaryFileWriter = new FileWriter(summaryFile);
		} catch (IOException e) {
			System.out.println("Failed to create file writer. " + e.getMessage());
			return -2;
		}

		try {
			if (dr.result.size() != 0) {
				summaryFileWriter.write("予測総数：" + dr.result.size() + "\n");
				summaryFileWriter.write("平均二乗誤差：" + dr.total_squareError / dr.result.size() + "\n");
				summaryFileWriter.write("平均パーセント誤差：" + dr.total_per / dr.result.size() + "\n");

				summaryFileWriter.flush();
			}
		} catch (IOException e) {
			System.out.println("Failed to write. " + e.getMessage());
		}

		try {
			summaryFileWriter.close();
		} catch (Exception e) {
			System.out.println("Failed to close file writer. " + e.getMessage());
			return -3;
		}

		return 0;
	}

	public String getHeader(File file) {
		FileReader fileReader;
		BufferedReader bufferedReader;
		String items;

		try {
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);

			items = bufferedReader.readLine();
		} catch (IOException e) {
			System.out.println("Failed to open file. " + e.getMessage());
			return null;
		}

		try {
			bufferedReader.close();
		} catch (IOException e) {
			System.out.println("Failed to close file. " + e.getMessage());
			return null;
		}

		return items;
	}

	public String getRecords(File file) {
		FileReader fileReader;
		BufferedReader bufferedReader;
		String items;

		try {
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);

			bufferedReader.readLine();
			items = bufferedReader.readLine();
		} catch (IOException e) {
			System.out.println("Failed to open file. " + e.getMessage());
			return null;
		}

		try {
			bufferedReader.close();
		} catch (IOException e) {
			System.out.println("Failed to close file. " + e.getMessage());
			return null;
		}

		return items;
	}

	public ArrayList<File> getLast(File[] files) {
		ArrayList<File> lastFiles = new ArrayList<File>();
		int len_ext = 4;
		int len_yyyyMMddHHmmss = 14;

		for (int i = 0; i < files.length; i++) {
			String lastName = files[i].toString();
			int length = lastName.length();
			String nowOutputDate = lastName.substring(0, length - (len_yyyyMMddHHmmss + len_ext + 1));

			if (i + 1 == files.length) {
				lastFiles.add(files[i]);
			} else {
				String lastNameNext = files[i + 1].toString();
				String nextOutputDate = lastNameNext.substring(0, length - (len_yyyyMMddHHmmss + len_ext + 1));

				if (nowOutputDate.equals(nextOutputDate)) {
					continue;
				} else {
					lastFiles.add(files[i]);
				}
			}
		}

		return lastFiles;
	}

	public class FileSearch {
		public static final int TYPE_FILE_OR_DIR = 1;
		public static final int TYPE_FILE = 2;
		public static final int TYPE_DIR = 3;

		public File[] listFiles(String directoryPath, String fileName) {
			if (fileName != null) {
				fileName = fileName.replace(".", "\\.");
				fileName = fileName.replace("*", ".*");
			}

			return listFiles(directoryPath, fileName, TYPE_FILE, true, 0);
		}

		@SuppressWarnings("unchecked")
		public File[] listFiles(String directoryPath, String fileNamePattern, int type, boolean isRecursive,
				int period) {
			File dir = new File(directoryPath);

			if (!dir.isDirectory()) {
				throw new IllegalArgumentException("引数で指定されたパス[" + dir.getAbsolutePath() + "]はディレクトリではありません。");
			}

			File[] files = dir.listFiles();

			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				addFile(type, fileNamePattern, set, file, period);

				if (isRecursive && file.isDirectory()) {
					listFiles(file.getAbsolutePath(), fileNamePattern, type, isRecursive, period);
				}
			}

			return (File[]) set.toArray(new File[set.size()]);
		}

		@SuppressWarnings("unchecked")
		private void addFile(int type, String match, @SuppressWarnings("rawtypes") TreeSet set, File file, int period) {
			switch (type) {
			case TYPE_FILE:
				if (!file.isFile()) {
					return;
				}

				break;

			case TYPE_DIR:
				if (!file.isDirectory()) {
					return;
				}

				break;
			}

			if (match != null && !file.getName().matches(match)) {
				return;
			}

			if (period != 0) {
				Date lastModifiedDate = new Date(file.lastModified());
				String lastModifiedDateStr = new SimpleDateFormat("yyyyMMdd")
						.format(lastModifiedDate);
				long oneDayTime = 24L * 60L * 60L * 1000L;
				long periodTime = oneDayTime * Math.abs(period);
				Date designatedDate = new Date(System.currentTimeMillis() - periodTime);
				String designatedDateStr = new SimpleDateFormat("yyyyMMdd")
						.format(designatedDate);

				if (period > 0) {
					if (lastModifiedDateStr.compareTo(designatedDateStr) < 0) {
						return;
					}
				} else {
					if (lastModifiedDateStr.compareTo(designatedDateStr) > 0) {
						return;
					}
				}
			}

			set.add(file);
		}

		@SuppressWarnings("rawtypes")
		private TreeSet set = new TreeSet();

		public void clear() {
			set.clear();
		}
	}

	public class DataResult {
		public ArrayList<String[]> result;
		public double total_squareError = 0.0;
		public double total_per = 0.0;

		public ArrayList<String[]> csv;
		public File report;
		public HashMap<String, String> realHash;

		public DataResult() {
			result = new ArrayList<String[]>();
			realHash = new HashMap<String, String>();

			setRealtimeCSV();
			setAllRecordsCSV();
			setRealtimeHashMap();
			setStatisticsInfo();
		}

		public int setRealtimeCSV() {
			String networkFileName = "setting/setting_network.properties";
			FileInputStream nin = null;
			try {
				nin = new FileInputStream(networkFileName);
			} catch (FileNotFoundException e) {
				System.out.println(e.toString());
				System.exit(-1);
			}

			Properties network_settings = new Properties();

			try {
				network_settings.load(nin);
			} catch (IOException e) {
				System.out.println(e.toString());
				System.exit(-1);
			}

			String network_path = network_settings.getProperty("RealtimePath_MIN01");
			DataRealtimeCSV dr = new DataRealtimeCSV(network_path);

			if (dr.setListCSV() < 0) {
				return -1;
			}

			csv = dr.getListCSV();

			return 0;
		}

		public int setAllRecordsCSV() {
			FileSearch fs = new FileSearch();
			File[] files = fs.listFiles(ReportPath, "*.csv");
			report = files[files.length - 1];

			return 0;
		}

		public int setRealtimeHashMap() { // index = 84 : nakajia
			for (int i = 0; i < csv.size(); i++) {
				String date = csv.get(i)[0];
				String key = date.replaceAll("-", "/");
				String value = csv.get(i)[84];

				realHash.put(key, value);
			}

			return 0;
		}

		public int setStatisticsInfo() {
			FileReader fileReader;
			BufferedReader bufferedReader;
			String items;

			try {
				fileReader = new FileReader(report);
				bufferedReader = new BufferedReader(fileReader);
				bufferedReader.readLine();

				while ((items = bufferedReader.readLine()) != null) {
					String[] itemArray = items.split(",");

					String key = itemArray[0];
					String predictString = itemArray[1];
					String realString = realHash.get(key);
					double predict;
					double real;
					double error;
					double squareError;
					double per;

					if (realString != null && !realString.isEmpty()
							&& !itemArray[itemArray.length - 1].equals(true)
					/* && Double.parseDouble(itemArray[6]) > 0.5 */) {
						predict = Double.parseDouble(predictString);
						real = Double.parseDouble(realString);
						error = predict - real;
						squareError = error * error;
						per = 0.0;

						if (real != 0.0) {
							per = (predict - real) * 100.0 / real;
						}

						String[] record = { key, predictString, realString, String.valueOf(error),
								String.valueOf(squareError), String.valueOf(per) };
						result.add(record);

						total_squareError += squareError;
						total_per += Math.abs(per);
					}
				}
			} catch (IOException e) {
				System.out.println("Failed to open file. " + e.getMessage());
				return -1;
			}

			try {
				bufferedReader.close();
			} catch (IOException e) {
				System.out.println("Failed to close file. " + e.getMessage());
				return -2;
			}

			return 0;
		}
	}

}
