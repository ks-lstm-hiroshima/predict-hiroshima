package jp.knowlsat.lstm;

import java.util.ArrayList;

public class DataMinibatch {
	public DataSetting ds;
	public int TrainSize;
	public int ValidationSize;
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

	public DataMinibatch(DataSetting ds, int TrainSize, int ValidationSize, int TestSize,
			int WindowSize, int DataType, int target_index, int nOut) {
		this.ds = ds;
		this.TrainSize = TrainSize;
		this.ValidationSize = ValidationSize;
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
					z_train[i][w][j] = ds.allDataW[ds.allDataSize_window - 1 - TestSize + i][w][j];
				}
			}
		}

		for (int i = 0; i < WindowSize; i++) {
			for (int w = 0; w < WindowSize - i; w++) {
				z_train[i][w][target_index] = ds.allDataW[ds.allDataSize_window - 1 - TestSize + i][w][target_index];
			}
		}

		for (int i = 0; i < TestSize; i++) {
			for (int w = 0; w < WindowSize; w++) {
				z_target[i][w][0] = ds.allDataWT[ds.allDataSize_window - 1 - TestSize + i][w][0];
				z_target[i][w][1] = ds.allDataWT[ds.allDataSize_window - 1 - TestSize + i][w][1];
				z_datetimes[i][w] = ds.datetimesWT[ds.allDataSize_window - 1 - TestSize + i][w];
				z_coDatetimes[i][w] = ds.coDatetimesWT[ds.allDataSize_window - 1 - TestSize + i][w];
			}
		}

		for (int i = 0; i < TestSize; i++) {
			for (int w = 0; w < WindowSize; w++) {
				if (ds.allDataWT[ds.allDataSize_window - 1 - TestSize + i][w][0] == 0.0) {
					z_flag[i][w] = false;
				} else {
					z_flag[i][w] = true;
				}
			}
		}
	}

}
