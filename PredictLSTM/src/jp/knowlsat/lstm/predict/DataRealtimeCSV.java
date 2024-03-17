package jp.knowlsat.lstm.predict;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class DataRealtimeCSV {
	public String network_path;
	public ArrayList<String[]> rTimeRecs = null;

	public DataRealtimeCSV(String network_path) {
		this.network_path = network_path;
		this.rTimeRecs = new ArrayList<>();
	}

	public int setListCSV() {
		File dir = new File(network_path);
		File[] files = dir.listFiles();

		if (files == null) {
			return -1;
		} else if (files.length == 0) {
			return -2;
		} else {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				FileReader fileReader;
				BufferedReader bufferedReader;
				String[] items;

				try {
					fileReader = new FileReader(file);
					bufferedReader = new BufferedReader(fileReader);
					bufferedReader.readLine(); // ヘッダ行は読み飛ばし
					String line = null;

					while ((line = bufferedReader.readLine()) != null) {
						items = line.split(",");
						rTimeRecs.add(items);
					}
				} catch (IOException e) {
					System.out.println("Failed to open file. " + e.getMessage());
					return -3;
				}

				try {
					bufferedReader.close();
				} catch (IOException e) {
					System.out.println("Failed to close file. " + e.getMessage());
					return -4;
				}
			}
		}

		return 0;
	}

	public ArrayList<String[]> getListCSV() {
		Collections.reverse(rTimeRecs);

		return rTimeRecs;
	}

}
