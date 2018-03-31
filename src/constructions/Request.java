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
	private int isReplyed=0;//����������ʾ��request�Ƿ񱻻ظ������޷����ظ�����ʼֵΪ0������ȴ��ظ���ֵΪ1�����ѻظ���ֵΪ2�����request���ܱ��ظ�
	private Coord location;//Ҫ��ѯ�����ݵĵص�
	private double time;//��ѯ��ʱ��
	private int type;//Ҫ��ѯ�����ݵ�����
	private double size=0;//��ѯ�������С���ݴ�С
	private int level;//��ѯ�ľ��ȼ���
	//ά��
	private Map<String,Double> dims=new LinkedHashMap<String,Double>();
	
	//status���ڱ�ʾһ��request��Ҫup��0����Ҫdown:1
	private int status;
	
	public Request(Coord l,double t,int ty,int level,int sta){
		this.location=l;
		this.time=t;
		this.type=ty;
		this.level=level;
		this.status=sta;
	}
	//���request�е�ά��
	public void addDim(String dimension,double d){
		this.dims.put(dimension, d);
	}
	/*
	 * �ж�һ�������Ƿ�����Ҫ������
	 * 1�������������͵���request���������
	 * 2���������ݵص���request����ص����
	 * 3�����ݵ�ά��ֵ��request�Ľ��շ�Χ֮��
	 */
	public boolean judgeData(Data d){
		if(this.getType()!=d.getType()) return false;
		if(this.getLocation().distance(d.getLocation())>MessageCenter.dis)
			return false;
		if(Math.abs(this.getTime()-d.getTime())>MessageCenter.okTime) return false;
		Set<String> dims= this.dims.keySet();
		//���ñ�ʶĬ��Ϊ0��Ϊfalse
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
		res.append(new Time(time).toString()).append("���ȼ���").append(level).append("��ѯ����")
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
	 * request�������ά��
	 */
	public void addDimensions(){
		if(type==0){
			Random r=new Random(System.currentTimeMillis());
			//���������Ƶʱ�䳤�ȣ�5-15���ӷ�Χ��
			double lenOfTime=r.nextDouble();
			if(lenOfTime<0)lenOfTime=0-lenOfTime;
			lenOfTime=lenOfTime*10+5;
			lenOfTime=lenOfTime*60;
			this.dims.put("Duration", lenOfTime);
			//��������龳������0/����1,���֮5�ĸ��ʳ���
			int situation=r.nextInt(10000);
			if(situation<5) situation=0;
			else situation=1;
			this.dims.put("Situation", (double)situation);
			int traSitua=r.nextInt(200);
			//������ɽ�ͨ���������0/һ��1/ӵ��2
			if(traSitua<60) traSitua=0;
			else if(traSitua<195) traSitua=1;
			else traSitua=2;
			this.dims.put("TrafficCondition", (double)traSitua);
			long seed=System.currentTimeMillis();
			//����������ɵ���Ƶʱ����������Ƶ��С
			//��ʱ����1������51.5KB��С
			double sizes=51.5*lenOfTime;
			this.size=sizes;
		}else if(type==1){
			Random r=new Random(System.currentTimeMillis());
			//�����������״��������0/����1/����2
			int i=r.nextInt(100);
			double weather=0;
			if(i<90) weather=0;
			else if(i<95) weather=1;
			else weather=2;
			this.dims.put("Weather", weather);
			//���ݵ�ǰʱ������ʱ���,�糿0/����1/����2/����3/����4
			double ntime=SimClock.getTime();
			int hour=(int) (ntime/3600);
			if(hour<7) hour=0;
			else if(hour<12) hour=1;
			else if(hour<14) hour=2;
			else if(hour<18) hour=3;
			else hour=4;
			this.dims.put("Time", (double)hour);
			//������ɽ�ͨ״��,����0/һ��1/ӵ��2
			int trafficSitu=r.nextInt(200);
			if(trafficSitu<60) trafficSitu=0;
			else if(trafficSitu<195) trafficSitu=1;
			else trafficSitu=2;
			this.dims.put("TrafficCondition",(double)trafficSitu);
			//ʹ�������������ͼ���С
			double sizes=r.nextDouble();
			if(sizes<0.2) sizes=sizes+0.3;
			else if(sizes<0.5) sizes=sizes;
			else sizes=sizes*2;
			this.size=sizes*1024;
		}else if(type==2){
			Random r=new Random(System.currentTimeMillis());
			//�����������ɳ���״̬,����0/�ϲ�1
			int states=r.nextInt(400);
			if(states<396) states=0;
			else states=1;
			this.dims.put("VehicleStatus", (double)states);
			//��ʱ������ɳ���
			int speeds=r.nextInt(50);
			this.dims.put("VehicleSpeed", (double)speeds);
			//����������ݴ�С��25KB-125KB)
			int sizes=r.nextInt(100)+25;
			this.size=sizes*1024;
		}else if(type==3){
			Random r=new Random(System.currentTimeMillis());
			//������ɷ�����ת��
			double angles=r.nextDouble();
			angles=angles*180;
			this.dims.put("SteeringWheelAngle", angles);
			//������ɼ����ų̶�
			r=new Random((long)SimClock.getTime());
			double degree=r.nextDouble();
			this.dims.put("GasPedal", degree);
			//����������ݴ�С(10-25KB)
			int sizes=r.nextInt(15)+10;
			this.size=sizes;
		}else if(type==4){
			Random r=new Random(System.currentTimeMillis());
			//���������������ͼƬ�еĳ�����(0-9��)
			int nums=r.nextInt(10);
			this.dims.put("NumOfVehicles",(double)nums);
			//������ɳ�����λ�ã�ƫ��0���м�1��ƫ��2��
			int position=r.nextInt(3);
			this.dims.put("LanePosition", (double)position);
			//������ɰ���ͼƬ���ݴ�С
			double sizes=r.nextDouble();
			if(sizes<0.2) sizes=sizes+0.3;
			else if(sizes<0.5) sizes=sizes;
			else sizes=sizes*2;
			this.size=sizes*1024;
		}
	}
	public int getIsReplyed() {
		return isReplyed;
	}
	public void setIsReplyed(int isReplyed) {
		this.isReplyed = isReplyed;
	}

}
