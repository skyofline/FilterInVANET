package constructions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import core.Coord;
import core.MessageCenter;
import core.SimClock;

public class Data {
	//���ݲ���ʱ��
	private double time;
	private int id;//������Դ
	//���ݸ�ʽ,����5�֣���Ƶ0��ͼ��1������״̬2����ʻԱ��Ϊ3���˹����4��
	private int type;
	private double size;//���ݴ�С
	private String content;//��������
	private Coord location;//���ݲ����ص�
	private int level;//���ݾ��ȵȼ�
	private double threshold=2.5;//������ֵ
	private Map<String,Double> dims=new LinkedHashMap<String, Double>();
	private int usageCount=0;//���ݱ�request���ɵĴ���
	private int expandState=0;//�����ܷ�ͨ��filter��״̬��ʾ��pass:0,close:1
	public Data(double t,int i,int ty,int l,String c,Coord loca){
		this.time=t;
		this.id=i;
		this.type=ty;
		this.level=l;
		this.content=c;
		this.location=loca;
		this.size=0;
	}
	public void fillData(){
		if(type==0){
			Random r=new Random(System.currentTimeMillis());
			//���������Ƶʱ�䳤�ȣ�5-15���ӷ�Χ��
			double lenOfTime=r.nextDouble()*10+5;
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
			double sizes=51.5*1024*lenOfTime;
			
			this.size=sizes;
			this.dims.put("Size",this.size);
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
			if(sizes<0) sizes=0-sizes;
			if(sizes<0.2) sizes=sizes+0.3;
			else if(sizes<0.5) sizes=sizes;
			else sizes=sizes*2;
			this.size=sizes*1024*1024;
			this.dims.put("Size",this.size);

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
			this.dims.put("Size",this.size);

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
			this.size=sizes*1024;
			this.dims.put("Size",this.size);

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
			this.size=sizes*1024*1024;
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
	 * �ж������Ƿ�����һ���������ƣ�������(non-Javadoc)
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
		String res="";
		res=res+" ����������"+this.id
				+",���ݸ�ʽΪ��"+this.type+" ,���ݴ�СΪ��"+this.size
				+"��������Ϊ��"+this.content;
		for(String dim:this.dims.keySet()){
			res=res+dim+":"+this.dims.get(dim)+",";
		}
		return res;
	}
	/*
	 * �ж����������Ƿ���һ�µ�����
	 */
	public boolean isEqual(Data d){
		//����ͨ���������󷵻�true����������һ��
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
	 *�ж�һ�������ϵ�ά��ֵ����һ�����ݵĶ�Ӧά���ϵ�ֵ�Ƿ���һ����Χ�ڿ���Ϊ���
	 */
	public boolean isInRange(String s,double dest1,double dest2){
		if(!this.dims.containsKey(s)) return false;
		//���������ͨ���������Ϊ���
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
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
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
	//�������ʹ�ô���
	public void addUsageCount(){
		this.usageCount++;
	}
}
