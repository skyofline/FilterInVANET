/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import constructions.Data;
import constructions.Filter;
import constructions.FilterCube;
import constructions.Keys;
import constructions.Request;
import constructions.Splits;
import constructions.TrackInfo;
import constructions.Values;
import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.util.RoutingInfo;

import static core.Constants.DEBUG;

/**
 * A DTN capable host.
 */
public class DTNHost implements Comparable<DTNHost> {
	
	private static int nextAddress = 0;
	private int address;

	private Coord location; 	// where is the host
	private Coord destination;	// where is it going

	private MessageRouter router;
	private MovementModel movement;
	private Path path;
	private double speed;
	private double nextTimeToMove;
	private String name;
	private List<MessageListener> msgListeners;
	private List<MovementListener> movListeners;
	private List<NetworkInterface> net;
	private ModuleCommunicationBus comBus;
	
	private static int CAR_TYPE=0;
	private static int RSU_TYPE=1;
	
	//存储从外界文件读取到的轨迹信息
	private List<TrackInfo> moveTracks=new ArrayList<>();
	//存储车辆走到本地存储的track中的第几步，起始值为0
	private int stepOfTrack=0;
	private Map<Data,Integer> datas=new LinkedHashMap<Data,Integer>();
    private List<Filter> filters=new ArrayList<>();
    private Map<Request,Integer> requests=new LinkedHashMap<Request, Integer>();
    //waitMessage存储发送过来的查询消息
    private List<Message> waitMessages=new ArrayList<Message>();
    //存储等待数据的查询消息
    private List<Message> waitDataMessages=new ArrayList<Message>();  
    private int type;//节点类型，包含一般节点（0）和RSU（1）还有cloud节点（2）
    private int time;//用来记录节点更新的次数
    
    //存储filter cube中更新filter各个维度的最新一次时间
    private double oldUpdateTime=0;
   
    /*
     * 计算dimensional split factor的balanced factor,暂时假定为0.5
     */
    private double balanceFactor=0.5;
    /*
     * 评价参数
     */
     private long numOfQuery=0;//用来表示该节点接收到的query个数
     private long numOfRepliedQuery=0;//存储已经被回复的query个数
     private double replyQueryTime=0;//用于回复查询的时间
     private long numOfRepliedImmidia=0;//用于存储能够利用本地数据回复的查询的个数
    /*
     * FilterCube
     */
    private FilterCube filterCube=new FilterCube();
//    //计算获取空间占用率
//    public double getSpaceOccupRate(){
//    	return (this.space-this.restSpace)/this.space;
//    }
 

    
//    //计算获得成功回应的数量
//    public double getSuccReplyNum(){
//    	double all=0;
//    	for(Message m:this.queRepMessages.keySet()){
//    		Message r=this.queRepMessages.get(m);
//    		if(r!=null){
//    			if(r.getProperty("reply")==null||!r.getProperty("reply").equals("No reply"))
//    				all=all+1;
//    		}    	
//    	}
//    	return all;
//    }
//    //计算回应比率
//    public double getReplyRate(){
//    	if(this.getReplyNum()==0) return 0;
//    	else return this.getSuccReplyNum()/this.getReplyNum();
//    }
//    public void showAver(){
//    	for(Message m:this.queRepMessages.keySet()){
//    		if(this.queRepMessages.get(m)!=null){
//    			System.out.println(m.toString()+"等待时间为："+(this.queRepMessages.get(m).getReceiveReplyTime()-m.getSendQueryTime()));
////    			if()>100) System.out.println(m.toString());
//    		}
//    		
//    	}
//    }
    /*
     * 创建原始filter cube,这里暂时使用类别号为1 的类别
     */
    public void createOrginFilterCube(){
    	Filter orginFilter=new Filter(1
    			,this.location,0,0);
    	orginFilter.addDimension("Weather",0,2);
    	orginFilter.addDimension("Time",0,4);
    	orginFilter.addDimension("TrafficCondition",0,2);
    	orginFilter.addDimension("Size",0.2*1024*1024,2*1024*1024);
//    	System.out.println(orginFilter.toString());
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
//	    		System.out.println(split[i]);
    			Map<Keys,Values> newMap=this.filterCube.splitDimension(k, dim, split[i]);
    			if(newMap!=null){
    				addKV.putAll(newMap);
    			}
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
    
    /*
     * 对filter cube中的数据进行更新
     * 更新节点中的存储数据
     */
    public void updateDatas(){
    	this.filterCube.updateDatas();
    }

    

    //模拟采集数据，这里自动生成数据
    public void collectData(){
    	Random r=new Random(System.currentTimeMillis());
    	//int type=r.nextInt(5);
    	int type=1;
        int level=r.nextInt(2);
        String content="null";
    	Data d=new Data(SimClock.getTime(),this.address,type,level,content,this.location);
    	d.fillData();
    	//当数据产生后，可将数据加入到filter cube中
    	this.filterCube.putData(d,this);   
    	this.uploadDataToEdge(d);
    }
	public Data collectDataForRequest(Request r){
		String content=null;
		Data d=new Data(SimClock.getTime(),this.address,r.getType(),r.getLevel(),content,this.location);
		d.fillData();
		return d;
	}
    
    //当RSU中没有可以回复查询的数据时，RSU从周围节点查询获取数据
    private void queryDataForRSU(Message m){
    	 List<DTNHost> destin=MessageCenter.selectNodeForRSU(this);
		 for(DTNHost d:destin){
			Message ret=new Message(this, d, "RSUQuery"+System.currentTimeMillis(), 1024,Message.Pull_Data_Type);
			Request q=(Request) m.getProperty("Query");
			ret.addProperty("Query", q);
			this.createNewMessage(ret);
		 }
    }
    /*
     * 上传数据到云端函数
     */
    public void uploadDataToCloud(Data d){
    	Cloud.getInstance().receiveDataFromEdge(d);
    }
    /*
     * 上传数据到edge
     */
    public void uploadDataToEdge(Data d){
    	List<DTNHost> dtns=SimScenario.getInstance().getHosts();
    	Iterator it=dtns.iterator();
    	while(it.hasNext()){
    		DTNHost dtn=(DTNHost) it.next();
    		if(dtn.getType()==1&&d.getLocation().distance(dtn.getLocation())<MessageCenter.dis){
    			/*
    			 * 创建消息发送数据
    			 */
    			Message m=new Message(this,dtn,"data"+this.getAddress()+SimClock.getTime(),(int)d.getSize(),Message.Data_Transfer_Type);
    			m.addProperty("Data", d);
    			this.createNewMessage(m);
    		}
    	}
    }
    static {
		DTNSim.registerForReset(DTNHost.class.getCanonicalName());
		reset();
	}
	/**
	 * Creates a new DTNHost.
	 * @param msgLs Message listeners
	 * @param movLs Movement listeners
	 * @param groupId GroupID of this host
	 * @param interf List of NetworkInterfaces for the class
	 * @param comBus Module communication bus object
	 * @param mmProto Prototype of the movement model of this host
	 * @param mRouterProto Prototype of the message router of this host
	 */
	public DTNHost(List<MessageListener> msgLs,
			List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf,
			ModuleCommunicationBus comBus,
			MovementModel mmProto, MessageRouter mRouterProto) {
		
		this.comBus = comBus;
		this.location = new Coord(0,0);
		this.address = getNextAddress();
		this.name = groupId+address;
		this.net = new ArrayList<NetworkInterface>();

		for (NetworkInterface i : interf) {
			NetworkInterface ni = i.replicate();
			ni.setHost(this);
			net.add(ni);
		}

		// TODO - think about the names of the interfaces and the nodes
		//this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

		this.msgListeners = msgLs;
		this.movListeners = movLs;

		// create instances by replicating the prototypes
		this.movement = mmProto.replicate();
		this.movement.setComBus(comBus);
		this.movement.setHost(this);
		setRouter(mRouterProto.replicate());

		this.location = movement.getInitialLocation();

		this.nextTimeToMove = movement.nextPathAvailable();
		this.path = null;
		
		//新添加了一个type属性，这里默认设置为0，即普通节点
		this.type=0;
		//新添加了一个time属性，这里起始值为0
		this.time=0;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}
		//初始化创建filtercube
		this.createOrginFilterCube();
		this.splitFilterCubeFirst();
		this.filterCube.showFilterCubeStruct();
		//生成dtnHost时添加轨迹数据
		if(DTNSim.getTracks().size()>0){
			if(DTNSim.getTracks().get(Integer.toString(this.address))!=null){
				for(String s:DTNSim.getTracks().get(Integer.toString(this.address))){
					TrackInfo t=new TrackInfo(s);
					this.moveTracks.add(t);
				}
			}
		}
		
		System.out.println(this.getName()+"已经完全建立");
		
	}
	public DTNHost(List<MessageListener> msgLs,
			List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf,
			ModuleCommunicationBus comBus,
			MovementModel mmProto, MessageRouter mRouterProto,int type) {
		this.comBus = comBus;
		this.location = new Coord(0,0);
		this.address = getNextAddress();
		this.name = groupId+address;
		this.net = new ArrayList<NetworkInterface>();
		
		//添加一个了type属性，创建时在此赋值
		this.setType(type);
		this.time=0;
		for (NetworkInterface i : interf) {
			NetworkInterface ni = i.replicate();
			ni.setHost(this);
			net.add(ni);
		}

		// TODO - think about the names of the interfaces and the nodes
		//this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

		this.msgListeners = msgLs;
		this.movListeners = movLs;

		// create instances by replicating the prototypes
		this.movement = mmProto.replicate();
		this.movement.setComBus(comBus);
		this.movement.setHost(this);
		setRouter(mRouterProto.replicate());

		this.location = movement.getInitialLocation();

		this.nextTimeToMove = movement.nextPathAvailable();
		this.path = null;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}
		//初始化创建filtercube
		this.createOrginFilterCube();
		this.splitFilterCubeFirst();
		this.filterCube.showFilterCubeStruct();
		//生成dtnHost时添加轨迹数据
		if(DTNSim.getTracks().size()>0){
			if(DTNSim.getTracks().get(Integer.toString(this.address))!=null){
				for(String s:DTNSim.getTracks().get(Integer.toString(this.address))){
					TrackInfo t=new TrackInfo(s);
					this.moveTracks.add(t);
				}
			}
		}
		
		
		System.out.println(this.getName()+"已经完全建立");
	}

	/**
	 * Returns a new network interface address and increments the address for
	 * subsequent calls.
	 * @return The next address.
	 */
	private synchronized static int getNextAddress() {
		return nextAddress++;
	}

	/**
	 * Reset the host and its interfaces
	 */
	public static void reset() {
		nextAddress = 0;
	}
	/**
	 * Returns true if this node is actively moving (false if not)
	 * @return true if this node is actively moving (false if not)
	 */
	public boolean isMovementActive() {
		return this.movement.isActive();
	}

	/**
	 * Returns true if this node's radio is active (false if not)
	 * @return true if this node's radio is active (false if not)
	 */
	public boolean isRadioActive() {
		// Radio is active if any of the network interfaces are active.
		for (final NetworkInterface i : this.net) {
			if (i.isActive()) return true;
		}
		return false;
	}

	/**
	 * Set a router for this host
	 * @param router The router to set
	 */
	private void setRouter(MessageRouter router) {
		router.init(this, msgListeners);
		this.router = router;
	}

	/**
	 * Returns the router of this host
	 * @return the router of this host
	 */
	public MessageRouter getRouter() {
		return this.router;
	}

	/**
	 * Returns the network-layer address of this host.
	 */
	public int getAddress() {
		return this.address;
	}

	/**
	 * Returns this hosts's ModuleCommunicationBus
	 * @return this hosts's ModuleCommunicationBus
	 */
	public ModuleCommunicationBus getComBus() {
		return this.comBus;
	}

    /**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * @param con  The connection object whose state changed
	 */
	public void connectionUp(Connection con) {
		this.router.changedConnection(con);
	}

	public void connectionDown(Connection con) {
		this.router.changedConnection(con);
	}

	/**
	 * Returns a copy of the list of connections this host has with other hosts
	 * @return a copy of the list of connections this host has with other hosts
	 */
	public List<Connection> getConnections() {
		List<Connection> lc = new ArrayList<Connection>();

		for (NetworkInterface i : net) {
			lc.addAll(i.getConnections());
		}

		return lc;
	}

	/**
	 * Returns the current location of this host.
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Returns the Path this node is currently traveling or null if no
	 * path is in use at the moment.
	 * @return The path this node is traveling
	 */
	public Path getPath() {
		return this.path;
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
		this.name = name;
	}

	/**
	 * Returns the messages in a collection.
	 * @return Messages in a collection
	 */
	public Collection<Message> getMessageCollection() {
		return this.router.getMessageCollection();
	}

	/**
	 * Returns the number of messages this node is carrying.
	 * @return How many messages the node is carrying currently.
	 */
	public int getNrofMessages() {
		return this.router.getNrofMessages();
	}

	/**
	 * Returns the buffer occupancy percentage. Occupancy is 0 for empty
	 * buffer but can be over 100 if a created message is bigger than buffer
	 * space that could be freed.
	 * @return Buffer occupancy percentage
	 */
	public double getBufferOccupancy() {
		long bSize = router.getBufferSize();
		long freeBuffer = router.getFreeBufferSize();
		return 100*((bSize-freeBuffer)/(bSize * 1.0));
	}

	/**
	 * Returns routing info of this host's router.
	 * @return The routing info.
	 */
	public RoutingInfo getRoutingInfo() {
		return this.router.getRoutingInfo();
	}

	/**
	 * Returns the interface objects of the node
	 */
	public List<NetworkInterface> getInterfaces() {
		return net;
	}

	/**
	 * Find the network interface based on the index
	 */
	public NetworkInterface getInterface(int interfaceNo) {
		NetworkInterface ni = null;
		try {
			ni = net.get(interfaceNo-1);
		} catch (IndexOutOfBoundsException ex) {
			throw new SimError("No such interface: "+interfaceNo +
					" at " + this);
		}
		return ni;
	}

	/**
	 * Find the network interface based on the interfacetype
	 */
	protected NetworkInterface getInterface(String interfacetype) {
		for (NetworkInterface ni : net) {
			if (ni.getInterfaceType().equals(interfacetype)) {
				return ni;
			}
		}
		return null;
	}

	/**
	 * Force a connection event
	 */
	public void forceConnection(DTNHost anotherHost, String interfaceId,
			boolean up) {
		NetworkInterface ni;
		NetworkInterface no;

		if (interfaceId != null) {
			ni = getInterface(interfaceId);
			no = anotherHost.getInterface(interfaceId);

			assert (ni != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
			assert (no != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
		} else {
			ni = getInterface(1);
			no = anotherHost.getInterface(1);

			assert (ni.getInterfaceType().equals(no.getInterfaceType())) :
				"Interface types do not match.  Please specify interface type explicitly";
		}

		if (up) {
			ni.createConnection(no);
		} else {
			ni.destroyConnection(no);
		}
	}

	/**
	 * for tests only --- do not use!!!
	 */
	public void connect(DTNHost h) {
		if (DEBUG) Debug.p("WARNING: using deprecated DTNHost.connect" +
			"(DTNHost) Use DTNHost.forceConnection(DTNHost,null,true) instead");
		forceConnection(h,null,true);
	}

	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	public void update(boolean simulateConnections) {
		if (!isRadioActive()) {
			// Make sure inactive nodes don't have connections
			tearDownAllConnections();
			return;
		}
		this.time=this.time+1;
//		if(this.time>1000000) this.time=this.time-1000000;
		if (simulateConnections) {
			for (NetworkInterface i : net) {
				i.update();
			}
		}
		this.router.update();		
		//如果是车辆节点，则采集数据，按照一定时间
		if(this.time%90==1){
			if(this.type==0) {
				this.collectData();
				
			}
		}	
		 
//		if(this.time%18000==1&&this.time!=1){
//			this.filterCube.updateDatas();
//			this.filterCube.update();
//		}
		
		//判断filter cube中的数据量是否过多，若是，则进行修改删除
		if(this.filterCube.getRestSpace()/this.filterCube.fullSpace<0.12){
			System.out.println(this.name+"的存储空间不足，正在删除以往filter和数据中。。。。。。。。");
			this.filterCube.updateDatas();
		}
		if(this.filterCube.getNumOfData()/this.filterCube.getFC().size()>500){
			System.out.println(this.getName()+"内部正在更新filter cube整体");
			if(SimClock.getTime()-this.oldUpdateTime>1800){
				this.oldUpdateTime=SimClock.getTime();
				this.filterCube.update();
			}
		}
//		if(this.time % 1800==1){//更新filter
//			//throw new SimError("RSU更新数据和filter时间");
//			//更新filter的阈值
//			if(this.type==1){
//				this.filterCube.update();
//			}
//			
//		}
	}

	/**
	 * Tears down all connections for this host.
	 */
	private void tearDownAllConnections() {
		for (NetworkInterface i : net) {
			// Get all connections for the interface
			List<Connection> conns = i.getConnections();
			if (conns.size() == 0) continue;

			// Destroy all connections
			List<NetworkInterface> removeList =
				new ArrayList<NetworkInterface>(conns.size());
			for (Connection con : conns) {
				removeList.add(con.getOtherInterface(i));
			}
			for (NetworkInterface inf : removeList) {
				i.destroyConnection(inf);
			}
		}
	}

	/**
	 * Moves the node towards the next waypoint or waits if it is
	 * not time to move yet
	 * @param timeIncrement How long time the node moves
	 */
	public void move(double timeIncrement) {
		double possibleMovement;
		double distance;
		double dx, dy;

		if (!isMovementActive() || SimClock.getTime() < this.nextTimeToMove) {
			return;
		}
		if (this.destination == null) {
			if (!setNextWaypoint()) {
				return;
			}
		}

		possibleMovement = timeIncrement * speed;
		distance = this.location.distance(this.destination);

		while (possibleMovement >= distance) {
			// node can move past its next destination
			this.location.setLocation(this.destination); // snap to destination
			possibleMovement -= distance;
			if (!setNextWaypoint()) { // get a new waypoint
				return; // no more waypoints left
			}
			distance = this.location.distance(this.destination);
		}
	
		// move towards the point for possibleMovement amount
		dx = (possibleMovement/distance) * (this.destination.getX() -
				this.location.getX());
		dy = (possibleMovement/distance) * (this.destination.getY() -
				this.location.getY());
		this.location.translate(dx, dy);
	}

	/**
	 * Sets the next destination and speed to correspond the next waypoint
	 * on the path.
	 * @return True if there was a next waypoint to set, false if node still
	 * should wait
	 */
	private boolean setNextWaypoint() {
		if (path == null) {
			if(this.stepOfTrack<this.moveTracks.size()){
				//在汽车行驶轨迹之前，先向目的地发送查询消息，查询该地点的数据信息
				this.createRequestMessage(this.moveTracks.get(this.stepOfTrack).getLocation());
				path = movement.getPath(this.location,this.moveTracks.get(stepOfTrack).getLocation());
				this.stepOfTrack++;
			}
			else 
				path=movement.getPath();
		}else if(!path.hasNext()) this.stepOfTrack++;//当一条路线走到头后，stepOfTrack加一
		
		
		if (path == null || !path.hasNext()) {
			this.nextTimeToMove = movement.nextPathAvailable();
			this.path = null;
			return false;
		}

		this.destination = path.getNextWaypoint();
		this.speed = path.getSpeed();
		
//		//如果之前成功依据此设置速度，这里利用当前与目的地距离来设置速度
//		if(this.stepOfTrack-1<this.tracks.size())
//			this.speed=this.destination.distance(this.location)/(this.tracks.get(this.stepOfTrack-1).getTime().getSimTime()-SimClock.getTime());
//			

		if (this.movListeners != null) {
			for (MovementListener l : this.movListeners) {
				l.newDestination(this, this.destination, this.speed);
			}
		}

		return true;
	}

	/**
	 * Sends a message from this host to another host
	 * @param id Identifier of the message
	 * @param to Host the message should be sent to
	 */
	public void sendMessage(String id, DTNHost to) {
		
		this.router.sendMessage(id, to);		
	}

	/**
	 * Start receiving a message from another host
	 * @param m The message
	 * @param from Who the message is from
	 * @return The value returned by
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int receiveMessage(Message m, DTNHost from) {
		
		int retVal = this.router.receiveMessage(m, from);
		if (retVal == MessageRouter.RCV_OK) {
			m.addNodeOnPath(this);	// add this node on the messages path
		}
		
		//判断是否为目的节点并且接收成功
		if(retVal == MessageRouter.RCV_OK && m.getTo()==this){
		
			//判断该消息是否为查询消息
			if(m.getType()==Message.Query_Type) {
				if(this.getType()==1) m.setReceiveQueryTime(SimClock.getTime());
				this.addMessageToWaitMessage(m);
				Request q=(Request) m.getProperty("Query");
				System.out.println("DTNHost中接收消息判断消息类型为查询消息，发送方为："+m.getFrom().name+"，接收方为："+m.getTo().name);
				
				
				//处理消息查询,创建回复消息
					
				/*若节点中存在满足查询条件的数据则回复查询，
				 * 若不，则需判断其是否为RSU节点，若是，则RSU节点要向周围节点发送去query请求相应数据，
				 * 然后再回复查询请求
				 * 
				 * processQuery处理消息，结果若为1则说明成功
				 */
				if(processQuery(m)==1){  
					if(m.getTo().getType()==1){//rsu
						System.out.println(this.name+"已经发出回复（包含数据）以响应来自"+m.getFrom().name+"的查询");
					}
//					else if(m.getTo().getType()==0){//r车辆
//						System.out.println(this.name+"已经成功发出数据给"+m.getFrom().name);
//					}
				}else{
					/*
					 * 如果处理失败，
					 */
					
					if(this.getType()==1) {
//						if(q.getType()==0)this.numOfQuery++;
						//此时为RSU节点，进行操作,将没有回复的message保存到数组中，留待下一次的处理
						System.out.println(this.name+"中没有数据可供"+m.getFrom().name+"查询，将消息存入等待队列中...");
						if(q!=null && !this.waitMessages.contains(m)){
							this.addMessageToWaitDataMessage(m);
						}
					}
					
				}
				
				
			}else if (m.getType()==Message.Reply_Type){ //判断该消息是否为查询回复消息
//				System.out.println("DTNHost中接收消息判断消息类型为回复消息,发送方为："+m.getFrom().name+"，接收方为："+m.getTo().name);
			
				this.processReply(m);
				
				/*
				 * 如果该节点为RSU节点中的查询消息过期，则删除
				 */
				
				if(this.type==1){
					//一个保存将要删除的message列表
					List<Message> removeMess=new ArrayList<>();
					
					for(Message s:this.waitDataMessages){
						if(SimClock.getTime()-m.getReceiveQueryTime()>MessageCenter.exitTime) removeMess.add(s);						
					}
					this.waitDataMessages.removeAll(removeMess);
				}
				
			}else if(m.getType()==Message.Gener_Type) {	
				System.out.println(this.getName()+"接收消息判断消息为一般消息,发送方为："+m.getFrom().name+"，接收方为："+m.getTo().name);
			}else if(m.getType()==Message.Data_Transfer_Type){
				this.processDataTransfer(m);
//				System.out.println(this.getName()+ "接收消息判断消息为数据传送消息");
			}else if(m.getType()==Message.Pull_Data_Type){
				System.out.println(this.getName()+"接收消息处理，消息为拉取数据消息");
				//当接收到拉取数据的消息是车辆时，说明要获取数据并回复
				if(this.getType()==0){
					Message ret=new Message(m.getTo(), m.getFrom(), "Reply"+m.getId(), 1024*100,Message.Pull_Data_Type);
					List<Data> ds=this.filterCube.answerRequest((Request) m.getProperty("Query"),this);
					if(ds.size()>0){
						System.out.println("车辆回复拉取数据消息，共回复数据"+ds.size()+"条");
						for(int i=0;i<ds.size();i++){
							ret.addProperty("Data"+i+System.currentTimeMillis(), ds.get(i));
						}
						this.createNewMessage(ret);
					}else{
						System.out.println("车辆中不包含可回复数据，利用传感器等主动捕捉数据");
						Data d=this.collectDataForRequest((Request) m.getProperty("Query"));
						ret.addProperty("Data0"+System.currentTimeMillis(), d);
						this.createNewMessage(ret);
					}
						
					
				}else if(this.getType()==1){
					//如果该节点是rsu edge节点，说明发起拉起数据的rsu接收到车辆发送过来的相关数据，
					//然后对数据进行处理，将数据传送给cloud端进行处理
					System.out.println(this.getName()+"接收到拉取数据返回消息");
					List<Data> datas=new ArrayList<Data>();
					for(String key:m.getProKeys()){
						if(key.contains("Data")){
							datas.add((Data) m.getProperty(key));
						}
					}
					for(Data d:datas){
						this.filterCube.putData(d,this);
					}
					System.out.println(datas.size()+this.getName()+"内的数据条数为："+this.filterCube.getNumOfData());
					//rsu获取数据后回复至云端处理数据来回复等待数据的消息
					Cloud.getInstance().workOnWaitMessage((List<Data>) datas.get(0));
				}
				
				
			}
		}
			
		return retVal;
	}

	
	/**
	 * Requests for deliverable message from this host to be sent trough a
	 * connection.
	 * @param con The connection to send the messages trough
	 * @return True if this host started a transfer, false if not
	 */
	public boolean requestDeliverableMessages(Connection con) {
		return this.router.requestDeliverableMessages(con);
	}



	/**
	 * Informs the host that a message was successfully transferred.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 */
	public void messageTransferred(String id, DTNHost from) {
		this.router.messageTransferred(id, from);
	}

	/**
	 * Informs the host that a message transfer was aborted.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		this.router.messageAborted(id, from, bytesRemaining);
	}

	/**
	 * Creates a new message to this host's router
	 * @param m The message to create
	 */
	public void createNewMessage(Message m) {
		if(m.getType()==Message.Query_Type){
			m.setReceiveQueryTime(SimClock.getTime());
			//创建一条查询
			Request q=this.createNewRequest();
			q.setTime(SimClock.getTime());
			m.addProperty("Query", q);
			//选择目的节点
			DTNHost news=MessageCenter.getNearestEdge(this);
			if(news!=null) m.setTo(news);
			this.addMessageToWaitMessage(m);
		}
		this.router.createNewMessage(m);
	}
	/*
	 * 向指定目的地生成查询
	 */
	public void createRequestMessage(Coord c){
		DTNHost to=MessageCenter.getNearestEdge(this);
		Message m=new Message(this,to,this.getName()+"Query"+SimClock.getTime(),1024*5,Message.Query_Type);
		m.setReceiveQueryTime(SimClock.getTime());
		//创建一条查询
		Request q=this.createNewRequest();
		q.setTime(SimClock.getTime());
		q.setLocation(c);
		m.addProperty("Query", q);
		
		this.addMessageToWaitMessage(m);
		this.router.createNewMessage(m);
	}

	/**
	 * Deletes a message from this host
	 * @param id Identifier of the message
	 * @param drop True if the message is deleted because of "dropping"
	 * (e.g. buffer is full) or false if it was deleted for some other reason
	 * (e.g. the message got delivered to final destination). This effects the
	 * way the removing is reported to the message listeners.
	 */
	public void deleteMessage(String id, boolean drop) {
		this.router.deleteMessage(id, drop);
	}

	/**
	 * Returns a string presentation of the host.
	 * @return Host's name
	 */
	public String toString() {
		return name;
	}

	/**
	 * Checks if a host is the same as this host by comparing the object
	 * reference
	 * @param otherHost The other host
	 * @return True if the hosts objects are the same object
	 */
	public boolean equals(DTNHost otherHost) {
		return this == otherHost;
	}

	/**
	 * Compares two DTNHosts by their addresses.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(DTNHost h) {
		return this.getAddress() - h.getAddress();
	}
/*
	 //发送查询消息的函数
    public int sendQuery(DTNHost to){
    	int val = 0;
    	Long currTime=System.currentTimeMillis();
    	Request q=createNewRequest();
    	q.setTime(SimClock.getTime());
    	q.addDimensions();
    	Message m=new Message(this,to,"query"+System.currentTimeMillis(),1024);
    	m.addProperty("query", q);
    	this.createNewMessage(m);
    	return 0;
    }
    */
    /*
     * 处理查询函数 ，获取数据并回复消息
     * 方法是：
     * 	  如果接收到查询的节点是路边RSU节点2，首先检查内部filter cube是否有数据可以回复查询
     * ，若可以，直接创建回复消息（包含数据）发回至发起查询的节点1；
     * 若不可以，将查询上传至云端，如果云端能处理，则将数据返回到节点2，由节点2进行处理
     * 如果云端中不含有该查询需要的数据，云端根据查询地点将查询消息发送至相应的rsu，
     * rsu获取数据（如果rsu中不包含数据，则向范围内的车辆请求数据），
     * 并将数据返回至云端、节点2，然后节点2进行回复。
     */
    public int processQuery(Message m){
    	Request q=(Request) m.getProperty("Query");
    	//如果消息中含有查询语句，通过遍历RSU中的data 的list，判断数据data的类型是否与query中的相同，
   		//若是，则加入到返回消息中，最后创建返回消息。
   		if(q!=null){
    		List<Data> datas=this.filterCube.answerRequest(q,this);
    	
    		Message ret=new Message(m.getTo(), m.getFrom(), "Reply"+m.getId(), 1024*100,Message.Reply_Type);
    		double sizes=0;
    		int i=0;
    		for(Data s:datas){
    			//将数据放入回复消息中
    			ret.addProperty("Data"+i+System.currentTimeMillis(), s);
    			sizes=sizes+s.getSize();
    			i++;
    		}
    		ret.setSize((int)sizes);
    		//如果rsu查询的数据可以回复消息，则创建回复消息
    		if(datas.size()>0){
    			System.out.println(this.getName()+"可以回复查询");
    			//如果是RSU节点，则添加回复查询事件
    			if(this.getType()==DTNHost.RSU_TYPE){
    				m.setReceiveReplyTime(SimClock.getTime());
    				this.replyQueryTime+=m.getReceiveReplyTime()-m.getReceiveQueryTime();
    			}
    			//添加成功回复查询数
    			this.numOfRepliedQuery++;
    			this.numOfRepliedImmidia++;
    			this.createNewMessage(ret);
    			return 1;
    		}else if(datas.size()==0){
    			double rsuQueryTime=m.getReceiveQueryTime();
    			//当rsu中的数据无法回应查询时，先将查询交由cloud处理,cloud搜索数据同时将request植入filter cube中
    			/*
    			 * 复制原本查询消息，产生一个副本res，用于存储在云端cloud
    			 */
    			Message mes=m.replicate();
    			mes.setReceiveQueryTime(SimClock.getTime());
    			List<Data> ds=Cloud.getInstance().answerRequest(q);
    			Cloud.getInstance().getFilterCube().putRequest(q);
    			Cloud.getInstance().addNumOfQuery();
    			//如果云端获取相应的数据，则将数据发送至接收查询的节点储存，然后重新处理该查询
    			if(ds.size()>0){
    				for(Data da:ds) this.filterCube.putData(da,this);
    	    		for(Data s:ds){
    	    			//将数据放入回复消息中
    	    			ret.addProperty("Data"+i+System.currentTimeMillis(), s);
    	    			sizes=sizes+s.getSize();
    	    			i++;
    	    		}
    	    		//回复消息中添加消息在rsu和cloud中的传送时间
    	    		mes.setTranTimeRAC(m.getTranTimeRAC()+Cloud.getInstance().getTransferTime(sizes));
    	    		mes.setReceiveReplyTime(SimClock.getTime());
    	    		ret.setSize((int)sizes);
    	    		System.out.println("云端可回应来自"+mes.getTo().getName()+"的查询");
    	    		if(this.getType()==DTNHost.RSU_TYPE){
    	    			this.replyQueryTime+=SimClock.getTime()-rsuQueryTime+mes.getTranTimeRAC();
    	    		}
    	    		Cloud.getInstance().addToRepliedMessage(mes);
    	    		Cloud.getInstance().addNumOfRepliedImmidia();
    	    		Cloud.getInstance().addNumOfRepliedQuery();
    	    		this.createNewMessage(ret);
    	    		return 1;
    			}else{//如果在云端获取不到相应的数据，则从根据edge node中获取数据
    	
    				ds=Cloud.getInstance().getDataFromEdge(q);
    				//如果云端成功获取数据，将数据存入云端，否则，查询失败
    				if(ds.size()>0){
    					for(Data d:ds){
    						Cloud.getInstance().getFilterCube().putData(d,this);
    						this.filterCube.putData(d,this);
    					}
    					for(Data s:ds){
    						//将数据放入回复消息中
    						ret.addProperty("Data"+i+System.currentTimeMillis(), s);
    	    				sizes=sizes+s.getSize();
    	    				i++;
    					}
    					//回复消息中添加消息在rsu和cloud中的传送时间
        	    		mes.setTranTimeRAC(mes.getTranTimeRAC()+Cloud.getInstance().getTransferTime(sizes));
        	    		mes.setReceiveReplyTime(SimClock.getTime());
    					ret.setSize((int)sizes);
    					System.out.println("云端从相应edge node中获取数据回复来自"+mes.getTo().getName()+"的查询");
    					if(this.getType()==DTNHost.RSU_TYPE){
        	    			this.replyQueryTime+=SimClock.getTime()-rsuQueryTime+mes.getTranTimeRAC();
        	    		}
        	    		Cloud.getInstance().addToRepliedMessage(mes);
        	    		Cloud.getInstance().addNumOfRepliedQuery();
        	    		
    					this.createNewMessage(ret);
    					return 1;
    				}else{
    					//云端把查询消息保存起来
    					Cloud.getInstance().addToWaitDataMessage(mes);
    					//如果在edge node中没有数据，则从该edge node从周围车辆拉取数据
    					List<DTNHost> dtns=Cloud.getInstance().getEdgesFromMessage(m);
    					for(DTNHost d:dtns){
    						d.queryDataForRSU(m);
    						
    					}
    					this.addMessageToWaitDataMessage(m);
    					return 0;
    				}
    			}
    		}
    	}
    return 0;
   }
    
    //相应处理回复函数，从回复消息中获取数据并存入节点中的datas
    public void processReply(Message m){
    	System.out.println(this.getName()+"正在接收回复。。。。");
    	Set<String> keys=m.getProKeys();
    	int size=0;
    	for(String s:keys){
    		//判断消息中的属性列表中是否包含数据
    		if(s.contains("Data")) {
    			Data nd=(Data) m.getProperty(s);  
    			//如果消息中包含数据，则存储到RSU的filterCube中
    			List<Message> delMessage=new ArrayList<>();
    			for(Message mes:this.waitMessages){
    				Request r=(Request) mes.getProperty("Query");
    				if(r.judgeData(nd)){
    					this.filterCube.putRequest(r);
    					this.replyQueryTime=this.replyQueryTime+SimClock.getInstance().getTime()-r.getTime()+mes.getTranTimeRAC();
    					this.numOfRepliedQuery++;
    				}
    					
    				delMessage.add(mes);
    			}
    			this.waitMessages.removeAll(delMessage);
    			  				
    			this.filterCube.putData(nd,this);
    			System.out.println("来自"+m.getFrom().name+"发往"+m.getTo().name+
    					"消息类型为"+m.getType()+",data的数据信息为："+m.getProperty(s).toString());
    			size=size+1;
    		}
    	}
    	System.out.println("==================================="+m.getTo().name+"共收到来自"+m.getFrom()
    	.name+"的"+size+"条数据");
    }
    
    /*
     * 响应处理数据传送的函数，从数据传送消息中获取数据并存入节点filter cube相应的data序列中
     * 
     */
    public void processDataTransfer(Message m){
    	for(String s:m.getProKeys()){
    		if(s.contains("Data")){
    			Data d=(Data) m.getProperty(s);
    			this.filterCube.putData(d,this);
    		}
    	}    	
    }
  
	@SuppressWarnings("unchecked")
	public List<Request> getQuery() {
		return (List<Request>) requests.keySet();
	}

	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getName(){
		return name;
	}
	//定时根据时间和查询次数修改数据的阈值
	public void updateDataThresh(int type,Data d,double num){
//		if(type==0) d.setThreshold(d.getThreshold()+num);//设定type为0时代表根据时间修改数据阈值
//		else if(type==1) d.setThreshold(d.getThreshold()+num);//设定type为1时代表根据查询次数修改数据阈值
		double time=(SimClock.getTime()-d.getTime())/60;//时间以分钟为单位，是从数据创建时间到当前时间的差
		int times=this.datas.get(d);
		num=times/time;
		d.setThreshold(d.getThreshold()+num);
	}
	//周期性的更新filter的阈值
	public void updataFilterThresh(){
		List<Filter> delf=new ArrayList();
		List<Filter> addf=new ArrayList();
		for(Filter f:this.filters){
			delf.add(f);
			//根据一段时间内的查询次数修改filter阈值，
			if(f.getNewTimes()-f.getOldTimes()<5) f.setThreshold(f.getThreshold()+0.2);
			else if(f.getNewTimes()-f.getOldTimes()>40) f.setThreshold(f.getThreshold()-0.2);
			f.setOldTimes(f.getNewTimes());
			if(f.getThreshold()<9 && f.getThreshold()>1) addf.add(f);
				
		}
		this.filters.removeAll(delf);
		this.filters.addAll(addf);
	}
	/*
	 * 将一系列数据存入filtercube中
	 */
	public void putDatas(List<Data> ds){
		for(Data d:ds){
			this.filterCube.putData(d,this);
		}
	}
//	//RSU处理来自云端的查询消息
//	public void processQueryFromCenter(Message m){
//				
//			if(m.getType()==Message.Query_Type) {//判断该消息是否为查询事件
//				Request q=(Request) m.getProperty("query");
//				
////				System.out.println("DTNHost中接收消息判断消息类型为查询消息，发送方为："+m.getFrom().name+"，接收方为："+m.getTo().name);
//				//将查询内容存入RSU中
//				if(this.requests.containsKey(q)){
//					int num=this.requests.get(q);
//					this.requests.replace(q, num, num+1);
//				}else{
//					this.requests.put(q, 1);
//				}
////				System.out.println("testing=======================================");
//				//处理消息查询,创建回复消息
//						
//				/*若节点中存在满足查询条件的数据则回复查询，
//				 * 若不，则需判断其是否为RSU节点，若是，则RSU节点要向周围节点发送去query请求相应数据，
//				 * 然后再回复查询请求
//				 */
//				if(processQuery(m)==1){  //成功获取数据，并创建回复消息
////					System.out.println("test2:::::::::::::::::::::::::::::::::::");
//					if(m.getTo().getType()==1){
////						System.out.println(this.name+"已经发出回复（包含数据）以响应来自"+m.getFrom().name+"的查询");
////						this.numOfExitQuery++;
////						m.getTo().numOfQuery++;
//					}
//					else if(m.getTo().getType()==0){
////						System.out.println(this.name+"已经成功发出数据给"+m.getFrom().name);
//					}
//				}
//				else{
//					if(this.getType()==1) {
//						this.numOfQuery++;
//						//此时为RSU节点，进行操作,将没有回复的message保存到数组中，留待下一次的处理
////						System.out.println(this.name+"中没有数据可供"+m.getFrom().name+"查询");
//						if(q!=null && !this.waitMessages.contains(m)){
//							this.waitMessages.add(m);
//						}
////						System.out.println(this.waitMessages.size()+",test3:::::::::::::::::::::::::::::::::::");
//						//rsu向周边节点获取数据
//						this.queryDataForRSU(m);
//					}
//				}
//			}
//		}
	public Long getNumOfQuery(){
		return this.numOfQuery;
	}
	public void setNumOfQuery(int i){
		this.numOfQuery=i;
	}
	public Long getNumOfRepliedQuery(){
		return this.numOfRepliedQuery;
	}
	public void setNumOfRepliedQuery(int i){
		this.numOfRepliedQuery=i;
	}
	//根据已有filter新增新的filter的函数
	public void addFilters(){
		List<Filter> newFilters=new ArrayList<>();
		for(Filter f:this.filters){
			if((f.getNewTimes()-f.getOldTimes())>10||f.getThreshold()<4){
				Filter nf=f.copyFromFilter(f);
				if(!nf.isEqual(f)) newFilters.add(nf);
			}
		}
		this.filters.addAll(newFilters);
	}
//	//根据filter向车辆请求数据
//	public void addTraffData(){
//			List<DTNHost> destin=MessageCenter.selectNodeForRSU(this);
//			 for(DTNHost d:destin){
//				 Message ret=new Message(this, d, "RSUQuery"+System.currentTimeMillis(), 1024,Message.Query_Type);
//				Request q=this.createNewRequest();
//				ret.addProperty("query", q);
//				System.out.println("正在请求数据。。。。。。");
//				this.createNewMessage(ret);
//			 }
//	}
	
	//车辆节点更新存储数据
	public void updateDataForCar(){
		List<Data> del=new ArrayList<>();
		for(Data d:this.datas.keySet()){
			if(SimClock.getTime()-d.getTime()>60*60*50){
				del.add(d);
			}
		}
		for(Data nd:del){
			this.datas.remove(nd);
		}
	}
	//创建Request,(随机生成数据地点，利用云端的edge节点)
	public Request createNewRequest(){
		Random r=new Random(System.currentTimeMillis());
//		int type=r.nextInt(5);
		int type=0;
		int level=r.nextInt(2);
		Coord c=Cloud.getInstance().getRandLocation();
		double time=SimClock.getTime();
		int status=0;
		Request req=new Request(c,time,type,level,status);
		req.addDimensions();
		return req;	
	}
	
	//根据请求生成filter
	public Filter generFilterByRequest(Request r){
		Filter f =new Filter(r.getType(),r.getLocation(),r.getLevel(),0);
		return f;
	}
	//根据数据生成filter
	public Filter generFilterByData(Data d){
		Filter f=new Filter(d.getType(),d.getLocation(),d.getLevel(),0);
		return f;
	}
	/*
	 * rsu当获取到数据后处理之前无法回复的消息
	 */
	public void workOnWaitMessage(Data d){
		List<Message> dels=new ArrayList<Message>();
		for(Message m:this.waitDataMessages){
			Request r=(Request) m.getProperty("Query");
			if(r.judgeData(d)){
//				System.out.println("正在创建回复查询消息。。。。。");
				this.filterCube.putData(d,this);
				Message ret=new Message(m.getTo(), m.getFrom(), "Reply"+m.getId(), 1024*100,Message.Reply_Type);
	    		ret.addProperty("Data"+0+System.currentTimeMillis(), d);
	    		ret.setSize((int)d.getSize());	
	    		this.createNewMessage(ret);
	    		this.numOfRepliedQuery++;
				dels.add(m);
			}
			System.out.println(m.getTo().name+"成功回复了来自"+m.getFrom().name+"的查询******************************");
			this.filterCube.putData(d,this);
			this.filterCube.putRequest(r);
			
		}
		this.waitDataMessages.remove(dels);
	}
	//添加查询消息到消息列表中，同时接收的查询数加一
	public void addMessageToWaitMessage(Message m){
		this.waitMessages.add(m);
		this.numOfQuery++;
	}

	//添加无法立即回复的消息到消息列表中，同时，接收的查询数加一
	public void addMessageToWaitDataMessage(Message m){
		this.waitDataMessages.add(m);
	}


	public double getBalanceFactor() {
		return balanceFactor;
	}
	public void setBalanceFactor(double balanceFactor) {
		this.balanceFactor = balanceFactor;
	}
	public FilterCube getFilterCube() {
		return filterCube;
	}
	public void setFilterCube(FilterCube filterCube) {
		this.filterCube = filterCube;
	}

	public void showEffect(){
		//如果是车辆节点
		if(this.getType()==0)
			System.out.println(this.getName()+"节点的剩余空间比率为："+this.getRestSpaceRate()
				+"，消息查询成功平均时间为："+this.getAverReplyTime()
				+"，消息查询成功率为："+this.getReplyRate()
				+"，成功回复的消息中利用本地数据直接回复的比例"+this.getReplyByLocalRate());
		else if(this.getType()==1){//如果是RSU
			System.out.println(this.getName()+"节点的剩余空间比率为："+this.getRestSpaceRate()
			+"，消息查询成功平均时间为："+this.getAverReplyTime()
			+"，消息查询成功率为："+this.getReplyRate()
			+"，成功回复的消息中利用本地数据直接回复的比例"+this.getReplyByLocalRate());
		}
	}
	/*
	 * 获取空间占用率
	 */
	public double getRestSpaceRate() {
		// TODO Auto-generated method stub
		return this.filterCube.getRestSpace()/this.filterCube.fullSpace;
	}
	/*
	 * 返回消息查询平均时间
	 */
	public double getAverReplyTime() {
		// TODO Auto-generated method stub
		return this.replyQueryTime/this.numOfRepliedQuery;
	}
	
	/*
	 * 返回消息查询成功率
	 */
	public double getReplyRate() {
		// TODO Auto-generated method stub
		if(this.numOfQuery==0) return 0;
		return (double)this.numOfRepliedQuery/this.numOfQuery;
	}
	/*
	 * 返回成功查询回复中利用本地数据直接回复的比例
	 */
	public double getReplyByLocalRate(){
		if(this.numOfRepliedQuery==0) return 0;
		return (double)this.numOfRepliedImmidia/this.numOfRepliedQuery;
	}

	public List<TrackInfo> getMoveTracks() {
		return moveTracks;
	}


	public void setMoveTracks(List<TrackInfo> tracks) {
		this.moveTracks = tracks;
	}


	public long getNumOfRepliedImmidia() {
		return numOfRepliedImmidia;
	}


	public void setNumOfRepliedImmidia(long numOfRepliedImmidia) {
		this.numOfRepliedImmidia = numOfRepliedImmidia;
	}
		
}
