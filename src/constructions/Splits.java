package constructions;

public class Splits{
	private String dimension;
	private int splits=1;
	private double minBord;
	private double maxBord;
	public Splits(String str,int split,double min,double max){
		this.setDimension(str);
		this.setSplits(split);
		this.setMinBord(min);
		this.setMaxBord(max);
	}
//	public void doSplit(){
//		
//	}
	/*
	 * 判断一个splits是否与另一个splits相同
	 */
	public boolean isEqual(Splits s){
		if(this.getDimension().equals(s.getDimension())
				&&this.getMinBord()==s.getMinBord()
				&&this.getMaxBord()==s.getMaxBord())
			return true;
		else return false;
	}
	//判断一个值是不是从属于该splits的范围
	public boolean inRange(double s){
		if(s<=this.getMaxBord()&&s>=this.getMinBord()) return true;
		else return false;
	}
	//判断一个split是不是从属于该split的范围内
	public boolean inRange(Splits s){
		if(!s.getDimension().equals(this.dimension)) return false;
		else{
			if(s.getMinBord()>=this.getMinBord()&&s.getMaxBord()<=this.getMaxBord())
				return true;
		}
		return false;
	}
	public String toString(){
		return this.getDimension()+"分片数为："+this.getSplits()+"，最大值为："
				+this.getMaxBord()+"，最小值为："+this.getMinBord();
	}
	public double getMinBord() {
		return minBord;
	}
	public void setMinBord(double minBord) {
		this.minBord = minBord;
	}
	public double getMaxBord() {
		return maxBord;
	}
	public void setMaxBord(double maxBord) {
		this.maxBord = maxBord;
	}
	public String getDimension() {
		return dimension;
	}
	public void setDimension(String dimension) {
		this.dimension = dimension;
	}
	public int getSplits() {
		return splits;
	}
	public void setSplits(int splits) {
		this.splits = splits;
	}
}