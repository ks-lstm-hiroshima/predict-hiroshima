package jp.knowlsat.lstm.predict;

public class LSTM_HiddenLayer {
	public int nIn;
	public int nHidden;

	public double[] BF;
	public double[] BI;
	public double[] BG;
	public double[] BO;

	public double[] Ftm;
	public double[] Itm;
	public double[] Gtm;
	public double[] Otm;
	public double[] Ft;
	public double[] It;
	public double[] Gt;
	public double[] Ot;
	public double[] C;
	public double[] Y;

	public double[] A;
	public double[] B;
	public double[] A2;
	public double[] B2;
	public double[] Wif;
	public double[] Wii;
	public double[] Wig;
	public double[] Wio;
	public double[] Whf;
	public double[] Whi;
	public double[] Whg;
	public double[] Who;
	public double[] Wcf;
	public double[] Wci;
	public double[] Wco;

	public int peephole_mode;
	public int elu_mode;

	public LSTM_HiddenLayer(int nIn, int nHidden, int peephole_mode, int elu_mode) {
		this.nIn = nIn;
		this.nHidden = nHidden;

		this.peephole_mode = peephole_mode;
		this.elu_mode = elu_mode;

		BF = new double[nHidden];
		BI = new double[nHidden];
		BG = new double[nHidden];
		BO = new double[nHidden];

		Ftm = new double[nHidden];
		Itm = new double[nHidden];
		Gtm = new double[nHidden];
		Otm = new double[nHidden];
		Ft = new double[nHidden];
		It = new double[nHidden];
		Gt = new double[nHidden];
		Ot = new double[nHidden];
		C = new double[nHidden];
		Y = new double[nHidden];

		if (elu_mode == 1) {
			A = new double[nHidden];
			B = new double[nHidden];
			A2 = new double[nHidden];
			B2 = new double[nHidden];
		}

		Wif = new double[nIn * nHidden];
		Wii = new double[nIn * nHidden];
		Wig = new double[nIn * nHidden];
		Wio = new double[nIn * nHidden];
		Whf = new double[nHidden * nHidden];
		Whi = new double[nHidden * nHidden];
		Whg = new double[nHidden * nHidden];
		Who = new double[nHidden * nHidden];

		if (peephole_mode == 1) {
			Wcf = new double[nHidden];
			Wci = new double[nHidden];
			Wco = new double[nHidden];
		}
	}

	public void forwardTest(double[] x, double[] y_prev, double[] c_prev) {
		for (int j = 0; j < nHidden; j++) {
			Ftm[j] = BF[j];
			Itm[j] = BI[j];
			Gtm[j] = BG[j];
			Otm[j] = BO[j];
		}

		for (int i = 0; i < nIn; i++) {
			int i_j = i * nHidden;

			for (int j = 0; j < nHidden; j++) {
				int ij = i_j + j;

				Ftm[j] += x[i] * Wif[ij];
				Itm[j] += x[i] * Wii[ij];
				Gtm[j] += x[i] * Wig[ij];
				Otm[j] += x[i] * Wio[ij];
			}
		}

		for (int i = 0; i < nHidden; i++) {
			int i_j = i * nHidden;

			for (int j = 0; j < nHidden; j++) {
				int ij = i_j + j;

				Ftm[j] += y_prev[i] * Whf[ij];
				Itm[j] += y_prev[i] * Whi[ij];
				Gtm[j] += y_prev[i] * Whg[ij];
				Otm[j] += y_prev[i] * Who[ij];
			}
		}

		if (peephole_mode == 1) {
			for (int j = 0; j < nHidden; j++) {
				Ftm[j] += c_prev[j] * Wcf[j]; // peephole LSTM
				Itm[j] += c_prev[j] * Wci[j]; // peephole LSTM
			}
		}

		for (int j = 0; j < nHidden; j++) {
			Gt[j] = Math.tanh(Gtm[j]);
			Ft[j] = 1.0 / (1.0 + Math.exp(-Ftm[j]));
			It[j] = 1.0 / (1.0 + Math.exp(-Itm[j]));
			C[j] = Ft[j] * c_prev[j] + It[j] * Gt[j];
		}

		if (peephole_mode == 1) {
			for (int j = 0; j < nHidden; j++) {
				Otm[j] += C[j] * Wco[j]; // peephole LSTM
			}
		}

		for (int j = 0; j < nHidden; j++) {
			Ot[j] = 1.0 / (1.0 + Math.exp(-Otm[j]));

			if (elu_mode == 0) {
				Y[j] = Ot[j] * Math.tanh(C[j]);
			} else if (elu_mode == 1) {
				if (C[j] >= B[j]) {
					double div_a = 1.0 / A[j];

					Y[j] = Ot[j] * (Math.pow(C[j] + 1.0 - B[j], A[j]) * div_a - (div_a - B[j]));
				} else if (C[j] >= 0.0){
					Y[j] = Ot[j] * C[j];
				} else {
					Y[j] = Ot[j] * (Math.exp(C[j]) - 1.0);
				}
			}
		}
	}

}
