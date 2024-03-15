package jp.knowlsat.lstm.predict;

import java.util.Collections;
import java.util.List;

/**
 * 量的データを正規化するクラス
 * 
 * @author Kazuki Yoshida
 * @version 20231208
 */

// 正規化を行うクラスのスーパークラス
// 現状Min-Max法での正規化を行う NormalNormalizeクラスと
// cosinで正規化を行う CoslNormalizeクラスがこのクラスを継承。
abstract public class DataNormalize {
	private double[] data;

	protected void set_data(double[] data) {
		this.data = data;
	}

	public double[] get() {
		return this.data;
	}
	
	public abstract double normalize(double val);
}

// 通常の Min-Max 法による正規化を行うクラス。
// DataNormalize を継承
// [0, 1]、[-1, 0]、[-1, 1]　三通りの正規化方法があるが、実際の処理は委譲
class NormalNormalize extends DataNormalize {
	private Normalizer normalizer;
	private Double oMax;
	private Double oMin;

	public NormalNormalize(List<Double> array) {
		Double max = Collections.max(array);
		Double min = Collections.min(array);
		this.oMax = max;
		this.oMin = min;

		if (min >= 0.0) { // [0, 1]
			this.normalizer = new PlusNormalizer(max,min);
		} else if (max <= 0.0) { // [-1, 0]
			this.normalizer = new MinusNormalizer(max,min);
		} else { // [-1, 1]
			this.normalizer = new PlusMinusNormalizer(max,min);
		}
		double[] data = normalizer.normalize(array);
		super.set_data( data );
	}

	public double inv(double x) {
		return this.normalizer.inv(x);
	}

	public boolean isNormalRange(Double val) {
		if( val.compareTo(this.oMax) <= 0 && val.compareTo(this.oMin) >= 0 ) {
			return true;
		}
		return false;
	}
	
	@Override
	public double normalize(double val) {
		return normalizer.normalize(val);
	}
}


// [0, 1]、[-1, 0]、[-1, 1]　三通りの正規化方法を実装する３つのクラスのスーパークラス
abstract class Normalizer{
	protected abstract double[] normalize( List<Double> array);
	protected abstract double inv(double x);
	protected abstract double normalize(double val);
}


// [0, 1]の正規化処理と、逆変換を実装したクラス。　
class PlusNormalizer extends Normalizer{
	private final double EPS = 1.0E-12;
	private double epsDiv;
	private double min;

	PlusNormalizer(double max, double min){
		this.epsDiv = max - min + EPS;
		this.min = min;
	}


	@Override
	public double[] normalize( List<Double> array) {
		return array.stream().mapToDouble(elem -> ((elem - min) / epsDiv)).toArray();
	}

	@Override
	public double inv(double x) {
		return (x * epsDiv + min);
	}
	
	@Override
	public double normalize(double val) {
		return (val - min) / epsDiv;
	}
}


// [-1, 0]の正規化処理と、逆変換を実装したクラス。
class MinusNormalizer extends Normalizer{
	private final double EPS = 1.0E-12;
	private double epsDiv;
	private double max;

	MinusNormalizer(double max, double min){
		this.epsDiv = max - min + EPS;
		this.max = max;
	}

	@Override
	public double[] normalize( List<Double> array) {
		return array.stream().mapToDouble(elem -> ((elem - max) / epsDiv)).toArray();
	}

	@Override
	public double inv(double x) {
		return (x * epsDiv + max);
	}
	
	@Override
	public double normalize(double val) {
		return (val - max) / epsDiv;
	}
}


// [-1, 1]の正規化処理と、逆変換を実装したクラス。
class PlusMinusNormalizer extends Normalizer{
	private final double EPS = 1.0E-12;
	private double epsDiv;
	private double min;

	PlusMinusNormalizer(double max, double min){
		this.epsDiv = (max - min + EPS) * 0.5;
		this.min = min;
	}

	public double[] normalize( List<Double> array) {
		return array.stream().mapToDouble(elem -> ((elem - min) / epsDiv - 1.0)).toArray();
	}

	@Override
	public double inv(double x) {
		return x * epsDiv;
	}
	
	@Override
	public double normalize(double val) {
		return (val - min) / epsDiv - 1.0;
	}
}


// cosin関数で正規化するクラス。正規化後の値からの逆変換はできないので実装せず。
class CoslNormalize extends DataNormalize {
	public CoslNormalize(List<Double> array) {
		double[] data = array.stream().mapToDouble(elem -> -Math.cos(elem)).toArray();
		super.set_data(data);
	}
	
	@Override
	public double normalize(double val) {
		return -Math.cos(val);
	}
}

