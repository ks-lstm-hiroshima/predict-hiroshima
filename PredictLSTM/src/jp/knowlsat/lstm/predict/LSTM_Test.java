package jp.knowlsat.lstm.predict;

public class LSTM_Test {
	public static void test(LSTM lstm) {
		double[][][] out = new double[lstm.Layers][lstm.window][lstm.nHidden];

		for (int i = 0; i < lstm.Layers; i++) {
			double[] y_prev = new double[lstm.nHidden];
			double[] c_prev = new double[lstm.nHidden];

			for (int j = 0; j < lstm.window; j++) {
				if (i == 0) {
					lstm.hiddenLayer[i].forwardTest(lstm.z_train[j], y_prev, c_prev);
				} else {
					lstm.hiddenLayer[i].forwardTest(out[i - 1][j], y_prev, c_prev);
				}

				for (int k = 0; k < lstm.nHidden; k++) {
					out[i][j][k] = lstm.hiddenLayer[i].Y[k];

					y_prev[k] = lstm.hiddenLayer[i].Y[k];
					c_prev[k] = lstm.hiddenLayer[i].C[k];
				}
			}
		}

		LSTM_Output output = lstm.outputLayer.test(out[lstm.Layers - 1][lstm.window - 1], lstm.z_target[lstm.window - 1],
			lstm.z_flag[lstm.window - 1], lstm.STATE_THRESHOLD, lstm.z_datetimes[lstm.window - 1], lstm.z_coDatetimes[lstm.window - 1],
			lstm.incident, lstm.minute, lstm.z_train);

		output.print();
		output.outputInputCSV();
		output.outputStatisticsCSV();
	}

	public static String p(double d) {
		return String.format("%.2f", d);
	}

}
