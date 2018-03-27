package core;

import static core.Constants.DEBUG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import constructions.Data;
import constructions.Filter;
import constructions.FilterCube;
import constructions.Keys;
import constructions.Request;
import constructions.Values;
import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.util.RoutingInfo;

public class Cloud {
	public static final Logger logger=LogManager.getLogger(Cloud.class.getName());
	private static Cloud myCloud=null;
	//由于是云端，这里设置location 为null
	private Coord location=null;
	//节点名
	private String name="Cloud";
	private Map<Data,Integer> datas=new LinkedHashMap<Data,Integer>();
    private List<Filter> filters=new ArrayList<>();
    private Map<Request,Integer> requests=new LinkedHashMap<Request, Integer>();
    //用来存储等待RSU获取数据后才能回复的消息
    private List<Message> waitDataMessages=new ArrayList<>();
    private List<Message> repliedMessages=new ArrayList<>();
    private List<Message> unrepliedMessage=new ArrayList<>();
    private int type=2;//节点类型，包含一般节点（0）和RSU（1）还有cloud节点（2）
    private int time;//用来记录节点更新的次数
    //Cloud端传输速率，每秒
    private double transferSpeed=1024*10;
    //评判filter作用的参数
    
    private long numOfQuery=0;//用来表示该节点接收到的query个数
    private long numOfRepliedQuery=0;//用来表示发送来的查询节点中已经有可以回复的数据的查询数量
    private double replyQueryTime=0;//用于统计RSU节点回复数据的时间
    private long numOfRepliedImmidia=0;//用于存储能够利用本地数据回复的查询的个数
  //存储filter cube中更新filter各个维度的最新一次时间
    private double oldUpdateTime=0;
    
    private double space=2*1024*1024;//用来表示节点的存储空间大小
    private double restSpace=2*1024*1024;//用来表示节点的剩余存小
    /*
     * 计算dimensional split factor的balanced factor,暂时假定为0.5
     */
    private double balanceFactor=0.5;
    /*
     * FilterCube
     */
    private Map<Integer,FilterCube> filterCubes=new LinkedHashMap<Integer,FilterCube>();
    /*
     * Cloud 端初始化函数
     */
    public Cloud(){
    	this.initOriginFilterCube();    	
    }
    public void initOriginFilterCube(){
    	//初始化创建filtercube
    	this.createOrginFilterCube();
    	for(FilterCube f:this.filterCubes.values()){
    		f.initFilterCube();
    	}
    	this.splitFilterCubeFirst();
    	for(Integer types:this.filterCubes.keySet()){
    		this.filterCubes.get(types).setFullSpace(this.space);
    		this.filterCubes.get(types).setRestSpace(this.restSpace);
    		if(types==2||types==3){
    			this.filterCubes.get(types).setFullSpace(0.2*1024*1024);
    			this.filterCubes.get(types).setRestSpace(0.2*1024*1024);
    		}
//    		this.filterCubes.get(types).showFilterCubeStruct();
    	}
    	
    }
    /*
     * 下传数据到edge端
     */
    public void downloadDataToEdge(Data d){
    	List<DTNHost> dtns=SimScenario.getInstance().getHosts();
    	Iterator it=dtns.iterator();
    	while(it.hasNext()){
    		DTNHost dtn=(DTNHost) it.next();
    		if(dtn.getType()==1&&d.getLocation().distance(dtn.getLocation())<MessageCenter.dis)
    			dtn.getFilterCubes().get(d.getType()).putData(d,dtn);
    	}
    }

    //计算获取空间占用率
	public double getRestSpaceRate() {
		// TODO Auto-generated method stub
		double alls=0;
		int nums=0;
		nums=this.filterCubes.keySet().size();
		for(Integer types:this.filterCubes.keySet()){
    		alls=alls+this.filterCubes.get(types).getRestSpace()/this.filterCubes.get(types).fullSpace;
    	}
		return alls/nums;
	}
    //计算满足率
    public double getReplyRate(){
    	if(this.numOfQuery==0) return 0;
    	else return (double)this.numOfRepliedQuery/this.numOfQuery;
    }
    //计算查询平均占用时间
    public double getAverReplyTime(){
    	return this.replyQueryTime/this.numOfRepliedQuery;
    }
    //计算本地数据满足率
    public double getReplyByLocalRate(){
    	if(this.numOfRepliedQuery==0) return 0;
    	else return (double)this.numOfRepliedImmidia/this.numOfRepliedQuery;
    }
    /*
     * 创建原始filter cube,这里暂时使用类别号为1 的类别
     */
    public void createOrginFilterCube(){
    	Filter orginFilter1=new Filter(1
    			,this.location,0,0);
    	orginFilter1.addDimension("Weather",0,2);
    	orginFilter1.addDimension("Time",0,4);
    	orginFilter1.addDimension("TrafficCondition",0,2);
    	orginFilter1.addDimension("Size",0.2*1024,2*1024);
//    	System.out.println(orginFilter.toString());
    	
    	//对原始filter cube进行切分以完成Filter Cube的建立过程
    	Filter orginFilter0=new Filter(1
    			,this.location,0,0);
    	orginFilter0.addDimension("Duration",300,900);
    	orginFilter0.addDimension("Situation",0,1);
    	orginFilter0.addDimension("TrafficCondition",0,2);
    	orginFilter0.addDimension("Size",51.5*300,51.5*900);
    	
    	Filter orginFilter2=new Filter(1
    			,this.location,0,0);
    	orginFilter2.addDimension("VehicleStatus",0,1);
    	orginFilter2.addDimension("VehicleSpeed",0,49);
    	orginFilter2.addDimension("Size",25,125);
    	
    	Filter orginFilter3=new Filter(1
    			,this.location,0,0);
    	orginFilter3.addDimension("SteeringWheelAngle",0,180);
    	orginFilter3.addDimension("GasPedal",0,1);
    	orginFilter3.addDimension("Size",10,25);
    	
    	Filter orginFilter4=new Filter(1
    			,this.location,0,0);
    	orginFilter4.addDimension("NumOfVehicles",0,9);
    	orginFilter4.addDimension("LanePosition",0,2);
    	orginFilter4.addDimension("Size",0.2*1024,2*1024);
    	FilterCube f0=new FilterCube();
    	FilterCube f1=new FilterCube();
    	FilterCube f2=new FilterCube();
    	FilterCube f3=new FilterCube();
    	FilterCube f4=new FilterCube();
    	f0.addDimFrameByFilter(orginFilter0);
    	f1.addDimFrameByFilter(orginFilter1);
    	f2.addDimFrameByFilter(orginFilter2);
    	f3.addDimFrameByFilter(orginFilter3);
    	f4.addDimFrameByFilter(orginFilter4);
    	this.filterCubes.put(0, f0);
    	this.filterCubes.put(1, f1);
    	this.filterCubes.put(2, f2);
    	this.filterCubes.put(3, f3);
    	this.filterCubes.put(4, f4);
    }
    /*
     * 对filter cube进行切分
     */
    public void splitFilterCubeFirst(){
    	for(Integer types:this.filterCubes.keySet()){
    		//这里先是假设类别为一的数据，这里假设的数据维度为4
    		int len=4;
    		if(types==0||types==1) len=4;
    		else if(types==2||types==3||types==4) len=3;
    		double[] min=new double[len];
    		int[] split=new int[len];
    		for(int i=0;i<len;i++){
    			min[i]=1000000;
    			split[i]=1;
    		}
    		Map<Keys,Values> addKV=new LinkedHashMap<Keys,Values>();
    		List<Keys> orginKey=new ArrayList<Keys>();
    		for(Keys k:this.filterCubes.get(types).getFC().keySet()){
    			int befores=addKV.size();
    			for(int i=0;i<len;i++){
    				String dim=this.filterCubes.get(types).getDimensions().get(i);
    				for(int j=1;j<=this.filterCubes.get(types).getMaxSplits(dim);j++){
    					double dimSplitFac=this.getDimSplitFactor(this.filterCubes.get(types), dim, j);
    					if(dimSplitFac<min[i]){
    						min[i]=dimSplitFac;
    						split[i]=j;
    					}
    				}
    				Map<Keys,Values> newMap=this.filterCubes.get(types).splitDimension(k, dim, split[i]);
    				if(newMap!=null&&newMap.size()>1){
    					addKV.putAll(newMap);
    				}
    			}
    			if(addKV.size()>befores) orginKey.add(k);
    		}
    		if(orginKey.size()>0&&addKV.size()>1){
    			for(Keys t:orginKey) this.filterCubes.get(types).getFc().remove(t);
    			this.filterCubes.get(types).getFc().putAll(addKV);
    		}else{
    			if(this.filterCubes.get(types).getFC().keySet().size()==1){
    				Map<Keys,Values> addsNew=new LinkedHashMap<Keys,Values>();
    				Keys news=null;
    				for(Keys k:this.filterCubes.get(types).getFc().keySet()){
    					news=k;
    					for(int i=0;i<len;i++){
    						Map<Keys,Values> newMap=this.filterCubes.get(types).splitDimension(k, this.filterCubes.get(types).getDimensions().get(i), 2);
    						if(newMap.size()>1) addsNew.putAll(newMap);
    					}
    				}
    				if(news!=null&&addsNew.size()>1){
    					this.filterCubes.get(types).getFc().remove(news);
    					this.filterCubes.get(types).getFC().putAll(addsNew);
    				}
    			}
    		}
    	}

    	
    }
    /*
     * 计算dimensional split factor
     */
    public double getDimSplitFactor(FilterCube f,String di,int x){
    	double res=(this.getBalanceFactor()*(double)x)/FilterCube.getMaxSplits(di);
    	res=res+(1-this.getBalanceFactor())*f.getSumOfMis(di, x);
    	return res;
    }
	/*
	 * 对filter cube进行更新
	 */
    public void updateFilterCubes(){
    	for(Integer types:this.filterCubes.keySet()){
        	this.filterCubes.get(types).update();
        }
    }
//    
//    //更新节点中的存储数据
//    public void updateDatas(){
////    	System.out.println(this.name+" Updating datas...................");
//    	for(Integer types:this.filterCubes.keySet()){
//    		if (this.filters==null){//若filter为空，则不作处理
//    			return;
//    		}
//        	this.filterCubes.get(types).updateDatas();
//        }
//    	
//  
//    }
    
    //删除节点中存储的数据操作
    public void deleteData(Data noda){
    	this.datas.remove(noda);
    }
    
    //向节点中添加数据
    public void addData(Data d){
    	int sign=0;
    	for(Data nd:this.datas.keySet()){
    		if(nd.isEqual(d)){
    			int num=this.datas.get(nd);
    			this.datas.replace(nd, num,num+1);
    			sign=1;
    		}
    	}
    	if(sign==0) this.datas.put(d, 0);
    	
    }

    
 
	/**
	 * Returns the current location of this host.
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Sets the Node's location overriding any location set by movement model
	 * @param location The location to set
	 */
	public void setLocation(Coord location) {
		this.location = location.clone();
	}

	/**
	 * Sets the Node's name overriding the default name (groupId + netAddress)
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name =name ;
	}
	
	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	public void update() {
//		for(Integer types:this.filterCubes.keySet()){
//			//判断filter cube中的数据量是否过多，若是，则进行修改删除
//			if((double)this.filterCubes.get(types).getRestSpace()/this.filterCubes.get(types).fullSpace<0.1){
//				this.filterCubes.get(types).updateDatas();
//			}	
//    	}
		if(SimClock.getTime()-this.oldUpdateTime>3600){
				this.oldUpdateTime=SimClock.getTime();
				double beginTime=SimClock.getTime();
				for(Integer types:this.filterCubes.keySet()){
		    		this.filterCubes.get(types).update();
		    	}
				MessageCenter.filterCubeUpdateTime=SimClock.getTime()-beginTime;
				MessageCenter.filterCubeUpdates=MessageCenter.filterCubeUpdates+1;
		}
	}
	public double getBalanceFactor() {
		return balanceFactor;
	}
	public void setBalanceFactor(double balanceFactor) {
		this.balanceFactor = balanceFactor;
	}

	/*
	 * cloud云端接收从edge获取的数据
	 */
	public void receiveDataFromEdge(Data d){
		d.setExpandState(1);
		this.filterCubes.get(d.getType()).putDataForCloud(d);
	}
	public static Cloud getInstance(){
		if(myCloud==null){
			myCloud=new Cloud();
		}
		return myCloud;
	}
	/*
	 * Cloud云端获取随机edge地点分配，并根据此来返回Coord
	 */
	public Coord getRandLocation(){
		Coord r=null;
		List<DTNHost> ld=SimScenario.getInstance()
				.getEdges();
		int length=ld.size();
		Random random=new Random(System.currentTimeMillis());
		int rnum=random.nextInt(length);
		r=ld.get(rnum).getLocation();
		return r;
	}
	/*
	 * Cloud获取包含查询地点的rsu
	 */
	public List<DTNHost> getEdgesFromMessage(Message m){
		List<DTNHost> res=new ArrayList<DTNHost>();
		List<DTNHost> ld=SimScenario.getInstance().getEdges();
		for(DTNHost d:ld){
			Request r=(Request) m.getProperty("Query");
			if(d.getLocation().distance(r.getLocation())
					<SimScenario.getInstance().getMaxHostRange()){
				res.add(d);
			}
		}
		return res;
	}
	/*
	 * Cloud云端对数据向下分发时选择相应的edge
	 */
	public List<DTNHost> searchDataExpandEdges(Data d){
		List<DTNHost> ld=SimScenario.getInstance().getEdges();
		List<DTNHost> res=new ArrayList<DTNHost>();
		for(DTNHost ed:ld){
			if(ed.getLocation().distance(d.getLocation())<MessageCenter.dis)
				res.add(ed);
		}
		return res;
	}
	/*
	 * 云端向edge节点获取数据
	 */
	public List<Data> getDataFromEdge(Request r){
		List<Data> datas=new ArrayList<>();
		List<DTNHost> dtns=SimScenario.getInstance().getEdges();
		for(DTNHost d:dtns){
			if(d.getLocation().distance(r.getLocation())<MessageCenter.dis){
				datas.addAll(d.getFilterCubes().get(r.getType()).answerRequest(r,this));
				if(datas.size()>0) return datas;
			}
		}
		return datas;
	}
	/*
	 * 云端存储暂时无法回复的消息
	 */
	public void addToWaitDataMessage(Message m){
		this.waitDataMessages.add(m);
		this.numOfQuery++;
	}
	public void addToRepliedMessage(Message m){
		this.repliedMessages.add(m);
	}
	public void addToUnRepliedMessage(Message m){
		this.unrepliedMessage.add(m);
	}
	/*
	 * 云端当获取到数据后处理之前无法回复的数据
	 */
	public void workOnWaitMessage(List<Data> ds){
		for(Data d:ds){
			List<Message> dels=new ArrayList<Message>();
			List<Message> unreplied=new ArrayList<Message>();
			for(Message m:this.waitDataMessages){
				Request r=(Request) m.getProperty("Query");
				if(SimClock.getTime()-r.getTime()>MessageCenter.exitTime){
					unreplied.add(m);
				}else if(r.judgeData(d)){
					this.filterCubes.get(d.getType()).putDataForCloud(d);
					//然后数据传递到edge node，并处理之前无法回复的消息
					m.getTo().workOnWaitMessage(d);
					dels.add(m);
					m.setReceiveReplyTime(SimClock.getTime());
					this.addNumOfRepliedQuery();
					//添加计量数据向下传送数量
					MessageCenter.pullDownDatas=MessageCenter.pullDownDatas+1;
				}
				
			}
			this.unrepliedMessage.addAll(unreplied);
			this.repliedMessages.addAll(dels);
			this.waitDataMessages.removeAll(dels);
			this.waitDataMessages.removeAll(unreplied);
		}
	}
	/*
	 * 云端当获取到数据后处理之前无法回复的数据,单个
	 */
	public void workOnWaitMessage(Data d){
		List<Message> dels=new ArrayList<Message>();
		List<Message> unreplied=new ArrayList<Message>();
		for(Message m:this.waitDataMessages){
			Request r=(Request) m.getProperty("Query");
			if(SimClock.getTime()-r.getTime()>MessageCenter.exitTime){
				unreplied.add(m);
			}else if(r.judgeData(d)){
				this.filterCubes.get(d.getType()).putDataForCloud(d);
				//然后数据传递到edge node，并处理之前无法回复的消息
				m.getTo().workOnWaitMessage(d);
				dels.add(m);
				m.setReceiveReplyTime(SimClock.getTime());
				this.addNumOfRepliedQuery();
				//添加计量数据向下传送数量
				MessageCenter.pullDownDatas=MessageCenter.pullDownDatas+1;
			}
			
		}
		this.unrepliedMessage.addAll(unreplied);
		this.repliedMessages.addAll(dels);
		this.waitDataMessages.removeAll(dels);
		this.waitDataMessages.removeAll(unreplied);
	}
	public void showEffect(){
		System.out.println("Cloud的剩余空间比率为："+this.getRestSpaceRate()
				+"，消息查询成功平均时间为："+this.getAverReplyTime()
				+"，消息查询成功率为："+this.getReplyRate()
				+"，成功回复消息中从本地数据获取回复的比例为；"+this.getReplyByLocalRate());
	}
	//获取数据传输时间
	public double getTransferTime(double size){
		return size/this.getTransferSpeed();
	}
	/*
	 * 回复查询
	 */
	public List<Data> answerRequest(Request r){
		List<Data> d=this.filterCubes.get(r.getType()).answerRequest(r,this);
		return d;
	}
	public void setFilterCubes(Map<Integer,FilterCube> f){
		this.filterCubes=f;
	}
	public Map<Integer,FilterCube> getFilterCubes(){
		return this.filterCubes;
	}
	public double getTransferSpeed() {
		return transferSpeed;
	}
	public void setTransferSpeed(double transferSpeed) {
		this.transferSpeed = transferSpeed;
	}
	public List<Message> getRepliedMessages() {
		return repliedMessages;
	}
	public void setRepliedMessages(List<Message> repliedMessages) {
		this.repliedMessages = repliedMessages;
	}
	public List<Message> getUnrepliedMessage() {
		return unrepliedMessage;
	}
	public void setUnrepliedMessage(List<Message> unrepliedMessage) {
		this.unrepliedMessage = unrepliedMessage;
	}
	public long getNumOfQuery(){
		return this.numOfQuery;
	}
	public long getNumOfRepliedQuery(){
		return this.numOfRepliedQuery;
	}
	public void setNumOfQuery(long i){
		this.numOfQuery=i;
	}
	public void setNumOfRepliedQuery(long i){
		this.numOfRepliedQuery=i;
	}
	public long getNumOfRepliedImmidia() {
		return numOfRepliedImmidia;
	}
	public void setNumOfRepliedImmidia(long numOfRepliedImmidia) {
		this.numOfRepliedImmidia = numOfRepliedImmidia;
	}
	public void addNumOfRepliedImmidia() {
		this.numOfRepliedImmidia++;
	}
	public void addNumOfQuery(){
		this.numOfQuery++;
	}
	public void addNumOfRepliedQuery(){
		this.numOfRepliedQuery++;
	}
}
