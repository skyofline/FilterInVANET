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
	//����split factor����Ҫ����,0-1����ʱĬ��Ϊ0.5
	private double balanceFactor=0.5;
	//����cost_radio�Ĳ�����factor for data transmission
	private double dataTransmFactor=0.5;
	//����cost_radio�Ĳ�����factor for request transmission
	private double reqTransmFactor=0.5;
	private int type;//filter Ҫ����������ͣ��ı�/ͼ��/��Ƶ/��Ƶ
	private double time;//filter����ʱ��
	private Coord loc;//filter��������
	private int size;//�������洢��
	private int level;//Ҫ��ȡ�����ݵľ��ȵȼ�
	private int status;//��ʾfilter״̬open:0/close:1/basic:2/more:3
	private double periodTime=1200;//��һ��filter����ʱ��
	private double basicTime=this.getTime();//��һ��filter�����¼�����ʼʱ��
	private int basicStatus=0;//һ��filter�Ƿ�ָΪbasic filter��0�����ǣ�1�����ǣ�Ĭ��Ϊ0
	private double threshold;//��ֵ�������ж�һ����Ϣ�Ƿ��ʺ�
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
//	//����һ��ֵ�ж��Ƿ�Ӧ��������
//	//1��������0��������
//	public int evaluData(Data d){
//		if(d.getSize()>this.getSize()) return 1;
//		else return 0;
//	}
	//���filter�е�ά��,���ݵ�����key��value���ж�
	public void addDimension(String key,double value1,double value2){
		Splits s=new Splits(key,1,value1,value2);
		this.dims.put(key, s);
	}
	/*
	 * filter tostring,չʾfilter�Ľṹ�͹���
	 */
	public String toString(){
		String res="";
		res=res+"filter��ά�ȣ�\n";
		for(String s:this.dims.keySet()){
			res=res+ this.dims.get(s).toString()+"\n";
		}
		return res;
	}
	/*
	 * ��ʱ����filter,�����filter��data����request��Ϊ0������Ϊclose״̬
	 * ���������cost ratio��ֵ��������1 ����Ϊopen״̬����֮��Ϊclose״̬
	 */
	public void updateStatusByRadioCost(List<Data>datas,List<Request> requests){
		if(datas.size()==0||requests.size()==0) this.setStatus(1);
		else if(this.calRadioCost(datas,requests)>1) this.setStatus(0);
		else this.setStatus(1);
	}
	//����filter�Ŀ����������ݵĿɷ񴫲����
	public void resetDataStatus(Data d){
		
		if(this.getStatus()==0)d.setExpandState(0);
		else {
			d.setExpandState(1);
		}
	}
	//�������ݵı�ʹ�ô�����ʱ��ĳ��ȱ����жϸ����ݵĿ��ÿ��ų̶�
	public double getUsedWithTime(Data d){
		return d.getUsageCount()/(SimClock.getTime()-d.getTime());
	}
	//�ж������Ƿ��Ƿ��ϸ�filter��Ҫ�� false:�����ϣ�true:����,Ĭ�ϲ�����
	public boolean judgeData(Data d){
		//�����ж���������ά�ȶ�������filterά��
		Map<String,Double> data=d.getDimensions();
		@SuppressWarnings("unchecked")
		Set<String> l= data.keySet();
		/*
		 * �ж����ݵ�ά���Ƿ񶼴�����filterά����
		 */
		int sign=0;//signΪ1ʱ��ʾ�����е�ά�Ȳ�������filter��
		for(String s:l){
			if(!this.getDims().containsKey(s)){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		/*
		 * �ж����ݵ�ά��ֵ����filter�Ķ�Ӧά��ֵ������֮��
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
	
	//�ж�request�Ƿ����filter��Ҫ��
	public boolean judgeRequest(Request r){
		//�����ж�request����ά�ȶ�������filterά��
		Map<String,Double> req=r.getDims();
		Set<String> l=req.keySet();
		/*
		* �ж����ݵ�ά���Ƿ񶼴�����filterά����
		*/
		int sign=0;//signΪ1ʱ��ʾ�����е�ά�Ȳ�������filter��
		for(String s:l){
			if(!this.getDims().containsKey(s)){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		/*
		* �ж����ݵ�ά��ֵ����filter�Ķ�Ӧά��ֵ������֮��
		*/
		for(String s:l){
			if(!this.getDims().get(s).inRange(req.get(s))){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		else return true;
	}
	//�ж�һ��filter�Ƿ�������һ��filter����
	public boolean isEqual(Filter f){
		if(this.getType()==f.getType())return true;
		else return false;
	}
	//�������е�filter�������µ�filter
	public Filter copyFromFilter(Filter f){
		return new Filter(f.getType(),f.getLoc(),f.getLevel(),f.getStatus());
	}
	

	//����filter��radio cost
	public double calRadioCost(List<Data> datas,List<Request> requests){
		double res= this.dataMatchRatio(datas,requests)+(this.reqTransmFactor/this.dataTransmFactor)*this.requestToDataRatio(datas,requests);
		return res;
	}
	//����ĳ��filter�е�data match ratio,�������ݵ�ʹ�ô����ж�
	public double dataMatchRatio(List<Data> datas,List<Request> requests){
		int num=0;
		for(Data d:datas){
			if(d.getUsageCount()>0) num++;
		}
		return (double)num/datas.size();
	}
	
	//����ĳ��filter��request to data ratio
	public double requestToDataRatio(List<Data>datas,List<Request>requests){
		return (double)requests.size()/datas.size();
	}
	//����filter��mistake factor
	public double getMistakeFactor(List<Data>datas){
		double res=0.5;
		Iterator it=datas.iterator();
		int passNum=0,blockNum=0,sum=0;
		while(it.hasNext()){
			Data d=(Data) it.next();
			sum++;
			if(d.getExpandState()==0&&d.getUsageCount()==0) passNum++;
			else if(d.getExpandState()==1&&d.getUsageCount()>0) blockNum++;
		}
		return res*(blockNum+passNum)/sum;
	}
	/*
	 * ����һ��filter�ǲ�����һ��filter��base filter
	 */
	public boolean isBased(Filter f){
		Iterator it=this.dims.keySet().iterator();
		while(it.hasNext()){
			String d=(String) it.next();
			//�жϸ���ά��ֵ�Ƿ��Ǹ�ά���ϵĻ���ֵ������ʱ��
//			if(d!="Time"&&this.dims.get(d)!=0){
//				return false;
//			}
			//�ж�Ҫ�Ƚϵ�filter�Ƿ������filter��ȫ��ά��
			if(!f.getDims().containsKey(d)){
				return false;
			}
		}
		return true;
	}
	/*
	 * ��������filter���ɻ���filter
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
}
