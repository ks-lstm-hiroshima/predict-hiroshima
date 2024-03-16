package jp.knowlsat.lstm.predict;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

	public int predictDataSize;
	public int predictDataSize_window;
	public double[][][] predictOriginDataW;
	public double[][][] predictOriginDataWT;
	public double[][][] predictDataW;
	public double[][][] predictDataWT;
	public String[][] predictDatetimesWT;
	public String[][] predictCoDatetimesWT;

	public double[][] data;
	public HashMap<Integer, DataNormalize> colDnMap;

	public DataSetting(int inputSize, int outputSize, int window, int test_mode, double KSPP, int ammonia_mode,
			int dataNumForTest, ArrayList<String[]> rTimeRecs, int passed)
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
				colIndexes, targetColIndexes, timeColIndexes, colToFlagValsList, changeParamValByFlagList,
				datetimeColIndex, coDatetimeColIndex);

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

		this.targetDnList = new ArrayList<>(targetArrayIndexes.size());
		colDnMap = new HashMap<>(colIndexes.size() + 1, 1.0f); // ArrayListの方が良い。

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
			colDnMap.put(Integer.valueOf(i), dn);
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

		RealDataSetting rds = new RealDataSetting(rTimeRecs, test_mode, colIndexes, timeColIndexes, datetimeColIndex,
				targetColIndexes, colDnMap, this.windowSize, dataNumForTest, passed);

		this.predictDataSize = rds.getPredictDataSize();
		this.predictDataSize_window = rds.getPredictDataSize_window();
		this.predictDataW = rds.getPredictDataW();
		this.predictDataWT = rds.getPredictDataWT();
		this.predictDatetimesWT = rds.getPredictDatetimesWT();
		this.predictCoDatetimesWT = rds.getPredictCoDatetimesWT();

		this.predictOriginDataW = rds.getPredictOriginDataW();
		this.predictOriginDataWT = rds.getPredictOriginDataWT();
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

class RealDataSetting {
	private int predictDataSize;
	private int predictDataSize_window;
	private double[][][] predictDataW;
	private double[][][] predictDataWT;
	private double[][][] predictOriginDataW;
	private double[][][] predictOriginDataWT;
	private String[][] predictDatetimesWT;
	private String[][] predictCoDatetimesWT;
	public int dataNumForTest;

	private List<Integer> colIndexes;
	private HashMap<Integer, DataNormalize> colDnMap;
	private final HashMap<Integer, Integer> learningDataColToCol;
	{
		learningDataColToCol = new HashMap<>();
		learningDataColToCol.put(Integer.valueOf(0), Integer.valueOf(0));
		learningDataColToCol.put(Integer.valueOf(8), Integer.valueOf(29));
		learningDataColToCol.put(Integer.valueOf(11), Integer.valueOf(59));
		learningDataColToCol.put(Integer.valueOf(12), Integer.valueOf(59));
		learningDataColToCol.put(Integer.valueOf(13), Integer.valueOf(59));
		learningDataColToCol.put(Integer.valueOf(14), Integer.valueOf(78));
		learningDataColToCol.put(Integer.valueOf(15), Integer.valueOf(84));
		learningDataColToCol.put(Integer.valueOf(16), Integer.valueOf(91));
		learningDataColToCol.put(Integer.valueOf(17), Integer.valueOf(96));
		learningDataColToCol.put(Integer.valueOf(18), Integer.valueOf(97));
		learningDataColToCol.put(Integer.valueOf(19), Integer.valueOf(98));
		learningDataColToCol.put(Integer.valueOf(20), Integer.valueOf(99));
		learningDataColToCol.put(Integer.valueOf(21), Integer.valueOf(101));
		learningDataColToCol.put(Integer.valueOf(22), Integer.valueOf(102));
		learningDataColToCol.put(Integer.valueOf(23), Integer.valueOf(103));
		learningDataColToCol.put(Integer.valueOf(24), Integer.valueOf(134));
	}

	RealDataSetting(ArrayList<String[]> rTimeRecs, int test_mode, List<Integer> colIndexes,
			List<Integer> timeColIndexes, int datetimeColIndex, List<Integer> targetColIndexes,
			HashMap<Integer, DataNormalize> colDnMap, int windowSize, int dataNumForTest,
			int passed) {
		this.colIndexes = colIndexes;
		this.colDnMap = colDnMap;

		this.dataNumForTest = dataNumForTest;
		// this.dataNumForTest = 1;

		this.predictDataSize_window = this.dataNumForTest;
		this.predictDataSize = windowSize + this.dataNumForTest - 1;

		ArrayList<String> pDateTimeWT = new ArrayList<>(windowSize);
		ArrayList<String> pCoDateTimeWT = new ArrayList<>(windowSize);

		HashMap<Integer, ArrayList<Double>> orgColToArray = new HashMap<>();

		int dtColIndex = this.learningDataColToCol.get(Integer.valueOf(datetimeColIndex));

		LocalDateTime nowDT = LocalDateTime.now();
		nowDT = LocalDateTime.of(2023, 9, 3, 22, 45, nowDT.getSecond(), nowDT.getNano());

		if (passed != 0) {
			nowDT = nowDT.minusHours(passed);
		}

		System.out.print("起動時刻は :  ");
		System.out.println(nowDT.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
		System.out.println(nowDT.getNano() + "ナノ秒");
		System.out.println(ParseDateTime.fTime(nowDT));

		// テストモード　-1（30分間隔モデル）の時にこのコードは正常に機能しない
		System.out.print("予測時刻は :  ");
		System.out.println(ParseDateTime.getNextDT(nowDT, test_mode)
				.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));

		LocalDateTime nextDT = ParseDateTime.getNextDT(nowDT, test_mode);

		// 予測点に関して実時刻と論理時刻の差はない
		pDateTimeWT.add(ParseDateTime.fTime(nextDT));
		pCoDateTimeWT.add(ParseDateTime.fTime(nextDT));

		LocalDateTime firstCoDT = nextDT.minusHours(1L);
		LocalDateTime curCoDT = nextDT.minusHours(1L);

		int previousRecIndex = 0;
		String dtStr;
		ParseDateTime parsedDT = null;
		for (; previousRecIndex < rTimeRecs.size(); previousRecIndex++) {
			dtStr = rTimeRecs.get(previousRecIndex)[dtColIndex];
			parsedDT = new ParseDateTime(dtStr);
			if (parsedDT.le(curCoDT)) {
				System.out.println(
						"abc" + parsedDT.datetime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
				break;
			}
		}
		if (previousRecIndex >= rTimeRecs.size()) {
			System.out.println("前の定刻レコードが見つかりませんでした。");
			System.out.println("previousRecIndex = " + previousRecIndex);
			System.out.println("rTimeRecs.size() = " + rTimeRecs.size());
			System.exit(-999);
		}

		System.out.print("テストモード[" + test_mode + "] 前の予測点は :  ");
		System.out.println(curCoDT.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
		System.out.print("予測時刻以前の最新レコードは :  ");
		System.out.println(parsedDT.datetime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));

		ArrayList<RecSource> recSources = new ArrayList<>(windowSize + 1);

		// 同じ時刻のレコードの重複や、レコードの欠損があるので、レコードのインデックスとズレた分数(ふんすう)は一致しない事がある。
		for (int wIndex = 0; wIndex <= windowSize; wIndex++) {
			for (int i = previousRecIndex; i < rTimeRecs.size(); i++) {
				String[] curOrgRec = rTimeRecs.get(i);
				parsedDT = new ParseDateTime(curOrgRec[dtColIndex]);
				if (parsedDT.gt(curCoDT)) {
					System.out.print("b");
					continue;
				}
				if (this.isNormalRec(curOrgRec)) {
					System.out.println("aaa");
					recSources.add(new RecSource(parsedDT, curOrgRec, ParseDateTime.fTime(curCoDT), i));
					previousRecIndex = i;
					curCoDT = curCoDT.minusHours(1);
					break;
				}
			}
		}

		// 過去の実時刻と論理時刻を文字列で保持
		for (int i = 0; i < windowSize - 1; i++) {
			pDateTimeWT.add(ParseDateTime.fTime(recSources.get(i).pdt.datetime));
			pCoDateTimeWT.add(recSources.get(i).coDT);
		}

		Collections.reverse(pDateTimeWT);
		Collections.reverse(pCoDateTimeWT);

		this.predictDatetimesWT = new String[this.dataNumForTest][windowSize];
		this.predictCoDatetimesWT = new String[this.dataNumForTest][windowSize];

		for (int iDataNum = 0; iDataNum < this.dataNumForTest; iDataNum++) {
			for (int w = 0; w < windowSize; w++) {
				this.predictDatetimesWT[iDataNum][w] = pDateTimeWT.get(iDataNum + w);
				this.predictCoDatetimesWT[iDataNum][w] = pCoDateTimeWT.get(iDataNum + w);
			}
		}

		// 4:REC_LAGがパラメータに採用されていれば
		// 最初（時系列的に最後）のREC_LAGは時間で割り出して、それ以降（時系列的に前）はレコードの欠損・重複はない前提で計算
		int[] recLagIndexes = new int[windowSize];
		ArrayList<Double> recLags = new ArrayList<>(windowSize);
		int firstRecLagIndex = 0;
		if (colIndexes.contains(4)) {
			recLags = new ArrayList<Double>(windowSize);
			firstRecLagIndex = (int) firstCoDT.until(recSources.get(0).pdt.datetime, ChronoUnit.MINUTES);
			recLagIndexes[0] = firstRecLagIndex;
			recLags.add(Double.valueOf(firstRecLagIndex / 5.0D));
			for (int i = 1; i < windowSize; i++) {
				recLagIndexes[i] = i * 60 + firstRecLagIndex
						- (recSources.get(i).recIndex - recSources.get(0).recIndex);
				recLags.add(Double.valueOf(recLagIndexes[i] / 5.0D));
			}
		}
		Collections.reverse(recLags);
		orgColToArray.put(Integer.valueOf(4), recLags);

		// 6:MIN_DIFF_60_00, 7:MIN_DIFF_60_30 がパラメータに採用されていれば。　// 5:MIN_DIFF_30の採用は想定外
		ArrayList<Double> minDiffs = new ArrayList<>(windowSize);
		if (colIndexes.contains(6) || colIndexes.contains(7)) {
			for (int i = 0; i < windowSize; i++) {
				minDiffs.add(
						Double.valueOf(
								(double) recSources.get(i + 1).pdt.datetime.until(recSources.get(i).pdt.datetime,
										ChronoUnit.MINUTES)));
			}
		}
		Collections.reverse(minDiffs);
		Integer minDiffsCol = null;
		if (colIndexes.contains(6)) {
			minDiffsCol = Integer.valueOf(6);
		} else if (colIndexes.contains(7)) {
			minDiffsCol = Integer.valueOf(7);
		} else if (colIndexes.contains(5)) {
			throw new AssertionError("パラメータ5番 MIN_DIFF_30 の指定は未対応です", null);
		}
		orgColToArray.put(minDiffsCol, minDiffs);

		// 10:FI6001_STATE がパラメータに採用されていれば。
		ArrayList<Double> FI6001_STATE_aList = new ArrayList<>(windowSize);
		if (colIndexes.contains(10)) {
			int FI6001_OrgCol = this.learningDataColToCol.get(8);
			for (int i = 0; i < windowSize; i++) {
				int rowIndex = recSources.get(i).recIndex;
				double val = Double.parseDouble(rTimeRecs.get(rowIndex)[FI6001_OrgCol]);
				FI6001_STATE_aList.add(
						val >= 100.0D ? 1.0 : 0.0);
			}
		}
		Collections.reverse(FI6001_STATE_aList);
		orgColToArray.put(Integer.valueOf(10), FI6001_STATE_aList);

		// 25:DATE_RADIAN がパラメータに採用されていれば。
		ArrayList<Double> DATE_RADIAN_aList = new ArrayList<>(windowSize);
		if (colIndexes.contains(25)) {
			for (int i = 0; i < windowSize; i++) {
				DATE_RADIAN_aList.add(CalcRadian.getDateRadian(recSources.get(i).pdt.datetime));
			}
		}
		Collections.reverse(DATE_RADIAN_aList);
		orgColToArray.put(Integer.valueOf(25), DATE_RADIAN_aList);

		// 26:TIME_RADIAN がパラメータに採用されていれば。
		ArrayList<Double> TIME_RADIAN_aList = new ArrayList<>(windowSize);
		if (colIndexes.contains(26)) {
			for (int i = 0; i < windowSize; i++) {
				TIME_RADIAN_aList.add(CalcRadian.getTimeRadian(recSources.get(i).pdt.datetime));
			}
		}
		Collections.reverse(TIME_RADIAN_aList);
		orgColToArray.put(Integer.valueOf(26), TIME_RADIAN_aList);

		// 27:AMMONIUM1001 がパラメータに採用されていれば。
		Double ammo_fixed = Double.valueOf(0.01); // 正規化前の値（学習データ MIN30_ver2.1.0__20180301_0000__20230903_2330.csv での下限値）
		ArrayList<Double> AMMONIUM1001_aList = new ArrayList<>(windowSize);
		if (colIndexes.contains(27)) {
			for (int i = 0; i < windowSize; i++) {
				AMMONIUM1001_aList.add(ammo_fixed);
			}
		}
		Collections.reverse(AMMONIUM1001_aList); // 固定値の場合は不要な処理
		orgColToArray.put(Integer.valueOf(27), AMMONIUM1001_aList);

		this.predictOriginDataW = new double[this.dataNumForTest][windowSize][colIndexes.size()];
		this.predictDataW = new double[this.dataNumForTest][windowSize][colIndexes.size()];

		HashMap<Integer, DataNormalize> targetColDnMap = new HashMap<>(targetColIndexes.size() + 1, 1.0f); // ArrayListの方が良い。

		Collections.reverse(recSources); // この操作を、実時刻と論理時刻保持の操作の前に移動させない事
		for (int iCol = 0; iCol < colIndexes.size(); iCol++) {
			Integer col = colIndexes.get(iCol);
			if (targetColIndexes.contains(col))
				targetColDnMap.put(Integer.valueOf(targetColIndexes.indexOf(col)), colDnMap.get(iCol));
			if (this.learningDataColToCol.containsKey(col)) {
				for (int iDataNum = 0; iDataNum < this.dataNumForTest; iDataNum++) {
					for (int i = 1; i <= windowSize; i++) {
						this.predictOriginDataW[iDataNum][i - 1][iCol] = Double
								.parseDouble(recSources.get(i).orgRec[(int) this.learningDataColToCol.get(col)]);
						this.predictDataW[iDataNum][i - 1][iCol] = colDnMap.get(iCol)
								.normalize(this.predictOriginDataW[iDataNum][i - 1][iCol]); // ボクシング：get(iCol)
					}
				}
			} else if (orgColToArray.containsKey(col)) {
				for (int iDataNum = 0; iDataNum < this.dataNumForTest; iDataNum++) {
					for (int i = 0; i < windowSize; i++) {
						this.predictOriginDataW[iDataNum][i][iCol] = orgColToArray.get(col).get(i);
						this.predictDataW[iDataNum][i][iCol] = colDnMap.get(iCol)
								.normalize(this.predictOriginDataW[iDataNum][i][iCol]); // ボクシング：get(iCol)
					}
				}
			} else {
				throw new AssertionError(col.toString() + "番はRealDataSettingクラスでサポートされていないカラム数です。");
			}
		}

		this.predictOriginDataWT = new double[this.dataNumForTest][windowSize][targetColIndexes.size()];
		this.predictDataWT = new double[this.dataNumForTest][windowSize][targetColIndexes.size()];

		for (int iCol = 0; iCol < targetColIndexes.size(); iCol++) {
			Integer col = targetColIndexes.get(iCol);
			if (this.learningDataColToCol.containsKey(col)) {
				for (int iDataNum = 0; iDataNum < this.dataNumForTest; iDataNum++) {
					for (int i = 2; i <= windowSize; i++) {
						this.predictOriginDataWT[iDataNum][i - 2][iCol] = Double
								.parseDouble(recSources.get(i).orgRec[(int) this.learningDataColToCol.get(col)]);
						this.predictDataWT[iDataNum][i - 2][iCol] = targetColDnMap.get(iCol)
								.normalize(this.predictOriginDataWT[iDataNum][i - 2][iCol]); // ボクシング：get(iCol)
					}
					this.predictOriginDataWT[iDataNum][windowSize - 1][iCol] = 0.0; // 未来の値なので意味のない値0.0で埋める。
					this.predictDataWT[iDataNum][windowSize - 1][iCol] = 0.0; // 未来の点に関して、正規化や逆変換が出来る関係でない事に注意
				}
			} else if (orgColToArray.containsKey(col)) {
				for (int iDataNum = 0; iDataNum < this.dataNumForTest; iDataNum++) {
					for (int i = 1; i < windowSize; i++) {
						this.predictOriginDataWT[iDataNum][i - 1][iCol] = orgColToArray.get(col).get(i);
						this.predictDataWT[iDataNum][i - 1][iCol] = targetColDnMap.get(iCol)
								.normalize(this.predictOriginDataWT[iDataNum][i - 1][iCol]); // ボクシング：get(iCol)
					}
					this.predictOriginDataWT[iDataNum][windowSize - 1][iCol] = 0.0; // 未来の値なので意味のない値0.0で埋める。
					this.predictDataWT[iDataNum][windowSize - 1][iCol] = 0.0; // 未来の点に関して、正規化や逆変換が出来る関係でない事に注意
				}
			} else {
				throw new AssertionError(col.toString() + "番はRealDataSettingクラスでサポートされていないカラム数です。");
			}
		}

		//↓↓↓↓　 簡易テストコード　↓↓↓↓

		for (int i = 0; i < windowSize; i++) {
			System.out.printf("%.2f：%.2f  ", recLags.get(i), minDiffs.get(i));
		}
		System.out.println();
		for (int i = 0; i <= windowSize; i++) {
			System.out.printf(" %s%5d", recSources.get(i).coDT, recSources.get(i).recIndex);
		}
		System.out.println();
		for (int i = 0; i < windowSize; i++) {
			int FI6001_OrgCol = this.learningDataColToCol.get(8);
			System.out.printf(" %.2f：%.2f ", Double.parseDouble(recSources.get(i + 1).orgRec[FI6001_OrgCol]),
					FI6001_STATE_aList.get(i));
		}
		System.out.println();
		for (int i = 0; i < windowSize; i++) {
			System.out.printf(" %s %.6f %.6f\n", ParseDateTime.fTime(recSources.get(i + 1).pdt.datetime),
					DATE_RADIAN_aList.get(i), TIME_RADIAN_aList.get(i));
		}
		System.out.println("dataNumForTest = " + this.dataNumForTest);
		for (int iDataNum = 0; iDataNum < this.dataNumForTest; iDataNum++) {
			for (int w = 0; w < windowSize; w++) {
				System.out.print(this.predictDatetimesWT[iDataNum][w] + "  ");
				System.out.println(this.predictCoDatetimesWT[iDataNum][w]);
			}
		}
		for (int i = 0; i < windowSize; i++) {
			double w_FI6001_STATE = this.predictOriginDataW[0][i][3];
			double w_PLC31_PAR_AI0083 = this.predictOriginDataW[0][i][6];
			double wt_PLC31_PAR_AI0083 = this.predictOriginDataWT[0][i][1];
			System.out.print(w_FI6001_STATE + "  ");
			System.out.print(w_PLC31_PAR_AI0083 + "  ");
			System.out.println(wt_PLC31_PAR_AI0083);
		}
		System.out.println("↓↓↓ 正規化後の値 ↓↓↓");
		for (int i = 0; i < windowSize; i++) {
			double w_FI6001_STATE = this.predictDataW[0][i][3];
			double w_PLC31_PAR_AI0083 = this.predictDataW[0][i][6];
			double wt_PLC31_PAR_AI0083 = this.predictDataWT[0][i][1];
			System.out.print(w_FI6001_STATE + "  ");
			System.out.print(w_PLC31_PAR_AI0083 + "  ");
			System.out.println(wt_PLC31_PAR_AI0083);
		}
		// ↑↑↑↑  簡易テストコード  ↑↑↑↑
	}

	private boolean isNormalRec(String[] orgRec) {
		for (Integer col : this.colIndexes) {
			if (this.learningDataColToCol.containsKey(col)) {
				NormalNormalize nn = (NormalNormalize) this.colDnMap.get(colIndexes.indexOf(col));
				if (!nn.isNormalRange(Double.parseDouble(orgRec[(int) this.learningDataColToCol.get(col)]))) {
					return false;
				}
			}
		}
		return true;
	}

	int getPredictDataSize() {
		return this.predictDataSize;
	}

	int getPredictDataSize_window() {
		return this.predictDataSize_window;
	}

	double[][][] getPredictOriginDataW() {
		return this.predictOriginDataW;
	}

	double[][][] getPredictOriginDataWT() {
		return this.predictOriginDataWT;
	}

	double[][][] getPredictDataW() {
		return this.predictDataW;
	}

	double[][][] getPredictDataWT() {
		return this.predictDataWT;
	}

	String[][] getPredictDatetimesWT() {
		return this.predictDatetimesWT;
	}

	String[][] getPredictCoDatetimesWT() {
		return this.predictCoDatetimesWT;
	}

}

class ParseDateTime {
	int year;
	int month;
	int day;
	int hour;
	int minutes;
	int second;
	String sYear;
	String sMonth;
	String sDay;
	String sHour;
	String sMinutes;
	String sSecond;
	LocalDateTime datetime;

	ParseDateTime(String datetime) {
		String[] datetimeParts = this.sepDT(datetime);
		String date = datetimeParts[0];
		String time = datetimeParts[1];

		String[] dateParts = this.sepD01(date);
		this.sYear = dateParts[0];
		this.sMonth = dateParts[1];
		this.sDay = dateParts[2];

		String[] timeParts = this.sepT(time);
		this.sHour = timeParts[0];
		this.sMinutes = timeParts[1];
		this.sSecond = timeParts[2];

		this.year = Integer.parseInt(this.sYear);
		this.month = Integer.parseInt(this.sMonth);
		this.day = Integer.parseInt(this.sDay);
		this.hour = Integer.parseInt(this.sHour);
		this.minutes = Integer.parseInt(this.sMinutes);
		this.second = Integer.parseInt(this.sSecond);

		this.datetime = LocalDateTime.of(this.year, this.month, this.day, this.hour, this.minutes, this.second);
	}

	private String[] sepDT(String datetime) {
		String[] datetimeParts = datetime.split(" ");
		return datetimeParts;
	}

	private String[] sepD01(String date) {
		String[] dateParts = date.split("-");
		return dateParts;
	}

	@SuppressWarnings("unused")
	private String[] sepD02(String date) {
		String[] dateParts = date.split("/");
		return dateParts;
	}

	private String[] sepT(String time) {
		String[] timeParts = time.split(":");
		return timeParts;
	}

	public boolean le(LocalDateTime ldt) {
		return this.datetime.isBefore(ldt) || this.datetime.isEqual(ldt);
	}

	public boolean gt(LocalDateTime ldt) {
		return this.datetime.isAfter(ldt);
	}

	public static LocalDateTime getNextDT(LocalDateTime ldt, int minutes) {
		ldt = ldt.minusSeconds(ldt.getSecond());
		ldt = ldt.minusNanos(ldt.getNano());
		ldt = ldt.plusMinutes(1); // 早めに起動して[60, 0) 分以上前にこのメソッドが呼び出される前提

		for (int iMin = 0; iMin < 60; iMin++) {
			if ((ldt.plusMinutes(iMin)).getMinute() == minutes) {
				return ldt.plusMinutes(iMin);
			}
		}

		throw new RuntimeException("次の予測時刻が見つかりませんでした");
	}

	public static String fTime(LocalDateTime ldt) {
		return ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
	}

}

class RecSource {
	ParseDateTime pdt;
	String[] orgRec;
	String coDT;
	int recIndex;

	RecSource(ParseDateTime pdt, String[] orgRec, String coDT, int recIndex) {
		this.pdt = pdt;
		this.orgRec = orgRec;
		this.coDT = coDT;
		this.recIndex = recIndex;
	}

}

class CalcRadian {

	public static double getDateRadian(LocalDateTime ldt) {
		LocalDateTime curYearStart = LocalDateTime.of(ldt.getYear(), 1, 1, 0, 0, 0, 0);
		LocalDateTime nextYearStart = LocalDateTime.of(ldt.getYear() + 1, 1, 1, 0, 0, 0, 0);
		long daysOfYear = curYearStart.until(nextYearStart, ChronoUnit.DAYS);
		System.out.println(ldt.getYear() + "年は" + daysOfYear + "日です。");
		return 2.0D * Math.PI * ((double) ldt.getDayOfYear() - 1.0D) / (double) daysOfYear;
	}

	public static double getTimeRadian(LocalDateTime ldt) {
		LocalDateTime curDayStart = LocalDateTime.of(ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), 0, 0, 0,
				0);
		return 2.0D * Math.PI * (double) curDayStart.until(ldt, ChronoUnit.MINUTES) / (double) (24 * 60);
	}

}
