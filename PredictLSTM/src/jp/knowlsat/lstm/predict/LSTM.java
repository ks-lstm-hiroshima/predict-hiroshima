package jp.knowlsat.lstm.predict;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class LSTM {
	public LSTM_HiddenLayer[] hiddenLayer;
	public LSTM_OutputLayer outputLayer;
	public PrintResult pr;
	public int Layers;
	public int nIn;
	public int nHidden;
	public int minibatchSize;
	public int window;

	public double[][] z_train;
	public double[][] z_target;
	public boolean[] z_flag;
	public String[] z_datetimes;
	public String[] z_coDatetimes;
	public boolean incident;

	public int targetIndex;
	public double STATE_THRESHOLD;
	public int test_mode;
	public int ammonia_mode;
	public String minute;

	public void setData(double[][] z_train, double[][] z_target, boolean[] z_flag, String[] z_datetimes, String[] z_coDatetimes, boolean incident) {
		this.z_train = z_train;
		this.z_target = z_target;
		this.z_flag = z_flag;
		this.z_datetimes = z_datetimes;
		this.z_coDatetimes = z_coDatetimes;
		this.incident = incident;
	}

	public LSTM(int Layers, int window, int nIn, int nHidden, int nOut, int targetIndex, int peephole_mode, int elu_mode, double STATE_THRESHOLD,
			DataSetting ds, int testRealSize, int test_mode, int ammonia_mode, String minute) {
		this.Layers = Layers;
		this.window = window;
		this.hiddenLayer = new LSTM_HiddenLayer[Layers];
		this.nIn = nIn;
		this.nHidden = nHidden;
		this.targetIndex = targetIndex;
		this.STATE_THRESHOLD = STATE_THRESHOLD;
		this.test_mode = test_mode;
		this.ammonia_mode = ammonia_mode;
		this.minute = minute;

		for (int i = 0; i < Layers; i++) {
			this.hiddenLayer[i] = new LSTM_HiddenLayer(nIn, nHidden, peephole_mode, elu_mode);
		}

		this.outputLayer = new LSTM_OutputLayer(nHidden, nOut, this, ds);
	}

	public static void main(String[] args) {
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
		DataRealtimeCSV csv = new DataRealtimeCSV(network_path);
		csv.setListCSV();

		String fileName = "setting/setting_";
		String minute = "";

		if (args.length < 2 ) {
			fileName += "Ammonia_00";
		} else {
			if (args[0] != "0") {
				fileName += "Ammonia";
			} else {
				fileName += "NoAmmonia";
			}

			fileName += "_";

			if (args[1] == "-1") {
				fileName += "all";
			} else {
				minute = LSTM_Load.m(Integer.parseInt(args[1]));
				fileName += minute;
			}
		}

		fileName += ".properties";

		FileInputStream in = null;
		try {
			in = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			System.out.println(e.toString());
			System.exit(-1);
		}

		Properties settings = new Properties();
		try {
			settings.load(in);
		} catch (IOException e) {
			System.out.println(e.toString());
			System.exit(-1);
		}

		int ammonia_mode = Integer.parseInt(settings.getProperty("ammonia_mode"));
		int inputSeries = Integer.parseInt(settings.getProperty("inputSeries"));
		int outDataSize = Integer.parseInt(settings.getProperty("outDataSize"));
		double KSPP = Double.parseDouble(settings.getProperty("KSPP"));
		double STATE_THRESHOLD = Double.parseDouble(settings.getProperty("STATE_THRESHOLD"));			
		int test_mode = Integer.parseInt(settings.getProperty("test_mode"));
		int windowSize = Integer.parseInt(settings.getProperty("windowSize"));

		
		int passed = 1; // 1時刻前のデータを用いて予測（通常は1時刻）
		int test_size = 1; // not changed
		int dataNumForTest = passed + test_size;
		
		ArrayList<String[]> rTimeRecs = csv.getListCSV();		
		DataSetting ds = null;
		try {
			ds = new DataSetting(inputSeries, outDataSize, windowSize, test_mode, KSPP, ammonia_mode, dataNumForTest, rTimeRecs);
		} catch (IOException e) {
			System.out.println(e.toString());
			System.exit(-1);
		}
		int dataType = ds.getDataTypeSize();
		int inDataSize = dataType;
		int nHidden = Integer.parseInt(settings.getProperty("nHidden"));
		int Layers = Integer.parseInt(settings.getProperty("Layers"));
		int peephole_mode = Integer.parseInt(settings.getProperty("peephole_mode"));
		int elu_mode = Integer.parseInt(settings.getProperty("elu_mode"));

		// int test_size = 1; // not changed
		LSTM lstm = new LSTM(Layers, windowSize, inDataSize, nHidden, outDataSize, ds.targetIndexes[ds.nakajiaIndexInTargets],
				peephole_mode, elu_mode, STATE_THRESHOLD, ds, test_size, test_mode, ammonia_mode, minute);
		LSTM_Load.load(lstm);
		LSTM_Input input = new LSTM_Input(windowSize, dataType, outDataSize, ds.targetIndexes[ds.nakajiaIndexInTargets], minute);

		// -- predict start --
		// int passed = windowSize; // windowSize時刻前から現在までのデータを用いて再予測（障害などの理由により過去X時刻から再予測する場合）
		// int passed = 1; // 1時刻前のデータを用いて予測（通常は1時刻）

		for (int test_index = passed; test_index > 0; test_index--) {
			/* 下記をリアルタイム前処理データに差し替える */
			DataMinibatch dm = new DataMinibatch(ds, test_size, windowSize, dataType, ds.targetIndexes[ds.nakajiaIndexInTargets], outDataSize, test_index);
			input.set(dm.z_train[0], dm.z_target[0], dm.z_flag[0], dm.z_datetimes[0], dm.z_coDatetimes[0], false);

			// DataRealtime dr = new DataRealtime(ds, test_size, windowSize, dataType, ds.targetIndexes[ds.nakajiaIndexInTargets], outDataSize, test_index); // 修正イメージ
			// input.set(dr.z_train[0], dr.z_target[0], dr.z_flag[0], dr.z_datetimes[0], dr.z_coDatetimes[0], dr.incident); // 修正イメージ
			/* ここまで */

			input.allLoad();
			lstm.setData(input.z_train, input.z_target, input.z_flag, input.z_datetimes, input.z_coDatetimes, input.incident);
			LSTM_Test.test(lstm);
		}
		// -- predict end --
	}

}
