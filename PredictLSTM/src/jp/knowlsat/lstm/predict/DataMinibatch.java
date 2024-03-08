package jp.knowlsat.lstm.predict;

import java.util.ArrayList;

public class DataMinibatch {
	public DataSetting ds;
	public int TestSize;
	public int WindowSize;
	public int DataType;
	public int nOut;
	public ArrayList<Integer> ShuffleIndex;

	public double[][][] z_train;
	public double[][][] z_target;
	public boolean[][] z_flag;
	public String[][] z_datetimes;
	public String[][] z_coDatetimes;

	public DataMinibatch(DataSetting ds, int TestSize, int WindowSize, int DataType, int target_index, int nOut, int test_index) {
		this.ds = ds;
		this.TestSize = TestSize;
		this.WindowSize = WindowSize;
		this.DataType = DataType;
		this.nOut = nOut;

		z_train = new double[TestSize][WindowSize][DataType];
		z_target = new double[TestSize][WindowSize][nOut];
		z_flag = new boolean[TestSize][WindowSize];
		z_datetimes = new String[TestSize][WindowSize];
		z_coDatetimes = new String[TestSize][WindowSize];

		for (int i = 0; i < TestSize; i++) {
			for (int j = 0; j < DataType; j++) {
				if (j == target_index) {
					continue;
				}

				for (int w = 0; w < WindowSize; w++) {
					z_train[i][w][j] = ds.predictDataW[ds.predictDataSize_window - 1 - test_index + i][w][j];
				}
			}
		}

		for (int i = 0; i < TestSize; i++) {
			for (int w = 0; w < WindowSize - i; w++) {
				z_train[i][w][target_index] = ds.predictDataW[ds.predictDataSize_window - 1 - test_index + i][w][target_index];
			}
		}

		for (int i = 0; i < TestSize; i++) {
			for (int w = 0; w < WindowSize; w++) {
				z_target[i][w][0] = ds.predictDataWT[ds.predictDataSize_window - 1 - test_index + i][w][0];
				z_target[i][w][1] = ds.predictDataWT[ds.predictDataSize_window - 1 - test_index + i][w][1];
				z_datetimes[i][w] = ds.datetimesWT[ds.predictDataSize_window - 1 - test_index + i][w];
				z_coDatetimes[i][w] = ds.coDatetimesWT[ds.predictDataSize_window - 1 - test_index + i][w];
			}
		}

		for (int i = 0; i < TestSize; i++) {
			for (int w = 0; w < WindowSize; w++) {
				if (ds.predictDataWT[ds.allDataSize_window - 1 - test_index + i][w][0] == 0.0) {
					z_flag[i][w] = false;
				} else {
					z_flag[i][w] = true;
				}
			}
		}
	}

}
