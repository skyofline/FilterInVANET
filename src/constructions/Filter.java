package constructions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.Coord;
import core.DTNHost;
import core.MessageCenter;
import core.SimClock;

public class Filter {
	//filter控制的数据中被使用的个数
	private int usedDatas=0;
	//filter控制的数据中被允许传播但使用次数为0的数据
	private int unusedPassDatas=0;
	//filter控制的数据中不被允许传播但是被使用查询多次的数据
	private int usedBlockDatas=0;
	//计算split factor的重要参数,0-1，暂时默认为0.5
	private double balanceFactor=0.5;
	//计算cost_radio的参数，factor for data transmission
	private double dataTransmFactor=1.3;
	//计算cost_radio的参数，factor for request transmission
	private double reqTransmFactor=1;
	private int type;//filter 要求的数据类型，文本/图像/音频/视频
	private double time;//filter产生时间
	private Coord loc;//filter所属区域
	private int size;//数据最大存储量
	private int level;//要获取的数据的精度等级
	private int status;//表示filter状态open:0/close:1/basic:2/more:3
	private double periodTime=1200;//是一个filter成熟时间
	private double basicTime=this.getTime();//是一个filter被重新检查的起始时间
	private int basicStatus=0;//一个filter是否被指为basic filter，0代表不是，1代表是，默认为0
	private double threshold;//阈值，用于判断一条信息是否适合
	private int newTimes;
	private int oldTimes;
	private Map<String,Splits> dims=new LinkedHashMap<String,Splits>();
	public Filter(int type,Coord loc,int level,int state){
		this.time=SimClock.getTime();
		this.type=type;
		this.loc=loc;
		this.level=level;
		this.setStatus(state);		
		this.setNewTimes(this.setOldTimes(0));
	}
	public Filter copy(){
		Filter f=new Filter(this.getType(),this.getLoc()
				,this.getLevel(),this.getStatus());
		f.setBalanceFactor(balanceFactor);
		f.setDataTransmFactor(dataTransmFactor);
		f.setReqTransmFactor(reqTransmFactor);
		f.setTime(time);
		f.setSize(size);
		f.setLevel(level);
		f.setStatus(status);
		f.setPeriodTime(periodTime);
		f.setBasicTime(basicTime);
		f.setThreshold(threshold);
		f.setNewTimes(newTimes);
		f.setOldTimes(oldTimes);
		f.setDims(dims);
		return f;
	}
//	//计算一个值判断是否应保留数据
//	//1代表保留，0代表不保留
//	public int evaluData(Data d){
//		if(d.getSize()>this.getSize()) return 1;
//		else return 0;
//	}
	//添加filter中的维度,根据单个的key和value来判断
	public void addDimension(String key,double value1,double value2){
		Splits s=new Splits(key,1,value1,value2);
		this.dims.put(key, s);
	}
	/*
	 * filter tostring,展示filter的结构和功能
	 */
	public String toString(){
		String res="";
		res=res+"filter的维度：\n";
		for(String s:this.dims.keySet()){
			res=res+this.dims.get(s).toString()+"\n";
		}
		return res;
	}
	/*
	 * 定时更新filter,如果该filter的data数或request数为0，则设为close状态
	 * 否则，则计算cost ratio的值，若大于1 则设为open状态，反之设为close状态
	 */
	public void updateStatusByRadioCost(List<Data>datas,List<Request> requests){
		if(datas.size()==0||requests.size()==0) this.setStatus(1);
		else if(this.calRadioCost(datas,requests)>1) this.setStatus(0);
		else this.setStatus(1);
	}
	//根据filter的开闭评估数据的可否传播情况
	public void resetDataStatus(Data d){
		
		if(this.getStatus()==0)d.setExpandState(0);
		else {
			d.setExpandState(1);
		}
	}
	//计算数据的被使用次数和时间的长度比来判断该数据的可用可信程度
	public double getUsedWithTime(Data d){
		return d.getUsageCount()/(SimClock.getTime()-d.getTime());
	}
	//判断数据是否是符合该filter的要求 false:不符合，true:符合,默认不符合
	public boolean judgeData(Data d){
		//首先判断数据所有维度都从属于filter维度
		Map<String,Double> data=d.getDimensions();
		@SuppressWarnings("unchecked")
		Set<String> l= data.keySet();
		/*
		 * 判断数据的维度是否都存在与filter维度中
		 */
		int sign=0;//sign为1时表示数据有的维度不从属于filter中
		for(String s:l){
			if(!this.getDims().containsKey(s)){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		/*
		 * 判断数据的维度值都在filter的对应维度值的区间之内
		 */
		for(String s:l){
			if(!this.getDims().get(s).inRange(data.get(s))){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		
		else return true;
	}
	
	//判断request是否符合filter的要求
	public boolean judgeRequest(Request r){
		//首先判断request所有维度都从属于filter维度
		Map<String,Double> req=r.getDims();
		Set<String> l=req.keySet();
		/*
		* 判断数据的维度是否都存在与filter维度中
		* 判断数据的维度值都在filter的对应维度值的区间之内
		*/
		int sign=0;//sign为1时表示数据有的维度不从属于filter中
		for(String s:l){
			if((!this.getDims().containsKey(s))||(!this.getDims().get(s).inRange(req.get(s)))){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		else return true;
	}
	//判断一个filter是否与另外一个filter类似
	public boolean isEqual(Filter f){
		if(this.getType()==f.getType())return true;
		else return false;
	}
	//复制现有的filter，生成新的filter
	public Filter copyFromFilter(Filter f){
		return new Filter(f.getType(),f.getLoc(),f.getLevel(),f.getStatus());
	}
	

	//计算filter的radio cost
	public double calRadioCost(List<Data> datas,List<Request> requests){
		double res= this.dataMatchRatio(datas)+(this.reqTransmFactor/this.dataTransmFactor)*this.requestToDataRatio(datas,requests);
		return res;
	}
	//计算某个filter中的data match ratio,根据数据的使用次数判断
	public double dataMatchRatio(List<Data> datas){
		if(datas.size()==0) return 0;
		else return (double)this.usedDatas/datas.size();
	}
	
	//计算某个filter的request to data ratio
	public double requestToDataRatio(List<Data>datas,List<Request>requests){
		return (double)requests.size()/datas.size();
	}
	//计算filter的mistake factor
	public double getMismatchFactor(List<Data>datas){
		double res=0.5;
		return res*(this.usedBlockDatas+this.unusedPassDatas)/datas.size();
	}
	/*
	 * 计算一个filter是不是另一个filter的base filter
	 */
	public boolean isBased(Filter f){
		Iterator it=this.dims.keySet().iterator();
		while(it.hasNext()){
			String d=(String) it.next();
			//判断各个维度值是否是该维度上的基本值，除了时间
//			if(d!="Time"&&this.dims.get(d)!=0){
//				return false;
//			}
			//判断要比较的filter是否包含本filter的全部维度
			if(!f.getDims().containsKey(d)){
				return false;
			}
		}
		return true;
	}
	/*
	 * 根据现有filter生成基本filter
	 */
	public Filter generBasicFilter(){
		Filter f=this.copy();
		return f;
	}

	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public Coord getLoc() {
		return loc;
	}
	public void setLoc(Coord loc) {
		this.loc = loc;
	}
	
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	
	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	public int getStatus() {
		return this.status;
	}
	public void setStatus(int state) {
		this.status = state;
	}
	public int getSize(){
		return this.size;
	}
	public void setSize(int size){
		this.size=size;
	}
	public int getNewTimes() {
		return newTimes;
	}
	public void setNewTimes(int newTimes) {
		this.newTimes = newTimes;
	}
	public int getOldTimes() {
		return oldTimes;
	}
	public int setOldTimes(int oldTimes) {
		this.oldTimes = oldTimes;
		return oldTimes;
	}

	public double getBalanceFactor() {
		return balanceFactor;
	}
	public void setBalanceFactor(double balanceFactor) {
		this.balanceFactor = balanceFactor;
	}
	public double getDataTransmFactor() {
		return dataTransmFactor;
	}
	public void setDataTransmFactor(double dataTransmFactor) {
		this.dataTransmFactor = dataTransmFactor;
	}
	public double getReqTransmFactor() {
		return reqTransmFactor;
	}
	public void setReqTransmFactor(double reqTransmFactor) {
		this.reqTransmFactor = reqTransmFactor;
	}
	public double getPeriodTime() {
		return periodTime;
	}
	public void setPeriodTime(double periodTime) {
		this.periodTime = periodTime;
	}
	public double getBasicTime() {
		return basicTime;
	}
	public void setBasicTime(double basicTime) {
		this.basicTime = basicTime;
	}
	public Map<String,Splits> getDims(){
		return this.dims;
	}
	public void setDims(Map<String,Splits> m){
		this.dims=m;
	}
	public int getBasicStatus() {
		return basicStatus;
	}
	public void setBasicStatus(int basicStatus) {
		this.basicStatus = basicStatus;
	}
	public int getUsedDatas() {
		return usedDatas;
	}
	public void setUsedDatas(int usedDatas) {
		this.usedDatas = usedDatas;
	}
	public void addUsedDatas(int i){
		this.setUsedDatas(this.getUsedDatas()+i);
	}
	public void addUnusedPassDatas(int i){
		this.setUnusedPassDatas(this.getUnusedPassDatas()+i);
	}
	public void addUsedBlockDatas(int i){
		this.setUsedBlockDatas(this.getUsedBlockDatas()+i);
	}
	public int getUnusedPassDatas() {
		return unusedPassDatas;
	}
	public void setUnusedPassDatas(int unusedPassDatas) {
		this.unusedPassDatas = unusedPassDatas;
	}
	public int getUsedBlockDatas() {
		return usedBlockDatas;
	}
	public void setUsedBlockDatas(int usedBlockDatas) {
		this.usedBlockDatas = usedBlockDatas;
	}
}
