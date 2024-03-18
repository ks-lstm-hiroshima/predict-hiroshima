package jp.knowlsat.lstm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PredictReport {
	public static final String ReportPath = "report\\";

	public static void main(String[] args) {
		LocalDateTime t_date = LocalDateTime.now();
		DateTimeFormatter t_df = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
		String date_str = t_df.format(t_date);

		generateStatistics(date_str);
		generateReport(date_str);
	}

	public static int generateStatistics(String date_str) {
		String fileName = "AllRecord_" + date_str + ".csv";
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
			fileWriter.write("Not implemented");
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

	public static int generateReport(String date_str) {
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

		try {
			fileWriter.write("Not implemented");
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
