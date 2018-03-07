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
	//�������ƶˣ���������location Ϊnull
	private Coord location=null;
	//�ڵ���
	private String name="Cloud";
	private Map<Data,Integer> datas=new LinkedHashMap<Data,Integer>();
    private List<Filter> filters=new ArrayList<>();
    private Map<Request,Integer> requests=new LinkedHashMap<Request, Integer>();
    //�����洢�ȴ�RSU��ȡ���ݺ���ܻظ�����Ϣ
    private List<Message> waitDataMessages=new ArrayList<>();
    private List<Message> repliedMessages=new ArrayList<>();
    private List<Message> unrepliedMessage=new ArrayList<>();
    private int type=2;//�ڵ����ͣ�����һ��ڵ㣨0����RSU��1������cloud�ڵ㣨2��
    private int time;//������¼�ڵ���µĴ���
    //Cloud�˴������ʣ�ÿ��
    private double transferSpeed=1024*1024*10;
    //����filter���õĲ���
    
    private long numOfQuery=0;//������ʾ�ýڵ���յ���query����
    private long numOfRepliedQuery=0;//������ʾ�������Ĳ�ѯ�ڵ����Ѿ��п��Իظ������ݵĲ�ѯ����
    private double replyQueryTime=0;//����ͳ��RSU�ڵ�ظ����ݵ�ʱ��
    private long numOfRepliedImmidia=0;//���ڴ洢�ܹ����ñ������ݻظ��Ĳ�ѯ�ĸ���
    
    
    private double space=1024*1024*1024*100;//������ʾ�ڵ�Ĵ洢�ռ��С
    private double restSpace=1024*1024*4;//������ʾ�ڵ��ʣ���С
    /*
     * ����dimensional split factor��balanced factor,��ʱ�ٶ�Ϊ0.5
     */
    private double balanceFactor=0.5;
    /*
     * FilterCube
     */
    private FilterCube filterCube=new FilterCube();
    /*
     * Cloud �˳�ʼ������
     */
    public Cloud(){
    	this.createOrginFilterCube();
    	this.splitFilterCubeFirst();
    }
    /*
     * �´����ݵ�edge��
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

    //�����ȡ�ռ�ռ����
    public double getRestSpaceRate(){
    	return this.filterCube.getRestSpace()/this.filterCube.fullSpace;
    }
    //����������
    public double getReplyRate(){
    	if(this.numOfQuery==0) return 0;
    	else return (double)this.numOfRepliedQuery/this.numOfQuery;
    }
    //�����ѯƽ��ռ��ʱ��
    public double getAverReplyTime(){
    	return this.replyQueryTime/this.numOfRepliedQuery;
    }
    //���㱾������������
    public double getReplyByLocalRate(){
    	if(this.numOfRepliedQuery==0) return 0;
    	else return (double)this.numOfRepliedImmidia/this.numOfRepliedQuery;
    }
    /*
     * ����ԭʼfilter cube,������ʱʹ������Ϊ1 �����
     */
    public void createOrginFilterCube(){
    	Filter orginFilter=new Filter(1
    			,this.location,0,0);
    	orginFilter.addDimension("Weather",0,2);
    	orginFilter.addDimension("Time",0,4);
    	orginFilter.addDimension("TrafficCondition",0,2);
    	orginFilter.addDimension("Size",0.3*1024*1024,2*1024*1024);
    	this.filterCube.addDimFrameByFilter(orginFilter);
    	//��ԭʼfilter cube�����з������Filter Cube�Ľ�������
    }
    /*
     * ��filter cube�����з�
     */
    public void splitFilterCubeFirst(){
    	//�������Ǽ������Ϊһ�����ݣ�������������ά��Ϊ4
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
     * ����dimensional split factor
     */
    public double getDimSplitFactor(FilterCube f,String di,int x){
    	double res=(this.getBalanceFactor()*(double)x)/FilterCube.getMaxSplits(di);
    	res=res+(1-this.getBalanceFactor())*f.getSumOfMis(di, x);
    	return res;
    }
	/*
	 * ��filter cube���и���
	 */
    public void updateFilterCube(){
    	
    	this.filterCube.update();
    }
    
    //���½ڵ��еĴ洢����
    public void updateDatas(){
//    	System.out.println(this.name+" Updating datas...................");
    	
    	//��filterΪ�գ���������
    	if (this.filters==null){
    		return;
    	}
    	this.filterCube.updateDatas();

    }
    
    //ɾ���ڵ��д洢�����ݲ���
    public void deleteData(Data noda){
    	this.datas.remove(noda);
    }
    
    //��ڵ����������
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
	 * cloud�ƶ˽��մ�edge��ȡ������
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
	 * Cloud�ƶ˻�ȡ���edge�ص���䣬�����ݴ�������Coord
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
	 * Cloud��ȡ������ѯ�ص��rsu
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
	 * Cloud�ƶ˶��������·ַ�ʱѡ����Ӧ��edge
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
	 * �ƶ���edge�ڵ��ȡ����
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
	 * �ƶ˴洢��ʱ�޷��ظ�����Ϣ
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
	 * �ƶ˵���ȡ�����ݺ���֮ǰ�޷��ظ�������
	 */
	public void workOnWaitMessage(List<Data> ds){
		System.out.println("Cloud���ڴ���������================");
		for(Data d:ds){
			List<Message> dels=new ArrayList<Message>();
			List<Message> unreplied=new ArrayList<Message>();
			for(Message m:this.waitDataMessages){
				Request r=(Request) m.getProperty("Query");
				if(SimClock.getTime()-r.getTime()>MessageCenter.exitTime){
					unreplied.add(m);
				}else if(r.judgeData(d)){
					this.filterCube.putDataForCloud(d);
					//Ȼ�����ݴ��ݵ�edge node��������֮ǰ�޷��ظ�����Ϣ
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
		System.out.println("Cloud��ʣ��ռ����Ϊ��"+this.getRestSpaceRate()
				+"����Ϣ��ѯ�ɹ�ƽ��ʱ��Ϊ��"+this.getAverReplyTime()
				+"����Ϣ��ѯ�ɹ���Ϊ��"+this.getReplyRate()
				+"���ɹ��ظ���Ϣ�дӱ������ݻ�ȡ�ظ��ı���Ϊ��"+this.getReplyByLocalRate());
	}
	//��ȡ���ݴ���ʱ��
	public double getTransferTime(double size){
		return size/this.getTransferSpeed();
	}
	/*
	 * �ظ���ѯ
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
