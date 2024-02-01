package jp.knowlsat.lstm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

	public double[][][] z_train;
	public double[][][] z_target;
	public boolean[][] z_flag;
	public String[][] z_datetimes;
	public String[][] z_coDatetimes;

	public int targetIndex;
	public double STATE_THRESHOLD;
	public int testRealSize;

	public void setData(double[][][] z_train, double[][][] z_target, boolean[][] z_flag, String[][] z_datetimes, String[][] z_coDatetimes) {
		this.z_train = z_train;
		this.z_target = z_target;
		this.z_flag = z_flag;
		this.z_datetimes = z_datetimes;
		this.z_coDatetimes = z_coDatetimes;
	}

	public LSTM(int Layers, int window, int nIn, int nHidden, int nOut, int targetIndex, int peephole_mode, int elu_mode, double STATE_THRESHOLD, DataSetting ds, int testRealSize) {
		pr = new PrintResult(Path.of("result.log"));

		this.Layers = Layers;
		this.window = window;
		this.hiddenLayer = new LSTM_HiddenLayer[Layers];
		this.nIn = nIn;
		this.nHidden = nHidden;
		this.targetIndex = targetIndex;
		this.STATE_THRESHOLD = STATE_THRESHOLD;
		this.testRealSize = testRealSize;

		for (int i = 0; i < Layers; i++) {
			this.hiddenLayer[i] = new LSTM_HiddenLayer(nIn, nHidden, peephole_mode, elu_mode);
		}

		this.outputLayer = new LSTM_OutputLayer(nHidden, nOut, pr, this, ds);
	}

	public static void main(String[] args) {
		ArrayList<String> settingFileNames = new ArrayList<>(args.length <= 1 ? 1 : args.length);

		if (args.length == 0) {
			settingFileNames.add("setting/setting_default.properties");
		} else if (args.length == 1) {
			settingFileNames.add(args[0]);
		} else {
			settingFileNames.addAll(
					Arrays.stream(args).map((String str) -> "my_settings/" + str).toList());
		}

		for (String settingFileName : settingFileNames) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(settingFileName);
			} catch (FileNotFoundException e) {
				System.out.println(e.toString());
				continue;
			}

			Properties settings = new Properties();
			try {
				settings.load(in);
			} catch (IOException e) {
				System.out.println(e.toString());
				continue;
			}

			int ammonia_mode = Integer.parseInt(settings.getProperty("ammonia_mode"));
			int inputSeries = Integer.parseInt(settings.getProperty("inputSeries"));
			int outDataSize = Integer.parseInt(settings.getProperty("outDataSize"));
			double KSPP = Double.parseDouble(settings.getProperty("KSPP"));
			double STATE_THRESHOLD = Double.parseDouble(settings.getProperty("STATE_THRESHOLD"));			
			int mode = Integer.parseInt(settings.getProperty("mode"));
			int windowSize = Integer.parseInt(settings.getProperty("windowSize"));
			int trainRealSize = Integer.parseInt(settings.getProperty("trainRealSize"));
			int testRealSize = Integer.parseInt(settings.getProperty("testRealSize"));

			DataSetting ds = null;
			try {
				ds = new DataSetting(inputSeries, outDataSize, trainRealSize, testRealSize, windowSize, mode, KSPP, ammonia_mode);
			} catch (IOException e) {
				System.out.println(e.toString());
				continue;
			}
			int dataType = ds.getDataTypeSize();
			int inDataSize = dataType;
			int trainSize = ds.getTrainSize();
			int validationSize = ds.getValidationSize();
			int nHidden = Integer.parseInt(settings.getProperty("nHidden"));
			int Layers = Integer.parseInt(settings.getProperty("Layers"));
			int peephole_mode = Integer.parseInt(settings.getProperty("peephole_mode"));
			int elu_mode = Integer.parseInt(settings.getProperty("elu_mode"));

			LSTM lstm = new LSTM(Layers, windowSize, inDataSize, nHidden, outDataSize, ds.targetIndexes[ds.nakajiaIndexInTargets], peephole_mode, elu_mode, STATE_THRESHOLD, ds, testRealSize);
			LSTM_Load.load(lstm);

			try {
				PrintResult.copyFile(Path.of(settingFileName));
			} catch (IOException e) {
				System.out.println(e.toString());
			}
			lstm.pr.wl("ammonia_mode:" + ammonia_mode);
			lstm.pr.wl("inputSeries:" + inputSeries);
			lstm.pr.wl("outDataSize:" + outDataSize);
			lstm.pr.wl("KSPP:" + KSPP);
			lstm.pr.wl("STATE_THRESHOLD:" + STATE_THRESHOLD);
			lstm.pr.wl("mode:" + mode);
			lstm.pr.wl("windowSize:" + windowSize);
			lstm.pr.wl("TotalDataType:" + dataType);
			lstm.pr.wl("trainDataType:" + inDataSize);
			lstm.pr.wl("validationDataType:" + outDataSize);
			lstm.pr.wl("TrainSize:" + trainSize);
			lstm.pr.wl("ValidationSize:" + validationSize);
			lstm.pr.wl("TestSize:" + testRealSize);
			lstm.pr.wl("nHidden:" + nHidden);
			lstm.pr.wl("Layers:" + Layers);
			lstm.pr.wl("peephole_mode:" + peephole_mode);
			lstm.pr.wl("elu_mode:" + elu_mode);

			DataMinibatch dm = new DataMinibatch(ds, trainSize, validationSize, testRealSize, windowSize, dataType, ds.targetIndexes[ds.nakajiaIndexInTargets], outDataSize);
			lstm.setData(dm.z_train, dm.z_target, dm.z_flag, dm.z_datetimes, dm.z_coDatetimes);

			String FileName = "result.log";
			PrintResultBuffer prb = new PrintResultBuffer(Path.of(FileName), testRealSize * 300);
			LSTM_Test.test(lstm, testRealSize, lstm.pr, prb, FileName);

			PrintResult.setNullClassFields();
			LSTM_Test.reInitStaticFielsds();
		}
	}

}
