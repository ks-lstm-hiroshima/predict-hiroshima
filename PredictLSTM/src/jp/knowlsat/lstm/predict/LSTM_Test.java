package jp.knowlsat.lstm.predict;

public class LSTM_Test {
	public static double mse_min = Double.MAX_VALUE;
	public static String mse_file_min;
	public static double mse2_min = Double.MAX_VALUE;
	public static String mse2_file_min;
	public static double ave_min = Double.MAX_VALUE;
	public static String ave_file_min;
	public static double ave2_min = Double.MAX_VALUE;
	public static String ave2_file_min;

	public static void reInitStaticFielsds() {
		mse_min = Double.MAX_VALUE;
		mse_file_min = null;
		mse2_min = Double.MAX_VALUE;
		mse2_file_min = null;
		ave_min = Double.MAX_VALUE;
		ave_file_min = null;
		ave2_min = Double.MAX_VALUE;
		ave2_file_min = null;
	}

	public static void test(LSTM lstm, int size, PrintResult spr, PrintResultBuffer prb, String FileName) {
		boolean mse_flag = false;
		double mse_error = 0.0;
		boolean mse2_flag = false;
		double mse2_error = 0.0;
		boolean ave_flag = false;
		double ave_error = 0.0;
		boolean ave2_flag = false;
		double ave2_error = 0.0;
		LSTM_OutputLayer.ave_per = 0.0;
		LSTM_OutputLayer.ave2_per = 0.0;
		LSTM_OutputLayer.mse = 0.0;
		LSTM_OutputLayer.mse2 = 0.0;
		LSTM_OutputLayer.all = 0;
		LSTM_OutputLayer.off_err = 0;
		LSTM_OutputLayer.off_max = 0;
		LSTM_OutputLayer.on_err = 0;
		LSTM_OutputLayer.on_max = 0;

		for (int h = 0; h < size; h++) {
			double[][][] out = new double[lstm.Layers][lstm.window][lstm.nHidden];

			for (int i = 0; i < lstm.Layers; i++) {
				double[] y_prev = new double[lstm.nHidden];
				double[] c_prev = new double[lstm.nHidden];

				for (int j = 0; j < lstm.window; j++) {
					if (i == 0) {
						lstm.hiddenLayer[i].forwardTest(lstm.z_train[h][j], y_prev, c_prev);
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

			lstm.outputLayer.test(out[lstm.Layers - 1][lstm.window - 1], lstm.z_target[h][lstm.window - 1],
				h, prb, lstm.z_flag[h][lstm.window - 1], lstm.STATE_THRESHOLD,
				lstm.z_datetimes[h][lstm.window - 1], lstm.z_coDatetimes[h][lstm.window - 1]);
		}

		mse_error = LSTM_OutputLayer.mse / (LSTM_OutputLayer.on_max - lstm.window);

		if (mse_error < mse_min) {
			mse_flag = true;
			mse_min = mse_error;
			mse_file_min = FileName;
		}

		if (mse_flag) {
			System.out.println("最小平均二乗誤差ファイル：" + mse_file_min + ", 最小平均二乗誤差：" + mse_min);
			spr.wl("最小平均二乗誤差ファイル：" + mse_file_min + ", 最小平均二乗誤差：" + mse_min);
			prb.wl("最小平均二乗誤差ファイル：" + mse_file_min + ", 最小平均二乗誤差：" + mse_min);
		}

		prb.wl("平均二乗誤差：" + mse_error);

		mse2_error = LSTM_OutputLayer.mse2 / (LSTM_OutputLayer.all - lstm.window);

		if (mse2_error < mse2_min) {
			mse2_flag = true;
			mse2_min = mse2_error;
			mse2_file_min = FileName;
		}

		if (mse2_flag) {
			System.out.println("補正最小平均二乗誤差ファイル：" + mse2_file_min + ", 補正最小平均二乗誤差：" + mse2_min);
			spr.wl("補正最小平均二乗誤差ファイル：" + mse2_file_min + ", 補正最小平均二乗誤差：" + mse2_min);
			prb.wl("補正最小平均二乗誤差ファイル：" + mse2_file_min + ", 補正最小平均二乗誤差：" + mse2_min);
		}

		prb.wl("補正平均二乗誤差：" + mse2_error);

		ave_error = LSTM_OutputLayer.ave_per / (LSTM_OutputLayer.on_max - lstm.window);

		if (ave_error < ave_min) {
			ave_flag = true;
			ave_min = ave_error;
			ave_file_min = FileName;
		}

		if (ave_flag) {
			System.out.println("最小平均絶対パーセント誤差ファイル：" + ave_file_min + ", 最小平均絶対パーセント誤差：" + ave_min);
			spr.wl("最小平均絶対パーセント誤差ファイル：" + ave_file_min + ", 最小平均絶対パーセント誤差：" + ave_min);
			prb.wl("最小平均絶対パーセント誤差ファイル：" + ave_file_min + ", 最小平均絶対パーセント誤差：" + ave_min);
		}

		prb.wl("平均絶対パーセント誤差：" + ave_error);

		ave2_error = LSTM_OutputLayer.ave2_per / (LSTM_OutputLayer.all - lstm.window);

		if (ave2_error < ave2_min) {
			ave2_flag = true;
			ave2_min = ave2_error;
			ave2_file_min = FileName;
		}

		if (ave2_flag) {
			System.out.println("補正最小平均絶対パーセント誤差ファイル：" + ave2_file_min + ", 補正最小平均絶対パーセント誤差：" + ave2_min);
			spr.wl("補正最小平均絶対パーセント誤差ファイル：" + ave2_file_min + ", 補正最小平均絶対パーセント誤差：" + ave2_min);
			prb.wl("補正最小平均絶対パーセント誤差ファイル：" + ave2_file_min + ", 補正最小平均絶対パーセント誤差：" + ave2_min);
		}

		prb.wl("補正平均絶対パーセント誤差：" + ave2_error);
		
		if (mse_flag || mse2_flag || ave_flag || ave2_flag) {
			System.out.println("中次亜注入OFF時におけるAI側の注入ON誤指示率：" + LSTM_OutputLayer.off_err + "/" + LSTM_OutputLayer.off_max + " "
					+ p(100.0 * LSTM_OutputLayer.off_err / LSTM_OutputLayer.off_max) + "%");
			spr.wl("中次亜注入OFF時におけるAI側の注入ON誤指示率：" + LSTM_OutputLayer.off_err + "/" + LSTM_OutputLayer.off_max + " "
					+ p(100.0 * LSTM_OutputLayer.off_err / LSTM_OutputLayer.off_max) + "%");
			prb.wl("中次亜注入OFF時におけるAI側の注入ON誤指示率：" + LSTM_OutputLayer.off_err + "/" + LSTM_OutputLayer.off_max + " "
					+ p(100.0 * LSTM_OutputLayer.off_err / LSTM_OutputLayer.off_max) + "%");

			System.out.println("中次亜注入ON時におけるAI側の注入OFF誤指示率：" + LSTM_OutputLayer.on_err + "/" + LSTM_OutputLayer.on_max + " "
					+ p(100.0 * LSTM_OutputLayer.on_err / LSTM_OutputLayer.on_max) + "%");
			spr.wl("中次亜注入ON時におけるAI側の注入OFF誤指示率：" + LSTM_OutputLayer.on_err + "/" + LSTM_OutputLayer.on_max + " "
					+ p(100.0 * LSTM_OutputLayer.on_err / LSTM_OutputLayer.on_max) + "%");
			prb.wl("中次亜注入ON時におけるAI側の注入OFF誤指示率：" + LSTM_OutputLayer.on_err + "/" + LSTM_OutputLayer.on_max + " "
					+ p(100.0 * LSTM_OutputLayer.on_err / LSTM_OutputLayer.on_max) + "%");

			System.out.println("中次亜注入ON/OFF指示正解率：" + (LSTM_OutputLayer.all - LSTM_OutputLayer.on_err - LSTM_OutputLayer.off_err) + "/" + LSTM_OutputLayer.all + " "
					+ p(100.0 * (LSTM_OutputLayer.all - LSTM_OutputLayer.on_err - LSTM_OutputLayer.off_err) / LSTM_OutputLayer.all) + "%");
			spr.wl("中次亜注入ON/OFF指示正解率：" + (LSTM_OutputLayer.all - LSTM_OutputLayer.on_err - LSTM_OutputLayer.off_err) + "/" + LSTM_OutputLayer.all + " "
					+ p(100.0 * (LSTM_OutputLayer.all - LSTM_OutputLayer.on_err - LSTM_OutputLayer.off_err) / LSTM_OutputLayer.all) + "%");
			prb.wl("中次亜注入ON/OFF指示正解率：" + (LSTM_OutputLayer.all - LSTM_OutputLayer.on_err - LSTM_OutputLayer.off_err) + "/" + LSTM_OutputLayer.all + " "
					+ p(100.0 * (LSTM_OutputLayer.all - LSTM_OutputLayer.on_err - LSTM_OutputLayer.off_err) / LSTM_OutputLayer.all) + "%");
			
			prb.writeToFile();
		}
	}

	public static String p(double d) {
		return String.format("%.2f", d);
	}

}
