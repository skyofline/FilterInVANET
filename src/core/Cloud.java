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
    private double transferSpeed=1024*1024*10;
    //评判filter作用的参数
    
    private long numOfQuery=0;//用来表示该节点接收到的query个数
    private long numOfRepliedQuery=0;//用来表示发送来的查询节点中已经有可以回复的数据的查询数量
    private double replyQueryTime=0;//用于统计RSU节点回复数据的时间
    private long numOfRepliedImmidia=0;//用于存储能够利用本地数据回复的查询的个数
    
    
    private double space=1024*1024*1024*100;//用来表示节点的存储空间大小
    private double restSpace=1024*1024*4;//用来表示节点的剩余存小
    /*
     * 计算dimensional split factor的balanced factor,暂时假定为0.5
     */
    private double balanceFactor=0.5;
    /*
     * FilterCube
     */
    private FilterCube filterCube=new FilterCube();
    /*
     * Cloud 端初始化函数
     */
    public Cloud(){
    	this.createOrginFilterCube();
    	this.splitFilterCubeFirst();
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
    			dtn.getFilterCube().putData(d,dtn);
    	}
    }

    //计算获取空间占用率
    public double getRestSpaceRate(){
    	return this.filterCube.getRestSpace()/this.filterCube.fullSpace;
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
    	Filter orginFilter=new Filter(1
    			,this.location,0,0);
    	orginFilter.addDimension("Weather",0,2);
    	orginFilter.addDimension("Time",0,4);
    	orginFilter.addDimension("TrafficCondition",0,2);
    	orginFilter.addDimension("Size",0.3*1024*1024,2*1024*1024);
    	this.filterCube.addDimFrameByFilter(orginFilter);
    	//对原始filter cube进行切分以完成Filter Cube的建立过程
    }
    /*
     * 对filter cube进行切分
     */
    public void splitFilterCubeFirst(){
    	//这里先是假设类别为一的数据，这里假设的数据维度为4
    	int len=4;
    	double[] min=new double[len];
    	int[] split=new int[len];
    	for(int i=0;i<len;i++){
    		min[i]=1000000;
    		split[i]=1;
    	}
    	Map<Keys,Values> addKV=new LinkedHashMap<Keys,Values>();
    	Keys orginKey=null;
    	for(Keys k:this.filterCube.getFC().keySet()){
    		orginKey=k;
    		for(int i=0;i<len;i++){
    			String dim=this.filterCube.getDimensions().get(i);
    			for(int j=1;j<=this.filterCube.getMaxSplits(dim);j++){
    				double dimSplitFac=this.getDimSplitFactor(this.filterCube, dim, j);
    				if(dimSplitFac<min[i]){
    					min[i]=dimSplitFac;
    					split[i]=j;
    				}
	    		}
	    			
    			Map<Keys,Values> newMap=this.filterCube.splitDimension(k, dim, split[i]);
    			if(newMap!=null) addKV.putAll(newMap);
    		}
    	}
    	if(orginKey!=null&&addKV!=null){
    		this.filterCube.getFc().remove(orginKey);
    		this.filterCube.getFc().putAll(addKV);
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
    public void updateFilterCube(){
    	
    	this.filterCube.update();
    }
    
    //更新节点中的存储数据
    public void updateDatas(){
//    	System.out.println(this.name+" Updating datas...................");
    	
    	//若filter为空，则不作处理
    	if (this.filters==null){
    		return;
    	}
    	this.filterCube.updateDatas();

    }
    
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
		this.updateFilterCube();
		if(this.getRestSpaceRate()<0.12) this.updateDatas();
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
		this.filterCube.putDataForCloud(d);
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
				datas.addAll(d.getFilterCube().answerRequest(r,this));
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
		System.out.println("Cloud正在处理数据中================");
		for(Data d:ds){
			List<Message> dels=new ArrayList<Message>();
			List<Message> unreplied=new ArrayList<Message>();
			for(Message m:this.waitDataMessages){
				Request r=(Request) m.getProperty("Query");
				if(SimClock.getTime()-r.getTime()>MessageCenter.exitTime){
					unreplied.add(m);
				}else if(r.judgeData(d)){
					this.filterCube.putDataForCloud(d);
					//然后数据传递到edge node，并处理之前无法回复的消息
					m.getTo().workOnWaitMessage(d);
					dels.add(m);
					m.setReceiveReplyTime(SimClock.getTime());
					this.addNumOfRepliedQuery();
				}
				
			}
			this.unrepliedMessage.addAll(unreplied);
			this.repliedMessages.addAll(dels);
			this.waitDataMessages.removeAll(dels);
			this.waitDataMessages.removeAll(unreplied);
		}
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
		List<Data> d=this.filterCube.answerRequest(r,this);
		return this.filterCube.answerRequest(r,this);
	}
	public void setFilterCube(FilterCube f){
		this.filterCube=f;
	}
	public FilterCube getFilterCube(){
		return this.filterCube;
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
