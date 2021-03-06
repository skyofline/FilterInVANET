package constructions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import core.Coord;
import core.MessageCenter;
import core.SimClock;

public class Request {
	private int isReplyed=0;//该项用来表示该request是否被回复，或无法被回复。初始值为0，代表等待回复，值为1代表已回复，值为2代表该request不能被回复
	private Coord location;//要查询的数据的地点
	private double time;//查询的时间
	private int type;//要查询的数据的类型
	private double size=0;//查询需求的最小数据大小
	private int level;//查询的精度级别
	//维度
	private Map<String,Double> dims=new LinkedHashMap<String,Double>();
	
	//status用于表示一个request是要up：0还是要down:1
	private int status;
	
	public Request(Coord l,double t,int ty,int level,int sta){
		this.location=l;
		this.time=t;
		this.type=ty;
		this.level=level;
		this.status=sta;
	}
	//添加request中的维度
	public void addDim(String dimension,double d){
		this.dims.put(dimension, d);
	}
	/*
	 * 判断一条数据是否是想要的数据
	 * 1、本地数据类型等于request请求的数据
	 * 2、本地数据地点与request请求地点相近
	 * 3、数据的维度值在request的接收范围之内
	 */
	public boolean judgeData(Data d){
		if(this.getType()!=d.getType()) return false;
		if(this.getLocation().distance(d.getLocation())>MessageCenter.dis)
			return false;
		if(Math.abs(this.getTime()-d.getTime())>MessageCenter.okTime) return false;
		Set<String> dims= this.dims.keySet();
		//设置标识默认为0即为false
		int sign=0;
		for(String t:dims){
			if(!d.getDimensions().containsKey(t)){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		return true;
	}

	public Coord getLocation() {
		return location;
	}

	public void setLocation(Coord location) {
		this.location = location;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
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

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	public int getLevel(){
		return this.level;
	}
	public void setLevel(int level){
		this.level=level;
	}
	public String toString(){
		StringBuilder res=new StringBuilder("");
		res.append(new Time(time).toString()).append("精度级别").append(level).append("查询类型")
			.append(type).append("\n");
		for(String s:this.dims.keySet()){
			res=res.append(s).append(":").append(this.dims.get(s)).append(",");
		}
				
		return res.toString();
	}

	public Map<String,Double> getDims() {
		return dims;
	}

	public void setDims(Map<String,Double> dims) {
		this.dims = dims;
	}
	
	/*
	 * request添加数据维度
	 */
	public void addDimensions(){
		if(type==0){
			//随机生成视频时间长度（5-15分钟范围）
			double lenOfTime=60*(Math.random()*10+5);
			this.dims.put("Duration", lenOfTime);
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
			this.dims.put("TrafficCondition",traSitua);
			//根据随机生成的视频时长来生成视频大小
			//暂时假设1秒钟有51.5KB大小
			this.size=51.5*lenOfTime;
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
		}else if(type==2){
			//随机数随机生成车辆状态,良好0/较差1
			double states=Math.random();
			if(states<0.95) states=0;
			else states=1;
			this.dims.put("VehicleStatus",states);
			//暂时随机生成车速
			this.dims.put("VehicleSpeed",  Math.random()*90);
			//随机生成数据大小（25KB-125KB)
			this.size=Math.random()*100+25;
		}else if(type==3){
			//随机生成方向盘转角
			this.dims.put("SteeringWheelAngle", Math.random()*180);
			//随机生成加油门程度
			this.dims.put("GasPedal",Math.random());
			//随机生成数据大小(10-25KB)
			this.size=Math.random()*15+10;
		}else if(type==4){
			//随机生成汽车拍摄图片中的车辆数(0-9辆)
			this.dims.put("NumOfVehicles",(double)((int)Math.random()*10));
			//随机生成车道线位置（偏左0，中间1，偏右2）
			this.dims.put("LanePosition", (double)((int)Math.random()*3));
			//随机生成包含图片数据大小
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5) this.size*=2;
			this.size*=1024;
		}
	}
	public int getIsReplyed() {
		return isReplyed;
	}
	public void setIsReplyed(int isReplyed) {
		this.isReplyed = isReplyed;
	}

}
