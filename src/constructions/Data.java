package constructions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import core.Coord;
import core.MessageCenter;
import core.SimClock;

public class Data {
	//数据产生时间
	private double time;
	private int id;//数据来源
	//数据格式,共有5种（视频0、图像1、车辆状态2、驾驶员行为3、人工标记4）
	private int type;
	private double size;//数据大小
	private Coord location;//数据产生地点
	private int level;//数据精度等级
	private double threshold=2.5;//数据阈值
	private Map<String,Double> dims=new LinkedHashMap<String, Double>();
	private int usageCount=0;//数据被request接纳的次数
	private int expandState=0;//数据能否通过filter的状态表示，pass:0,close:1
	public Data(double t,int i,int ty,int l,Coord loca){
		this.time=t;
		this.id=i;
		this.type=ty;
		this.level=l;
		this.location=loca;
		this.size=0;
	}
	public void fillData(){
		if(type==0){
			//随机生成视频时间长度（5-15分钟范围）
			double lenOfTime=60*(Math.random()*10+5);
			this.dims.put("Duration",lenOfTime );
			//随机生成情境，车祸0/正常1,万分之5的概率车祸
			double situation=Math.random();
			if(situation<0.0005) situation=0;
			else situation=1;
			this.dims.put("Situation", situation);
			double traSitua=Math.random();
			//随机生成交通情况，良好0/一般1/拥堵2
			if(traSitua<0.35) traSitua=0;
			else if(traSitua<0.85) traSitua=1;
			else traSitua=2;
			this.dims.put("TrafficCondition", traSitua);
			//根据随机生成的视频时长来生成视频大小
			//暂时假设1秒钟有51.5KB大小
			this.size=51.5*lenOfTime;
			this.dims.put("Size",this.size);
		}else if(type==1){
			//随机生成天气状况，晴天0/阴天1/降雨2
			double weather=Math.random();
			if(weather<0.75) weather=0;
			else if(weather<0.9) weather=1;
			else weather=2;
			this.dims.put("Weather", weather);
			//根据当前时间生成时间段,早晨0/上午1/中午2/下午3/晚上4
			double hour=SimClock.getTime()/3600;
			if(hour<7) hour=0;
			else if(hour<12) hour=1;
			else if(hour<14) hour=2;
			else if(hour<18) hour=3;
			else hour=4;
			
			this.dims.put("Time",hour);
			//随机生成交通状况,良好0/一般1/拥堵2
			double trafficSitu=Math.random();
			if(trafficSitu<0.3) trafficSitu=0;
			else if(trafficSitu<0.9) trafficSitu=1;
			else trafficSitu=2;
			this.dims.put("TrafficCondition",trafficSitu);
			//使用随机数据生成图像大小
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5) this.size*=2;
			this.size*=1024;
			this.dims.put("Size",this.size);

		}else if(type==2){
		
			//随机数随机生成车辆状态,良好0/较差1
			double states=Math.random(); 
			if(states<0.95) states=0;
			else states=1;
			this.dims.put("VehicleStatus",states);
			//暂时随机生成车速
			this.dims.put("VehicleSpeed", Math.random()*90);
			//随机生成数据大小（25KB-125KB)
			this.size=Math.random()*100+25;
			this.dims.put("Size",this.size);

		}else if(type==3){
			//随机生成方向盘转角
			this.dims.put("SteeringWheelAngle",Math.random()*180);
			//随机生成加油门程度
			this.dims.put("GasPedal", Math.random());
			//随机生成数据大小(10-25KB)
			this.size=Math.random()*15+10;
			this.dims.put("Size",this.size);

		}else if(type==4){
			//随机生成汽车拍摄图片中的车辆数(0-9辆)
			this.dims.put("NumOfVehicles",(double)((int)Math.random()*10));
			//随机生成车道线位置（偏左0，中间1，偏右2）
			this.dims.put("LanePosition", (double)((int)Math.random()*3));
			//随机生成包含图片数据大小
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5)this.size*=2;
			this.size*=1024;
			this.dims.put("Size",this.size);

		}
	}
	public void addDimension(String key,double value){
		this.dims.put(key, value);
	}
	public Map<String,Double> getDimensions(){
		return this.dims;
	}
	 
	/*
	 * 判断数据是否与另一条数据相似，可整合(non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public boolean isSimalar(Data nd){
		if(this.getType()!=nd.getType()||
				(this.getTime()-nd.getTime())>MessageCenter.okTime||
				(nd.getTime()-this.getTime())>MessageCenter.okTime||
				this.getLocation().distance(nd.getLocation())>MessageCenter.dis)
			return false;
		else{
			for(String s:this.getDimensions().keySet())
				if(!nd.getDimensions().containsKey(s)||!this.isInRange(s, this.getDimensions().get(s), nd.getDimensions().get(s)))
					return false;
			
		}
		return true;
	}
	
	public String toString(){
		StringBuilder res=new StringBuilder("数据来自于");
		res.append(this.id).append(",数据格式为：").append(this.type).append(" ,数据大小为：")
			.append(this.size);
		for(String dim:this.dims.keySet()){
			res=res.append(dim).append(":").append(this.dims.get(dim)).append(",");
		}
		return res.toString();
	}
	/*
	 * 判断两条数据是否是一致的数据
	 */
	public boolean isEqual(Data d){
		//数据通过层层检查最后返回true，两条数据一致
		if(this.getType()!=d.getType()||
				(this.getTime()-d.getTime())>MessageCenter.okTime||
						(d.getTime()-this.getTime())>MessageCenter.okTime||
						this.getLocation().distance(d.getLocation())>MessageCenter.dis)
			return false;
		else{
			for(String s:this.getDimensions().keySet()){
				if(!d.getDimensions().containsKey(s)
						||!this.isInRange(s, this.getDimensions().get(s), d.getDimensions().get(s)))
						return false;
			}
		}
		return true;
	}
	/*
	 *判断一条数据上的维度值与另一条数据的对应维度上的值是否在一定范围内可视为相等
	 */
	public boolean isInRange(String s,double dest1,double dest2){
		if(!this.dims.containsKey(s)) return false;
		//如果数据能通过检查则视为相等
		if(s.equals("Weather")){
			if(Math.abs(dest1-dest2)>0.5) return false;
		}else if(s.equals("Time")){
			if(Math.abs(dest1-dest2)>0.5) return false;
		}else if(s.equals("TrafficCondition")){
			if(Math.abs(dest1-dest2)>0.5) return false;
		}else{
			if(Math.abs(dest1-dest2)>0.5) return false;
		}
		return true;
	}
	public Coord getLocation(){
		return this.location;
	}
	public void setLocation(Coord location) {
		this.location = location;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public double getTime(){
		return this.time;
	}
	public void setTime(double t){
		this.time=t;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}

	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	public int getUsageCount() {
		return usageCount;
	}
	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}
	public int getExpandState() {
		return expandState;
	}
	public void setExpandState(int expandState) {
		this.expandState = expandState;
	}
	//数据添加使用次数
	public void addUsageCount(){
		this.usageCount++;
	}
}
