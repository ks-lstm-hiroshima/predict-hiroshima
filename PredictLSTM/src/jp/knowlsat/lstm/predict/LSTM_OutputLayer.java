package jp.knowlsat.lstm.predict;

public class LSTM_OutputLayer {
	public int nHidden;
	public int nOut;
	public double[] B;
	public double[] W;

	public PrintResult pr;
	public LSTM lstm;
	private DataSetting ds;

	public LSTM_OutputLayer(int nHidden, int nOut, PrintResult pr, LSTM lstm, DataSetting ds) {
		this.nHidden = nHidden;
		this.nOut = nOut;

		this.pr = pr;
		this.lstm = lstm;		
		this.ds = ds;

		W = new double[nOut * nHidden];
		B = new double[nOut];
	}

	public static double ave_per;
	public static double ave2_per;
	public static double mse;
	public static double mse2;

	public static int all;
	public static int off_err;
	public static int off_max;
	public static int on_err;
	public static int on_max;

	public void test(double[] x, double[] t, int index, PrintResultBuffer prb, boolean z_flag, double stateThreshold, String z_datetime, String z_coDatetime) {
		double[] p = new double[nOut];

		for (int j = 0; j < nOut; j++) {
			int j_i = j * nHidden;
			p[j] = B[j];

			for (int i = 0; i < nHidden; i++) {
				p[j] += W[j_i + i] * x[i];
			}
		}

		if (p[1] < 0.0) {
			p[1] = 0.0;
		} else if (p[1] > 1.0) {
			p[1] = 1.0;
		}

		boolean onJudge;
		double next;

		if (p[0] > stateThreshold) {
			onJudge = true;
			next = p[1];
		} else {
			onJudge = false;
			next = 0.0;
		}

		for (int i = 1; i <= lstm.window; i++) {
			if (index + i < lstm.testRealSize) {
				lstm.z_train[index + i][lstm.window - i][lstm.targetIndex] = next;
			}
		}

		double inv_p = this.ds.targetDnList.get(ds.nakajiaIndexInTargets).inv(p[1]);
		double inv_t = this.ds.targetDnList.get(ds.nakajiaIndexInTargets).inv(t[1]);

		double per;
		double per2;
		double e;
		double PredictSquaredError;
		double e2;
		double PredictSquaredError2;

		all++;

		if (!z_flag) {
			off_max++;
		} else {
			on_max++;
		}

		// if (!z_flag && inv_p > 0.45) {
		if (!z_flag && onJudge) {
			off_err++;
		}

		// if (z_flag && inv_p <= 0.45) {
		if (z_flag && !onJudge) {
			on_err++;
		}

		if (!z_flag) { // 取水停止
			e = 0.0; // 取水停止なので二乗誤差0
			per = 0.0; // 取水停止なので誤差0%
		} else { // 取水中
			e = inv_p - inv_t; // inv_pはそのままのため
			per = (inv_p - inv_t) * 100.0 / inv_t; // inv_pはそのままのため
		}

		if (!z_flag && !onJudge) {
			inv_p = 0.0;
			e2 = 0.0; // inv_t=0,inv_p=0のため二乗誤差0
			per2 = 0.0; // inv_t=0,inv_p=0のため誤差0%
		} else if (!z_flag && onJudge) {
			e2 = inv_p; // inv_t=0のため
			per2 = 100.0; // inv_t=0,inv_p>0のため誤差100%
		} else if (z_flag && !onJudge) {
			inv_p = 0.0;
			e2 = -inv_t; // inv_p=0のため
			per2 = -100.0; // inv_p=0のため誤差100%
		} else { // z_flag && onJudge
			e2 = inv_p- inv_t; // inv_pはそのままのため
			per2 = (inv_p - inv_t) * 100.0 / inv_t; // inv_pはそのままのため
		}

		PredictSquaredError = e * e;
		PredictSquaredError2 = e2 * e2;

		prb.w("データ：" + index);
		prb.w(",定刻日時：" + z_coDatetime);
		prb.w(",日時：" + z_datetime);
		prb.w(",取水フラグ：" + z_flag);
		prb.w(",取水判断：" + onJudge);
		prb.w(",予測値：" + inv_p);
		prb.w(",実測値：" + inv_t);
		prb.w(",誤差：" + e);
		prb.w(",二乗誤差：" + PredictSquaredError);
		prb.w(",補正誤差：" + e2);
		prb.w(",補正二乗誤差：" + PredictSquaredError2);
		prb.w(",%誤差：" + per);
		prb.w(",補正%誤差：" + per2);
		prb.wl("");

		if (index >= lstm.window) {
			if (z_flag) {
				mse += PredictSquaredError;
				ave_per += Math.abs(per);
			}

			mse2 += PredictSquaredError2;
			ave2_per += Math.abs(per2);
		}
	}

}
