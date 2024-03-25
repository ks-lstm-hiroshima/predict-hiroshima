package jp.knowlsat.lstm.predict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LSTM_Output {
	public static final String INPUT_PATH = "output\\input";
	public static final String STATISTICS_PATH = "output\\statistics";

	public String z_coDatetime; // 定刻日時
	public double inv_p; // 予測値
	public double next; // 正規化予測値
	public boolean onJudge; // 取水判断
	public boolean incident; // インシデント
	public String minute; // タイムモード
	public double[] input; // 入力データ[dataType]
	public double[] origin_input; // オリジナル入力データ[dataType]

	public String prev_z_coDatetime; // 前回定刻日時
	public double prev_inv_p; // 前回予測値
	public boolean prev_incident; // 前回インシデント
	public double prev_real_nakajia; // 前回実測値

	public String year;
	public String month;
	public String day;
	public String hour;

	public Path input_path;
	public Path statistics_path;

	public double prev_e; // 前回誤差
	public double prev_PredictSquaredError; // 前回二乗誤差
	public double prev_per; // 前回%誤差

	public boolean z_flag; // 取水フラグ

	//	public String z_datetime; // 日時
	//	public double inv_t; // 実測値
	//	public double e; // 誤差
	//	public double PredictSquaredError; // 二乗誤差
	//	public double per; // %誤差
	//	public double e2; // 補正誤差
	//	public double PredictSquaredError2; // 補正二乗誤差
	//	public double per2; // 補正%誤差

	public LSTM_Output(boolean incident, String z_coDatetime, String minute, int dataSize) {
		this.z_coDatetime = z_coDatetime;
		this.incident = incident;
		this.minute = minute;
		this.input = new double[dataSize];
		this.origin_input = new double[dataSize];

		setInputPath();
	}

	public LSTM_Output(String z_coDatetime, String z_datetime, boolean onJudge, boolean z_flag, double inv_p,
			double inv_t, double e, double PredictSquaredError, double per, double e2, double PredictSquaredError2,
			double per2, double next, boolean incident, String minute, double[] input, double[] origin_input) {
		this.z_coDatetime = z_coDatetime;
		this.inv_p = inv_p;
		this.next = next;
		this.onJudge = onJudge;
		this.incident = incident;
		this.minute = minute;
		this.input = input;
		this.origin_input = origin_input;

		this.z_flag = z_flag;

		// this.z_datetime = z_datetime;
		// this.inv_t = inv_t;
		// this.e = e;
		// this.PredictSquaredError = PredictSquaredError;
		// this.per = per;
		// this.e2 = e2;
		// this.PredictSquaredError2 = PredictSquaredError2;
		// this.per2 = per2;

		setInputPath();
	}

	public void setPrevResult(String prev_z_coDatetime, double prev_inv_p, boolean prev_incident) {
		this.prev_z_coDatetime = prev_z_coDatetime;
		this.prev_inv_p = prev_inv_p;
		this.prev_incident = prev_incident;
	}

	public void setPrevRealNakajia(double prev_real_nakajia) {
		this.prev_real_nakajia = prev_real_nakajia;
	}

	public void setError() {
		if (prev_z_coDatetime != null) {
			if (prev_real_nakajia == 0.0 && prev_inv_p == 0.0) {
				prev_e = 0.0;
				prev_PredictSquaredError = 0.0;
				prev_per = 0.0;
			} else if (prev_real_nakajia == 0.0) {
				prev_e = prev_inv_p - prev_real_nakajia;
				prev_PredictSquaredError = prev_e * prev_e;
				prev_per = 100.0;
			} else if (prev_inv_p == 0.0) {
				prev_e = prev_inv_p - prev_real_nakajia;
				prev_PredictSquaredError = prev_e * prev_e;
				prev_per = -100.0;
			} else {
				prev_e = prev_inv_p - prev_real_nakajia;
				prev_PredictSquaredError = prev_e * prev_e;
				prev_per = (prev_inv_p - prev_real_nakajia) * 100.0 / prev_real_nakajia;
			}
		} else {
			prev_e = 0.0;
			prev_PredictSquaredError = 0.0;
			prev_per = 0.0;
		}
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
		} catch (Exception e) {
			System.out.println("Failed to create error log directory. " + e.getMessage());
			System.exit(-1);
		}

		LocalDateTime t_date = LocalDateTime.now();
		DateTimeFormatter t_df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		input_path = Path.of(INPUT_PATH + "\\" + year + "\\" + month + "\\" + day,
				"I_" + year + month + day + hour + minute + "_" + t_df.format(t_date) + ".csv");
		statistics_path = Path.of(STATISTICS_PATH + "\\" + year + "\\" + month + "\\" + day,
				"S_" + year + month + day + hour + minute + "_" + t_df.format(t_date) + ".csv");
	}

	public void print() {
		System.out.println("--- 今回実施の予測実行結果 ---");
		System.out.print("予測時刻");
		System.out.print(",");
		System.out.print("中次亜塩素酸注入率予測値（正規化前）");
		System.out.print(",");
		System.out.print("インシデント発生フラグ（false：正常, true：異常）");
		System.out.println();
		System.out.print(z_coDatetime);
		System.out.print(",");
		System.out.print(e3(inv_p));
		System.out.print(",");
		System.out.print(incident);
		System.out.println();
		System.out.println();

		System.out.println("--- 前回実施の予測実行結果 ---");

		if (prev_z_coDatetime != null) {
			System.out.print("予測時刻");
			System.out.print(",");
			System.out.print("中次亜塩素酸注入率予測値（正規化前）");
			System.out.print(",");
			System.out.print("インシデント発生フラグ（false：正常, true：異常）");
			System.out.println();
			System.out.print(prev_z_coDatetime);
			System.out.print(",");
			System.out.print(e3(prev_inv_p));
			System.out.print(",");
			System.out.print(prev_incident);
			System.out.println();
			System.out.println();
		} else {
			System.out.println("前回実施の予測結果はありません");
			System.out.println();
		}

		System.out.println("--- 前回実施の中次亜実測値および誤差情報 ---");
		System.out.print("実測値（正規化前）");
		System.out.print(",");
		System.out.print("予測誤差");
		System.out.print(",");
		System.out.print("二乗誤差");
		System.out.print(",");
		System.out.print("％誤差");
		System.out.println();
		System.out.print(e3(prev_real_nakajia));
		System.out.print(",");
		System.out.print(e3(prev_e));
		System.out.print(",");
		System.out.print(e3(prev_PredictSquaredError));
		System.out.print(",");
		System.out.print(e3(prev_per));
		System.out.println();
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
			fileWriter.write("predictDate");
			fileWriter.write(",");
			fileWriter.write("next");
			fileWriter.write(",");
			fileWriter.write("onJudge");
			fileWriter.write(",");
			fileWriter.write("ammonia");
			fileWriter.write(",");
			fileWriter.write("incident");
			fileWriter.write("\n");
			fileWriter.write(z_coDatetime);
			fileWriter.write(",");
			fileWriter.write(Double.toString(next));
			fileWriter.write(",");
			fileWriter.write(Boolean.toString(onJudge));
			fileWriter.write(",");
			fileWriter.write(Double.toString(getAmmoniaNP()));
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
			fileWriter.write("predictDate");
			fileWriter.write(",");
			fileWriter.write("inv_p");
			fileWriter.write(",");
			fileWriter.write("onJudge");
			fileWriter.write(",");

			for (int i = 0; i < input.length; i++) {
				fileWriter.write("input_" + Integer.toString(i));
				fileWriter.write(",");
			}

			for (int i = 0; i < origin_input.length; i++) {
				fileWriter.write("origin_input_" + Integer.toString(i));
				fileWriter.write(",");
			}

			fileWriter.write("incident");
			// fileWriter.write(",");
			// fileWriter.write("inv_t");
			// fileWriter.write(",");
			// fileWriter.write("z_flag");
			// fileWriter.write(",");
			// fileWriter.write("e");
			// fileWriter.write(",");
			// fileWriter.write("PredictSquaredError");
			// fileWriter.write(",");
			// fileWriter.write("per");
			// fileWriter.write(",");
			// fileWriter.write("e2");
			// fileWriter.write(",");
			// fileWriter.write("PredictSquaredError2");
			// fileWriter.write(",");
			// fileWriter.write("per2");
			fileWriter.write("\n");

			fileWriter.write(z_coDatetime);
			fileWriter.write(",");
			fileWriter.write(Double.toString(inv_p));
			fileWriter.write(",");
			fileWriter.write(Boolean.toString(onJudge));
			fileWriter.write(",");

			for (int i = 0; i < input.length; i++) {
				fileWriter.write(Double.toString(input[i]));
				fileWriter.write(",");
			}

			for (int i = 0; i < origin_input.length; i++) {
				fileWriter.write(Double.toString(origin_input[i]));
				fileWriter.write(",");
			}

			fileWriter.write(Boolean.toString(incident));
			// fileWriter.write(",");
			// fileWriter.write(Double.toString(inv_t));
			// fileWriter.write(",");
			// fileWriter.write(Boolean.toString(z_flag));
			// fileWriter.write(",");
			// fileWriter.write(Double.toString(e));
			// fileWriter.write(",");
			// fileWriter.write(Double.toString(PredictSquaredError));
			// fileWriter.write(",");
			// fileWriter.write(Double.toString(per));
			// fileWriter.write(",");
			// fileWriter.write(Double.toString(e2));
			// fileWriter.write(",");
			// fileWriter.write(Double.toString(PredictSquaredError2));
			// fileWriter.write(",");
			// fileWriter.write(Double.toString(per2));
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

	public static double getAmmoniaNP() throws IOException {
		String fileName = "setting/setting_per_Ammonia.txt";
		File file = new File(fileName);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String text = bufferedReader.readLine();
		bufferedReader.close();

		return Double.parseDouble(text);
	}

	public static String e3(double d) {
		return String.format("%.2e", d);
	}

	public static String e4(double d) {
		return String.format("%.3e", d);
	}

}
