package jp.knowlsat.lstm.predict;

public class LSTM_OutputLayer {
	public int nHidden;
	public int nOut;
	public double[] B;
	public double[] W;
	public LSTM lstm;
	private DataSetting ds;

	public LSTM_OutputLayer(int nHidden, int nOut, LSTM lstm, DataSetting ds) {
		this.nHidden = nHidden;
		this.nOut = nOut;

		this.lstm = lstm;		
		this.ds = ds;

		W = new double[nOut * nHidden];
		B = new double[nOut];
	}

	public LSTM_Output test(double[] x, double[] t, boolean z_flag, double stateThreshold, String z_datetime, String z_coDatetime,
		boolean incident, String time_mode, double[][] input) {
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

		double inv_p = this.ds.targetDnList.get(ds.nakajiaIndexInTargets).inv(p[1]);
		double inv_t = this.ds.targetDnList.get(ds.nakajiaIndexInTargets).inv(t[1]);

		double per;
		double per2;
		double e;
		double PredictSquaredError;
		double e2;
		double PredictSquaredError2;

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

		LSTM_Output output = new LSTM_Output(z_coDatetime, z_datetime, onJudge, z_flag, inv_p, inv_t,
        	e, PredictSquaredError, per, e2, PredictSquaredError2, per2,
			next, incident, time_mode, input[input.length - 1]);

		return output;
	}

}
