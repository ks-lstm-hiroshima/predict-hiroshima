package jp.knowlsat.lstm.predict;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LSTM_Output {
	public static final String INPUT_PATH = "output\\input";
	public static final String STATISTICS_PATH = "output\\statistics";

	public String z_coDatetime; // 定刻日時
	public String z_datetime; // 日時
	public boolean z_flag; // 取水フラグ
	public boolean onJudge; // 取水判断
	public double inv_p; // 予測値
	public double inv_t; // 実測値
	public double e; // 誤差
	public double PredictSquaredError; // 二乗誤差
	public double per; // %誤差
	public double e2; // 補正誤差
	public double PredictSquaredError2; // 補正二乗誤差
	public double per2; // 補正%誤差
	public double next; // 正規化予測値
	public boolean incident; // インシデント
	public String minute; // タイムモード
	public double[] input; // 入力データ[dataType]

	public String year;
	public String month;
	public String day;
	public String hour;

	public Path input_path;
	public Path statistics_path;

	public LSTM_Output(String z_coDatetime, String z_datetime, boolean onJudge, boolean z_flag, double inv_p, double inv_t,
			double e, double PredictSquaredError, double per, double e2, double PredictSquaredError2, double per2,
			double next, boolean incident, String minute, double[] input) {
		this.z_coDatetime = z_coDatetime;
		this.z_datetime = z_datetime;
		this.onJudge = onJudge;
		this.z_flag = z_flag;
		this.inv_p = inv_p;
		this.inv_t = inv_t;
		this.e = e;
		this.PredictSquaredError = PredictSquaredError;
		this.per = per;
		this.e2 = e2;
		this.PredictSquaredError2 = PredictSquaredError2;
		this.per2 = per2;
		this.next = next;
		this.incident = incident;
		this.minute = minute;
		this.input = input;

		setInputPath();
	}

	public void setInputPath() {
		year = z_coDatetime.substring(0, 4);
		month = z_coDatetime.substring(5, 7);
		day = z_coDatetime.substring(8, 10);
		hour = z_coDatetime.substring(11, 13);

		try {
			Files.createDirectories(Path.of(INPUT_PATH + "\\" + year));
			Files.createDirectories(Path.of(STATISTICS_PATH + "\\" + year));
			Files.createDirectories(Path.of(INPUT_PATH + "\\" + year + "\\" + month));
			Files.createDirectories(Path.of(STATISTICS_PATH + "\\" + year + "\\" + month));
			Files.createDirectories(Path.of(INPUT_PATH + "\\" + year + "\\" + month + "\\" + day));
			Files.createDirectories(Path.of(STATISTICS_PATH + "\\" + year + "\\" + month + "\\" + day));
		} catch (FileAlreadyExistsException e) {
		} catch (Exception e) {
			System.out.println("Failed to create error log directory. " + e.getMessage());
			System.exit(-1);
		}

		LocalDateTime t_date = LocalDateTime.now();
		DateTimeFormatter t_df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		input_path = Path.of(INPUT_PATH + "\\" + year + "\\" + month + "\\" + day, "I_" + year + month + day + hour + minute + "_" + t_df.format(t_date) + ".csv");
		statistics_path = Path.of(STATISTICS_PATH + "\\" + year + "\\" + month + "\\" + day, "S_" + year + month + day + hour + minute + "_" + t_df.format(t_date) + ".csv");
	}

	public void print() {
		System.out.print(inv_p);
		System.out.print(",");
		System.out.print(incident);
		System.out.println();
	}

	public int outputInputCSV() {
		File file;
		FileWriter fileWriter;
		try {
			file = Files.createFile(input_path).toFile();
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
			fileWriter.write("next");
			fileWriter.write(",");
			fileWriter.write("onJudge");
			fileWriter.write(",");
			fileWriter.write("incident");
			fileWriter.write("\n");
			fileWriter.write(Double.toString(next));
			fileWriter.write(",");
			fileWriter.write(Boolean.toString(onJudge));
			fileWriter.write(",");
			fileWriter.write(Boolean.toString(incident));
			fileWriter.write("\n");
			fileWriter.flush();
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

	public int outputStatisticsCSV() {
		File file;
		FileWriter fileWriter;
		try {
			file = Files.createFile(statistics_path).toFile();
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
			fileWriter.write("inv_p");
			fileWriter.write(",");
			fileWriter.write("inv_t");
			fileWriter.write(",");
			fileWriter.write("onJudge");
			fileWriter.write(",");
			fileWriter.write("z_flag");
			fileWriter.write(",");

			for (int i = 0; i < input.length; i++) {
				fileWriter.write("input_" + Integer.toString(i));
				fileWriter.write(",");
			}

			fileWriter.write("incident");
			fileWriter.write("\n");

			fileWriter.write(Double.toString(inv_p));
			fileWriter.write(",");
			fileWriter.write(Double.toString(inv_t));
			fileWriter.write(",");
			fileWriter.write(Boolean.toString(onJudge));
			fileWriter.write(",");
			fileWriter.write(Boolean.toString(z_flag));
			fileWriter.write(",");

			for (int i = 0; i < input.length; i++) {
				fileWriter.write(Double.toString(input[i]));
				fileWriter.write(",");
			}

			fileWriter.write(Boolean.toString(incident));
			fileWriter.write("\n");
			fileWriter.flush();
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

}
