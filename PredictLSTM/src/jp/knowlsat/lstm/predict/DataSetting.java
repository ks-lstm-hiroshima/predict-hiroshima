package jp.knowlsat.lstm.predict;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
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
	public double[][][] predictDataW;
	public double[][][] predictDataWT;
	public String[][] predictDatetimesWT;
	public String[][] predictCoDatetimesWT;
	

	public double[][] data;

	public DataSetting(int inputSize, int outputSize, int window, int test_mode, double KSPP, int ammonia_mode, int dataNumForTest, ArrayList<String[]> rTimeRecs)
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
		HashMap<Integer,DataNormalize> colDnMap = new HashMap<>( colIndexes.size() + 1, 1.0f);

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

		RealDataSetting rds = new RealDataSetting(rTimeRecs, test_mode, colIndexes, timeColIndexes, datetimeColIndex, colDnMap);

		this.predictDataSize = rds.getPredictDataSize();
		this.predictDataSize_window = rds.getPredictDataSize_window();
		this.predictDataW = rds.getPredictDataW();
		this.predictDataWT = rds.getPredictDataWT();
		this.predictDatetimesWT = rds.getPredictDatetimesWT();
		this.predictCoDatetimesWT = rds.getPredictCoDatetimesWT();
		

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


class RealDataSetting{
	private int predictDataSize;
	private int predictDataSize_window;
	private double[][][] predictDataW;
	private double[][][] predictDataWT;
	private String[][] predictDatetimesWT;
	private String[][] predictCoDatetimesWT;
	
	private HashMap<Integer,DataNormalize> colDnMap;
	private final HashMap<Integer,Integer> learningDataColToCol;
	{
		learningDataColToCol = new HashMap<>();
		learningDataColToCol.put(Integer.valueOf(0),  Integer.valueOf(0));
		learningDataColToCol.put(Integer.valueOf(8),  Integer.valueOf(3));
		learningDataColToCol.put(Integer.valueOf(11), Integer.valueOf(14));
		learningDataColToCol.put(Integer.valueOf(12), Integer.valueOf(14));
		learningDataColToCol.put(Integer.valueOf(13), Integer.valueOf(14));
		learningDataColToCol.put(Integer.valueOf(14), Integer.valueOf(27));
		learningDataColToCol.put(Integer.valueOf(15), Integer.valueOf(33));
		learningDataColToCol.put(Integer.valueOf(16), Integer.valueOf(40));
		learningDataColToCol.put(Integer.valueOf(17), Integer.valueOf(45));
		learningDataColToCol.put(Integer.valueOf(18), Integer.valueOf(46));
		learningDataColToCol.put(Integer.valueOf(19), Integer.valueOf(47));
		learningDataColToCol.put(Integer.valueOf(20), Integer.valueOf(48));
		learningDataColToCol.put(Integer.valueOf(21), Integer.valueOf(49));
		learningDataColToCol.put(Integer.valueOf(22), Integer.valueOf(50));
		learningDataColToCol.put(Integer.valueOf(23), Integer.valueOf(51));
		learningDataColToCol.put(Integer.valueOf(24), Integer.valueOf(63));
	}
	
	RealDataSetting(ArrayList<String[]> rTimeRecs, int test_mode, List<Integer> colIndexes, List<Integer> timeColIndexes, int datetimeColIndex, HashMap<Integer,DataNormalize> colDnMap){
		this.colDnMap = colDnMap;
		
		int dtColIndex = this.learningDataColToCol.get(Integer.valueOf(datetimeColIndex));
		
		int previousRecIndex = 0;
		String dtStr;
		ParseDateTime parsedDT = null;
		for( ;previousRecIndex < rTimeRecs.size(); previousRecIndex++) {
			dtStr = rTimeRecs.get(previousRecIndex) [dtColIndex];
			parsedDT = new ParseDateTime(dtStr);
			if(parsedDT.minutes == test_mode) { // テストモード　-1（30分間隔モデル）の時にこのコードは正常に機能しない
				break;
			}
		}
		if( previousRecIndex >= rTimeRecs.size()) {
			System.out.println("前の定刻レコードが見つかりませんでした。");
			System.out.println("previousRecIndex = " + previousRecIndex);
			System.out.println("rTimeRecs.size() = " + rTimeRecs.size());
			System.exit(-999);
		}
		
		System.out.print( test_mode + "分モデル予測モード前の予測点は :  ");
		System.out.println(parsedDT.datetime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
		
	}
	
	int getPredictDataSize(){
		return this.predictDataSize;
	}

	int getPredictDataSize_window(){
		return this.predictDataSize_window;
	}
	
	double[][][] getPredictDataW(){
		return this.predictDataW;
	}
	
	double[][][] getPredictDataWT(){
		return this.predictDataWT;
	}
	
	String[][] getPredictDatetimesWT(){
		return this.predictDatetimesWT;
	}
	
	String[][] getPredictCoDatetimesWT(){
		return this.predictCoDatetimesWT;
	}
}


class ParseDateTime{
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
	
	ParseDateTime(String datetime){
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
	
	private String[] sepD02(String date) {
		String[] dateParts = date.split("/");
		return dateParts;
	}
	
	private String[] sepT(String time) {
		String[] timeParts = time.split(":");
		return timeParts;
	}	
}

