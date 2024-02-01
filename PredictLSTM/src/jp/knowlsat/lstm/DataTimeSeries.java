package jp.knowlsat.lstm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 入力データ読み込み用クラス
 * 
 * @author Kazuki Yoshida
 * @version 20230326
 */

public class DataTimeSeries {
	private List<Integer> targetArrayIndexes;
	private List<Integer> timeArrayIndexes;
	private ArrayList<ArrayList<Double>> data;
	private String[] datetimes;
	private String[] coDatetimes;

	public DataTimeSeries(String path, List<Integer> colIndexes, List<Integer> targetColIndexes,
			List<Integer> timeColIndexes,
			List<ColToFlagVals> colToFlagValsList, List<ChangeParamValByFlag> changeVal, int datetimeColIndex, int coDatetimeColIndex)
			throws IOException {

		// ファイル中で使用するデータの最大カラムインデックス
		int maxIndex = Collections.max(colIndexes);
		maxIndex = maxIndex > datetimeColIndex ? maxIndex : datetimeColIndex;
		maxIndex = maxIndex > coDatetimeColIndex ? maxIndex : coDatetimeColIndex;

		File file = new File(path);
		FileReader fileReader = new FileReader(file);
		@SuppressWarnings("resource")
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		// 全カラムそれぞれのArrayListを格納するArrayListを作成、サイズはLSTMに使用するパラメータ数
		var data = new ArrayList<ArrayList<Double>>(colIndexes.size());

		var datetimes = new ArrayList<String>();
		var coDatetimes = new ArrayList<String>();

		// 各カラムのデータを格納するArrayListを作成
		for (int i = 0; i < colIndexes.size(); i++) {
			data.add(new ArrayList<Double>());
		}

		// 入力データをCSVファイルから一行ずつ読み込み、上で用意したArrayListに格納する。
		String line;

		readLine: for (int row = 1; (line = bufferedReader.readLine()) != null; row++) {
			if (row == 1)
				continue; // ヘッダー行を読み飛ばす
			// System.out.println("row = " + row);

			String[] cols = line.split(",");

			// Arrays.stream(cols).forEach(s -> System.out.print(s + " "));
			// System.out.println();

			if (cols.length < maxIndex + 1) {
				throw new illegalFileFormatException(path + " : line " + row + "  " + (maxIndex + 1)
						+ " columns nead but exists only " + cols.length);
			}

			// if (mode == -1) {
			// } else if (mode == 0 && !cols[2].equals("0")) {
			//     continue;
			// } else if (mode == 30 && !cols[2].equals("30")) {
			// 	   continue;
			// }

			// 仮引数　List<ColToFlagVals>　colToFlagValsList の設定に従って条件に合わないレコードを読み飛ばす処理。
			for (ColToFlagVals flagCheckObj : colToFlagValsList) {
				if (flagCheckObj.isNotContain(cols)) {
					continue readLine;
				}
			}

			/* KS-PreProcessing */
			/* 取水停止ならば中次亜塩素酸注入(PLC31_PAR_AI0083)にホワイトノイズを設定 */
			// if (KSPP >= 0.0) {
			// 	if (cols[9].equals("0")) {
			// 		cols[15] = Double.toString(KSPP); // KSPP値をホワイトノイズ（KS秘伝の味）として付与
			// 	}
			// }

			for (var cv : changeVal) {
				if (cv.execChange()) {
					if (cols[cv.getFlagIndex()].equals(cv.getChangeFlagVal())) {
						cols[cv.getTargetIndex()] = Double.toString(cv.getChangeVal());
					}
				}
			}

			for (int i = 0; i < colIndexes.size(); i++) {
				data.get(i).add(Double.parseDouble(cols[colIndexes.get(i)]));
			}

			// 日付カラムの文字列を保存
			datetimes.add(cols[datetimeColIndex]);
			coDatetimes.add(cols[coDatetimeColIndex]);

		}
		// System.out.println();

		bufferedReader.close();

		this.targetArrayIndexes = targetColIndexes.stream().map(iObj -> Integer.valueOf(colIndexes.indexOf(iObj)))
				.toList();
		this.timeArrayIndexes = timeColIndexes.stream().map(iObj -> Integer.valueOf(colIndexes.indexOf(iObj))).toList();

		this.data = data;
		this.datetimes = datetimes.toArray(new String[datetimes.size()]);
		this.coDatetimes = coDatetimes.toArray(new String[coDatetimes.size()]);
	}

	/**
	 * getメソッド
	 * 入力時系列データが格納されたCSVファイルのパスを受け取り、ファイルからデータを読み込んでArrayList<ArrayList<Double>>に格納して返すメソッド
	 * 
	 * @param path       String型 入力データのファイルパス
	 * @param colIndexes List<Integer>型
	 *                   入力データCSVファイル中のどのカラムのデータを使用し、度の順番で格納するかを指定するList
	 * @return ArrayList<ArrayList<Double>>型
	 *         使用する入力時系列データを、種類ごとにArrayListに格納したものをさらにArrayListに格納した物
	 * @throws IOException
	 * @throws illegalFileFormatException when 入力行のカラム数が足りなかった時
	 */

	public ArrayList<ArrayList<Double>> getData() {
		return this.data;
	}

	public String[] getDatetimes() {
		return this.datetimes;
	}
	
	public String[] getCoDatetimes(){
		return this.coDatetimes;
	}

	public List<Integer> getTargetArrayIndexes() {
		return this.targetArrayIndexes;
	}

	public List<Integer> getTimeArrayIndexes() {
		return this.timeArrayIndexes;
	}

}

class illegalFileFormatException extends RuntimeException {
	illegalFileFormatException(String msg) {
		super(msg);
	}

}