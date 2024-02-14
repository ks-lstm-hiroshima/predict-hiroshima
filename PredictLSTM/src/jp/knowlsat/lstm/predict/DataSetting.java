package jp.knowlsat.lstm.predict;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class DataSetting {
	public int inputSize;
	public int outputSize;
	public int dataTypeSize;
	public int targetDataTypeSize;
	public int windowSize;
	public int nakajiaIndexInTargets;
	public int[] targetIndexes;
	public ArrayList<NormalNormalize> targetDnList;
	public int allDataSize;
	public int allDataSize_window;
	public double[][][] allDataW;
	public double[][][] allDataWT;
	public String[][] datetimesWT;
	public String[][] coDatetimesWT;

	public double[][] data;

	public DataSetting(int inputSize, int outputSize, int window, int test_mode, double KSPP, int ammonia_mode)
			throws IOException {

		this.inputSize = inputSize;
		this.outputSize = outputSize;
		this.windowSize = window;

		// minutes difference column
		int minDiffCol = switch (test_mode) {
			case -1 -> 5;
			case 0 -> 6;
			case 30 -> 7;
			default -> {
			throw new IllegalArgumentException("不正な値：test_mode = " + test_mode);
			}
		};
		List<Integer> colIndexes;

		if (ammonia_mode == 1) {
			colIndexes = List.of(4, minDiffCol, 8, 10, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
		} else {
			colIndexes = List.of(4, minDiffCol, 8, 10, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26);
		}

		List<Integer> targetColIndexes = List.of(10, 15);
		int nakajiaColIndex = 15;
		List<Integer> timeColIndexes = List.of(25, 26);
		int datetimeColIndex = 0;
		int coDatetimeColIndex = 1;

		this.nakajiaIndexInTargets = targetColIndexes.indexOf(nakajiaColIndex);

		// mode が -1 なら すべての入力データを読み込む。
		// mode が　0 なら　カラム2のフラグが"0" のレコードのみ読み込む
		// mode が　30 なら　カラム2のフラグが"30" のレコードのみ読み込む
		// 上記の設定を colToFlagValsList に設定して DataTimeSeries クラスの dp.get()に引数として渡して 読み込みレコードを制御する。
		List<ColToFlagVals> colToFlagValsList = switch (test_mode) {
			case -1 -> List.of();
			case 0 -> List.of(new ColToFlagVals(2, List.of("0")));
			case 30 -> List.of(new ColToFlagVals(2, List.of("30")));
			default -> {
				throw new IllegalArgumentException("不正な値：test_mode = " + test_mode);
			}
		};

		List<ChangeParamValByFlag> changeParamValByFlagList = List.of(
				// index 9 が "0" なら　index 15 を KSPP に変更する、ただし KSPP が 0.0 以上の時だけ。
				new ChangeParamValByFlag(9, "0", 15, KSPP, x -> x >= 0.0) // KSPP値をホワイトノイズ（KS秘伝の味）として付与
				);

		DataTimeSeries dp = new DataTimeSeries(
				"data/MIN30_ver2.1.0__20180301_0000__20230903_2330.csv",
				colIndexes, targetColIndexes, timeColIndexes, colToFlagValsList, changeParamValByFlagList, datetimeColIndex, coDatetimeColIndex);

		ArrayList<ArrayList<Double>> dataList = dp.getData();
		String[] datetimes = dp.getDatetimes();
		String[] coDatetimes = dp.getCoDatetimes();

		this.dataTypeSize = colIndexes.size();
		this.targetDataTypeSize = targetColIndexes.size();

		this.allDataSize = dataList.get(0).size();
		this.allDataSize_window = this.allDataSize - this.windowSize + 1;
		List<Integer> targetArrayIndexes = dp.getTargetArrayIndexes();
		List<Integer> timeArrayIndexes = dp.getTimeArrayIndexes();

		int numOfParam = dataList.size();

		this.data = new double[numOfParam][];

		this.targetDnList = new ArrayList<>( targetArrayIndexes.size() );		

		for (int i = 0; i < numOfParam; i++) {
			DataNormalize dn;
			NormalNormalize nn;

			if (timeArrayIndexes.contains(Integer.valueOf(i))) {
				dn = new CoslNormalize(dataList.get(i));
			} else if (!targetArrayIndexes.contains(Integer.valueOf(i))) {
				dn = new NormalNormalize(dataList.get(i));
			} else {
				nn = new NormalNormalize(dataList.get(i));
				this.targetDnList.add(nn);
				dn = (DataNormalize) nn;
			}
			this.data[i] = dn.get();
		}

		this.targetIndexes = targetArrayIndexes.stream().mapToInt(Integer::intValue).toArray();

		this.allDataW = new double[this.allDataSize_window - 1][this.windowSize][this.dataTypeSize];
		this.allDataWT = new double[this.allDataSize_window - 1][this.windowSize][this.targetDataTypeSize];
		this.datetimesWT = new String[this.allDataSize_window - 1][this.windowSize];
		this.coDatetimesWT = new String[this.allDataSize_window - 1][this.windowSize];

		for (int iParam = 0; iParam < this.dataTypeSize; iParam++) {
			for (int iTime = 0; iTime < this.allDataSize_window - 1; iTime++) {
				for (int w = 0; w < this.windowSize; w++) {
					this.allDataW[iTime][w][iParam] = this.data[iParam][iTime + w];
				}
			}
		}

		int index = 0;

		for (int iParam = 0; iParam < this.dataTypeSize; iParam++) {
			if (!targetArrayIndexes.contains(Integer.valueOf(iParam))) {
				continue;
			}

			for (int iTime = 1; iTime < this.allDataSize_window; iTime++) {
				for (int w = 0; w < this.windowSize; w++) {
					this.allDataWT[iTime - 1][w][index] = this.data[iParam][iTime + w];
				}
			}

			index++;
		}

		for (int iTime = 1; iTime < this.allDataSize_window; iTime++) {
			for (int w = 0; w < this.windowSize; w++) {
				this.datetimesWT[iTime - 1][w] = datetimes[iTime + w];
				this.coDatetimesWT[iTime - 1][w] = coDatetimes[iTime + w];
			}
		}

	}

	public int getDataTypeSize() {
		return this.dataTypeSize;
	}
}

// colIndex の値が、Collection flagValsに設定した値（ホワイトリスト）に含まれている行のみレコードを読み込む設定をするクラス。
// 複数オブジェクトで複数カラムに設定した場合は、すべてのColToFlagValsインスタンスで読み込む行と判断されたレコードしか読み込まれない。
class ColToFlagVals {
	private int colIndex;
	private HashSet<String> flagVals;

	ColToFlagVals(int colIndex, Collection<String> flagVals) {
		this.colIndex = colIndex;
		this.flagVals = new HashSet<String>(flagVals);
	}

	public boolean isNotContain(String[] row) {
		return !flagVals.contains(row[colIndex]);
	}
}

// KSPPのように、特定のカラムのフラグを元に、別のカラムの値を変更する処理を抽象化する機能です。
// DataTimeSeries.javaクラスにハードコーディングするのではなく、index指定を行っているこのクラスで設定変更を完結させる目的もあります。
// 副作用としてプログラム起動時の前処理の速度は落ちると思います。
class ChangeParamValByFlag {
	private int flagColIndex;
	private String changeFlagVal;
	private int targetColIndex;
	private double changeVal;
	private boolean execChange;

	ChangeParamValByFlag(int flagColIndex, String changeFlagVal, int targetColIndex, double changeVal,
			JugeChange jugeChange) {
		this.flagColIndex = flagColIndex;
		this.changeFlagVal = changeFlagVal;
		this.targetColIndex = targetColIndex;
		this.changeVal = changeVal;
		this.execChange = jugeChange.isChange(changeVal);
	}

	public int getFlagIndex() {
		return this.flagColIndex;
	}

	public String getChangeFlagVal() {
		return this.changeFlagVal;
	}

	public int getTargetIndex() {
		return this.targetColIndex;
	}

	public double getChangeVal() {
		return this.changeVal;
	}

	public boolean execChange() {
		return this.execChange;
	}

}

@FunctionalInterface
interface JugeChange {
	boolean isChange(double value);
}
