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
	
	//�洢������ļ���ȡ���Ĺ켣��Ϣ
	private List<TrackInfo> moveTracks=new ArrayList<>();
	//�洢�����ߵ����ش洢��track�еĵڼ�������ʼֵΪ0
	private int stepOfTrack=0;
	private Map<Data,Integer> datas=new LinkedHashMap<Data,Integer>();
    private List<Filter> filters=new ArrayList<>();
    private Map<Request,Integer> requests=new LinkedHashMap<Request, Integer>();
    //waitMessage�洢���͹����Ĳ�ѯ��Ϣ
    private List<Message> waitMessages=new ArrayList<Message>();
    //�洢�ȴ����ݵĲ�ѯ��Ϣ
    private List<Message> waitDataMessages=new ArrayList<Message>();  
    private int type;//�ڵ����ͣ�����һ��ڵ㣨0����RSU��1������cloud�ڵ㣨2��
    private int time;//������¼�ڵ���µĴ���
    
    //�洢filter cube�и���filter����ά�ȵ�����һ��ʱ��
    private double oldUpdateTime=0;
   
    /*
     * ����dimensional split factor��balanced factor,��ʱ�ٶ�Ϊ0.5
     */
    private double balanceFactor=0.5;
    /*
     * ���۲���
     */
     private long numOfQuery=0;//������ʾ�ýڵ���յ���query����
     private long numOfRepliedQuery=0;//�洢�Ѿ����ظ���query����
     private double replyQueryTime=0;//���ڻظ���ѯ��ʱ��
     private long numOfRepliedImmidia=0;//���ڴ洢�ܹ����ñ������ݻظ��Ĳ�ѯ�ĸ���
    /*
     * FilterCube
     */
    private FilterCube filterCube=new FilterCube();
//    //�����ȡ�ռ�ռ����
//    public double getSpaceOccupRate(){
//    	return (this.space-this.restSpace)/this.space;
//    }
 

    
//    //�����óɹ���Ӧ������
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
//    //�����Ӧ����
//    public double getReplyRate(){
//    	if(this.getReplyNum()==0) return 0;
//    	else return this.getSuccReplyNum()/this.getReplyNum();
//    }
//    public void showAver(){
//    	for(Message m:this.queRepMessages.keySet()){
//    		if(this.queRepMessages.get(m)!=null){
//    			System.out.println(m.toString()+"�ȴ�ʱ��Ϊ��"+(this.queRepMessages.get(m).getReceiveReplyTime()-m.getSendQueryTime()));
////    			if()>100) System.out.println(m.toString());
//    		}
//    		
//    	}
//    }
    /*
     * ����ԭʼfilter cube,������ʱʹ������Ϊ1 �����
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
    
    /*
     * ��filter cube�е����ݽ��и���
     * ���½ڵ��еĴ洢����
     */
    public void updateDatas(){
    	this.filterCube.updateDatas();
    }

    

    //ģ��ɼ����ݣ������Զ���������
    public void collectData(){
    	Random r=new Random(System.currentTimeMillis());
    	//int type=r.nextInt(5);
    	int type=1;
        int level=r.nextInt(2);
        String content="null";
    	Data d=new Data(SimClock.getTime(),this.address,type,level,content,this.location);
    	d.fillData();
    	//�����ݲ����󣬿ɽ����ݼ��뵽filter cube��
    	this.filterCube.putData(d,this);   
    }
	public Data collectDataForRequest(Request r){
		String content=null;
		Data d=new Data(SimClock.getTime(),this.address,r.getType(),r.getLevel(),content,this.location);
		d.fillData();
		return d;
	}
    
    //��RSU��û�п��Իظ���ѯ������ʱ��RSU����Χ�ڵ��ѯ��ȡ����
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
     * �ϴ����ݵ��ƶ˺���
     */
    public void uploadDataToCloud(Data d){
    	Cloud.getInstance().receiveDataFromEdge(d);
    }
    /*
     * �ϴ����ݵ�edge
     */
    public void uploadDataToEdge(Data d){
    	List<DTNHost> dtns=SimScenario.getInstance().getHosts();
    	Iterator it=dtns.iterator();
    	while(it.hasNext()){
    		DTNHost dtn=(DTNHost) it.next();
    		if(dtn.getType()==1&&d.getLocation().distance(dtn.getLocation())<MessageCenter.dis){
    			/*
    			 * ������Ϣ��������
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
		
		//��������һ��type���ԣ�����Ĭ������Ϊ0������ͨ�ڵ�
		this.type=0;
		//��������һ��time���ԣ�������ʼֵΪ0
		this.time=0;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}
		//��ʼ������filtercube
		this.createOrginFilterCube();
		this.splitFilterCubeFirst();
		this.filterCube.showFilterCubeStruct();
		//����dtnHostʱ���ӹ켣����
		if(DTNSim.getTracks().size()>0){
			if(DTNSim.getTracks().get(Integer.toString(this.address))!=null){
				for(String s:DTNSim.getTracks().get(Integer.toString(this.address))){
					TrackInfo t=new TrackInfo(s);
					this.moveTracks.add(t);
				}
			}
		}
		
		System.out.println(this.getName()+"�Ѿ���ȫ����");
		
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
		
		//����һ����type���ԣ�����ʱ�ڴ˸�ֵ
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
		//��ʼ������filtercube
		this.createOrginFilterCube();
		this.splitFilterCubeFirst();
		this.filterCube.showFilterCubeStruct();
		//����dtnHostʱ���ӹ켣����
		if(DTNSim.getTracks().size()>0){
			if(DTNSim.getTracks().get(Integer.toString(this.address))!=null){
				for(String s:DTNSim.getTracks().get(Integer.toString(this.address))){
					TrackInfo t=new TrackInfo(s);
					this.moveTracks.add(t);
				}
			}
		}
		
		
		System.out.println(this.getName()+"�Ѿ���ȫ����");
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
		//����ǳ����ڵ㣬��ɼ����ݣ�����һ��ʱ��
		if(this.time%90==1){
			if(this.type==0) {
				this.collectData();
				
			}
		}	
		 
//		if(this.time%18000==1&&this.time!=1){
//			this.filterCube.updateDatas();
//			this.filterCube.update();
//		}
		
		//�ж�filter cube�е��������Ƿ���࣬���ǣ�������޸�ɾ��
		if(this.filterCube.getRestSpace()/this.filterCube.fullSpace<0.12){
			System.out.println(this.name+"�Ĵ洢�ռ䲻�㣬����ɾ������filter�������С���������������");
			this.filterCube.updateDatas();
		}
		if(this.filterCube.getNumOfData()/this.filterCube.getFC().size()>500){
			System.out.println(this.getName()+"�ڲ����ڸ���filter cube����");
			if(SimClock.getTime()-this.oldUpdateTime>1800){
				this.oldUpdateTime=SimClock.getTime();
				this.filterCube.update();
			}
		}
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
				//��������ʻ�켣֮ǰ������Ŀ�ĵط��Ͳ�ѯ��Ϣ����ѯ�õص��������Ϣ
				this.createRequestMessage(this.moveTracks.get(this.stepOfTrack).getLocation());
				path = movement.getPath(this.location,this.moveTracks.get(stepOfTrack).getLocation());
				this.stepOfTrack++;
			}
			else 
				path=movement.getPath();
		}else if(!path.hasNext()) this.stepOfTrack++;//��һ��·���ߵ�ͷ��stepOfTrack��һ
		
		
		if (path == null || !path.hasNext()) {
			this.nextTimeToMove = movement.nextPathAvailable();
			this.path = null;
			return false;
		}

		this.destination = path.getNextWaypoint();
		this.speed = path.getSpeed();
		
//		//���֮ǰ�ɹ����ݴ������ٶȣ��������õ�ǰ��Ŀ�ĵؾ����������ٶ�
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
		
		//�ж��Ƿ�ΪĿ�Ľڵ㲢�ҽ��ճɹ�
		if(retVal == MessageRouter.RCV_OK && m.getTo()==this){
		
			//�жϸ���Ϣ�Ƿ�Ϊ��ѯ��Ϣ
			if(m.getType()==Message.Query_Type) {
				if(this.getType()==1) m.setReceiveQueryTime(SimClock.getTime());
				this.addMessageToWaitMessage(m);
				Request q=(Request) m.getProperty("Query");
				System.out.println("DTNHost�н�����Ϣ�ж���Ϣ����Ϊ��ѯ��Ϣ�����ͷ�Ϊ��"+m.getFrom().name+"�����շ�Ϊ��"+m.getTo().name);
				
				
				//������Ϣ��ѯ,�����ظ���Ϣ
					
				/*���ڵ��д��������ѯ������������ظ���ѯ��
				 * �����������ж����Ƿ�ΪRSU�ڵ㣬���ǣ���RSU�ڵ�Ҫ����Χ�ڵ㷢��ȥquery������Ӧ���ݣ�
				 * Ȼ���ٻظ���ѯ����
				 * 
				 * processQuery������Ϣ�������Ϊ1��˵���ɹ�
				 */
				if(processQuery(m)==1){  
					if(m.getTo().getType()==1){//rsu
						System.out.println(this.name+"�Ѿ������ظ����������ݣ�����Ӧ����"+m.getFrom().name+"�Ĳ�ѯ");
					}
//					else if(m.getTo().getType()==0){//r����
//						System.out.println(this.name+"�Ѿ��ɹ��������ݸ�"+m.getFrom().name);
//					}
				}else{
					/*
					 * �������ʧ�ܣ�
					 */
					
					if(this.getType()==1) {
//						if(q.getType()==0)this.numOfQuery++;
						//��ʱΪRSU�ڵ㣬���в���,��û�лظ���message���浽�����У�������һ�εĴ���
						System.out.println(this.name+"��û�����ݿɹ�"+m.getFrom().name+"��ѯ������Ϣ����ȴ�������...");
						if(q!=null && !this.waitMessages.contains(m)){
							this.addMessageToWaitDataMessage(m);
						}
					}
					
				}
				
				
			}else if (m.getType()==Message.Reply_Type){ //�жϸ���Ϣ�Ƿ�Ϊ��ѯ�ظ���Ϣ
//				System.out.println("DTNHost�н�����Ϣ�ж���Ϣ����Ϊ�ظ���Ϣ,���ͷ�Ϊ��"+m.getFrom().name+"�����շ�Ϊ��"+m.getTo().name);
			
				this.processReply(m);
				
				/*
				 * ����ýڵ�ΪRSU�ڵ��еĲ�ѯ��Ϣ���ڣ���ɾ��
				 */
				
				if(this.type==1){
					//һ�����潫Ҫɾ����message�б�
					List<Message> removeMess=new ArrayList<>();
					
					for(Message s:this.waitDataMessages){
						if(SimClock.getTime()-m.getReceiveQueryTime()>MessageCenter.exitTime) removeMess.add(s);						
					}
					this.waitDataMessages.removeAll(removeMess);
				}
				
			}else if(m.getType()==Message.Gener_Type) {	
				System.out.println(this.getName()+"������Ϣ�ж���ϢΪһ����Ϣ,���ͷ�Ϊ��"+m.getFrom().name+"�����շ�Ϊ��"+m.getTo().name);
			}else if(m.getType()==Message.Data_Transfer_Type){
				this.processDataTransfer(m);
//				System.out.println(this.getName()+ "������Ϣ�ж���ϢΪ���ݴ�����Ϣ");
			}else if(m.getType()==Message.Pull_Data_Type){
				System.out.println(this.getName()+"������Ϣ��������ϢΪ��ȡ������Ϣ");
				//�����յ���ȡ���ݵ���Ϣ�ǳ���ʱ��˵��Ҫ��ȡ���ݲ��ظ�
				if(this.getType()==0){
					Message ret=new Message(m.getTo(), m.getFrom(), "Reply"+m.getId(), 1024*100,Message.Pull_Data_Type);
					List<Data> ds=this.filterCube.answerRequest((Request) m.getProperty("Query"),this);
					if(ds.size()>0){
						System.out.println("�����ظ���ȡ������Ϣ�����ظ�����"+ds.size()+"��");
						for(int i=0;i<ds.size();i++){
							ret.addProperty("Data"+i+System.currentTimeMillis(), ds.get(i));
						}
						this.createNewMessage(ret);
					}else{
						System.out.println("�����в������ɻظ����ݣ����ô�������������׽����");
						Data d=this.collectDataForRequest((Request) m.getProperty("Query"));
						ret.addProperty("Data0"+System.currentTimeMillis(), d);
						this.createNewMessage(ret);
					}
						
					
				}else if(this.getType()==1){
					//����ýڵ���rsu edge�ڵ㣬˵�������������ݵ�rsu���յ��������͹�����������ݣ�
					//Ȼ������ݽ��д����������ݴ��͸�cloud�˽��д���
					System.out.println(this.getName()+"���յ���ȡ���ݷ�����Ϣ");
					List<Data> datas=new ArrayList<Data>();
					for(String key:m.getProKeys()){
						if(key.contains("Data")){
							datas.add((Data) m.getProperty(key));
						}
					}
					for(Data d:datas){
						this.filterCube.putData(d,this);
					}
					System.out.println(datas.size()+this.getName()+"�ڵ���������Ϊ��"+this.filterCube.getNumOfData());
					//rsu��ȡ���ݺ�ظ����ƶ˴����������ظ��ȴ����ݵ���Ϣ
					Cloud.getInstance().workOnWaitMessage(datas);
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
			//����һ����ѯ
			Request q=this.createNewRequest();
			q.setTime(SimClock.getTime());
			m.addProperty("Query", q);
			//ѡ��Ŀ�Ľڵ�
			DTNHost news=MessageCenter.getNearestEdge(this);
			if(news!=null) m.setTo(news);
			this.addMessageToWaitMessage(m);
		}
		this.router.createNewMessage(m);
	}
	/*
	 * ��ָ��Ŀ�ĵ����ɲ�ѯ
	 */
	public void createRequestMessage(Coord c){
		DTNHost to=MessageCenter.getNearestEdge(this);
		Message m=new Message(this,to,this.getName()+"Query"+SimClock.getTime(),1024*5,Message.Query_Type);
		m.setReceiveQueryTime(SimClock.getTime());
		//����һ����ѯ
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
	 //���Ͳ�ѯ��Ϣ�ĺ���
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
     * ������ѯ���� ����ȡ���ݲ��ظ���Ϣ
     * �����ǣ�
     * 	  ������յ���ѯ�Ľڵ���·��RSU�ڵ�2�����ȼ���ڲ�filter cube�Ƿ������ݿ��Իظ���ѯ
     * �������ԣ�ֱ�Ӵ����ظ���Ϣ���������ݣ������������ѯ�Ľڵ�1��
     * �������ԣ�����ѯ�ϴ����ƶˣ�����ƶ��ܴ����������ݷ��ص��ڵ�2���ɽڵ�2���д���
     * ����ƶ��в����иò�ѯ��Ҫ�����ݣ��ƶ˸��ݲ�ѯ�ص㽫��ѯ��Ϣ��������Ӧ��rsu��
     * rsu��ȡ���ݣ����rsu�в��������ݣ�����Χ�ڵĳ����������ݣ���
     * �������ݷ������ƶˡ��ڵ�2��Ȼ��ڵ�2���лظ���
     */
    public int processQuery(Message m){
    	Request q=(Request) m.getProperty("Query");
    	//�����Ϣ�к��в�ѯ��䣬ͨ������RSU�е�data ��list���ж�����data�������Ƿ���query�е���ͬ��
   		//���ǣ�����뵽������Ϣ�У���󴴽�������Ϣ��
   		if(q!=null){
    		List<Data> datas=this.filterCube.answerRequest(q,this);
    	
    		Message ret=new Message(m.getTo(), m.getFrom(), "Reply"+m.getId(), 1024*100,Message.Reply_Type);
    		double sizes=0;
    		int i=0;
    		for(Data s:datas){
    			//�����ݷ���ظ���Ϣ��
    			ret.addProperty("Data"+i+System.currentTimeMillis(), s);
    			sizes=sizes+s.getSize();
    			i++;
    		}
    		ret.setSize((int)sizes);
    		//���rsu��ѯ�����ݿ��Իظ���Ϣ���򴴽��ظ���Ϣ
    		if(datas.size()>0){
    			System.out.println(this.getName()+"���Իظ���ѯ");
    			//�����RSU�ڵ㣬�����ӻظ���ѯ�¼�
    			if(this.getType()==DTNHost.RSU_TYPE){
    				m.setReceiveReplyTime(SimClock.getTime());
    				this.replyQueryTime+=m.getReceiveReplyTime()-m.getReceiveQueryTime();
    			}
    			//���ӳɹ��ظ���ѯ��
    			this.numOfRepliedQuery++;
    			this.numOfRepliedImmidia++;
    			this.createNewMessage(ret);
    			return 1;
    		}else if(datas.size()==0){
    			double rsuQueryTime=m.getReceiveQueryTime();
    			//��rsu�е������޷���Ӧ��ѯʱ���Ƚ���ѯ����cloud����,cloud��������ͬʱ��requestֲ��filter cube��
    			/*
    			 * ����ԭ����ѯ��Ϣ������һ������res�����ڴ洢���ƶ�cloud
    			 */
    			Message mes=m.replicate();
    			mes.setReceiveQueryTime(SimClock.getTime());
    			List<Data> ds=Cloud.getInstance().answerRequest(q);
    			Cloud.getInstance().getFilterCube().putRequest(q);
    			Cloud.getInstance().addNumOfQuery();
    			//����ƶ˻�ȡ��Ӧ�����ݣ������ݷ��������ղ�ѯ�Ľڵ㴢�棬Ȼ�����´����ò�ѯ
    			if(ds.size()>0){
    				for(Data da:ds) this.filterCube.putData(da,this);
    	    		for(Data s:ds){
    	    			//�����ݷ���ظ���Ϣ��
    	    			ret.addProperty("Data"+i+System.currentTimeMillis(), s);
    	    			sizes=sizes+s.getSize();
    	    			i++;
    	    		}
    	    		//�ظ���Ϣ��������Ϣ��rsu��cloud�еĴ���ʱ��
    	    		mes.setTranTimeRAC(m.getTranTimeRAC()+Cloud.getInstance().getTransferTime(sizes));
    	    		mes.setReceiveReplyTime(SimClock.getTime());
    	    		ret.setSize((int)sizes);
    	    		System.out.println("�ƶ˿ɻ�Ӧ����"+mes.getTo().getName()+"�Ĳ�ѯ");
    	    		if(this.getType()==DTNHost.RSU_TYPE){
    	    			this.replyQueryTime+=SimClock.getTime()-rsuQueryTime+mes.getTranTimeRAC();
    	    		}
    	    		Cloud.getInstance().addToRepliedMessage(mes);
    	    		Cloud.getInstance().addNumOfRepliedImmidia();
    	    		Cloud.getInstance().addNumOfRepliedQuery();
    	    		this.createNewMessage(ret);
    	    		return 1;
    			}else{//������ƶ˻�ȡ������Ӧ�����ݣ���Ӹ���edge node�л�ȡ����
    	
    				ds=Cloud.getInstance().getDataFromEdge(q);
    				//����ƶ˳ɹ���ȡ���ݣ������ݴ����ƶˣ����򣬲�ѯʧ��
    				if(ds.size()>0){
    					for(Data d:ds){
    						Cloud.getInstance().getFilterCube().putData(d,this);
    						this.filterCube.putData(d,this);
    					}
    					for(Data s:ds){
    						//�����ݷ���ظ���Ϣ��
    						ret.addProperty("Data"+i+System.currentTimeMillis(), s);
    	    				sizes=sizes+s.getSize();
    	    				i++;
    					}
    					//�ظ���Ϣ��������Ϣ��rsu��cloud�еĴ���ʱ��
        	    		mes.setTranTimeRAC(mes.getTranTimeRAC()+Cloud.getInstance().getTransferTime(sizes));
        	    		mes.setReceiveReplyTime(SimClock.getTime());
    					ret.setSize((int)sizes);
    					System.out.println("�ƶ˴���Ӧedge node�л�ȡ���ݻظ�����"+mes.getTo().getName()+"�Ĳ�ѯ");
    					if(this.getType()==DTNHost.RSU_TYPE){
        	    			this.replyQueryTime+=SimClock.getTime()-rsuQueryTime+mes.getTranTimeRAC();
        	    		}
        	    		Cloud.getInstance().addToRepliedMessage(mes);
        	    		Cloud.getInstance().addNumOfRepliedQuery();
        	    		
    					this.createNewMessage(ret);
    					return 1;
    				}else{
    					//�ƶ˰Ѳ�ѯ��Ϣ��������
    					Cloud.getInstance().addToWaitDataMessage(mes);
    					//�����edge node��û�����ݣ���Ӹ�edge node����Χ������ȡ����
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
    
    //��Ӧ�����ظ��������ӻظ���Ϣ�л�ȡ���ݲ�����ڵ��е�datas
    public void processReply(Message m){
    	System.out.println(this.getName()+"���ڽ��ջظ���������");
    	Set<String> keys=m.getProKeys();
    	int size=0;
    	for(String s:keys){
    		//�ж���Ϣ�е������б����Ƿ��������
    		if(s.contains("Data")) {
    			Data nd=(Data) m.getProperty(s);  
    			//�����Ϣ�а������ݣ���洢��RSU��filterCube��
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
    			System.out.println("����"+m.getFrom().name+"����"+m.getTo().name+
    					"��Ϣ����Ϊ"+m.getType()+",data��������ϢΪ��"+m.getProperty(s).toString());
    			size=size+1;
    		}
    	}
    	System.out.println("==================================="+m.getTo().name+"���յ�����"+m.getFrom()
    	.name+"��"+size+"������");
    }
    
    /*
     * ��Ӧ�������ݴ��͵ĺ����������ݴ�����Ϣ�л�ȡ���ݲ�����ڵ�filter cube��Ӧ��data������
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
	//��ʱ����ʱ��Ͳ�ѯ�����޸����ݵ���ֵ
	public void updateDataThresh(int type,Data d,double num){
//		if(type==0) d.setThreshold(d.getThreshold()+num);//�趨typeΪ0ʱ��������ʱ���޸�������ֵ
//		else if(type==1) d.setThreshold(d.getThreshold()+num);//�趨typeΪ1ʱ�������ݲ�ѯ�����޸�������ֵ
		double time=(SimClock.getTime()-d.getTime())/60;//ʱ���Է���Ϊ��λ���Ǵ����ݴ���ʱ�䵽��ǰʱ��Ĳ�
		int times=this.datas.get(d);
		num=times/time;
		d.setThreshold(d.getThreshold()+num);
	}
	//�����Եĸ���filter����ֵ
	public void updataFilterThresh(){
		List<Filter> delf=new ArrayList();
		List<Filter> addf=new ArrayList();
		for(Filter f:this.filters){
			delf.add(f);
			//����һ��ʱ���ڵĲ�ѯ�����޸�filter��ֵ��
			if(f.getNewTimes()-f.getOldTimes()<5) f.setThreshold(f.getThreshold()+0.2);
			else if(f.getNewTimes()-f.getOldTimes()>40) f.setThreshold(f.getThreshold()-0.2);
			f.setOldTimes(f.getNewTimes());
			if(f.getThreshold()<9 && f.getThreshold()>1) addf.add(f);
				
		}
		this.filters.removeAll(delf);
		this.filters.addAll(addf);
	}
	/*
	 * ��һϵ�����ݴ���filtercube��
	 */
	public void putDatas(List<Data> ds){
		for(Data d:ds){
			this.filterCube.putData(d,this);
		}
	}
//	//RSU���������ƶ˵Ĳ�ѯ��Ϣ
//	public void processQueryFromCenter(Message m){
//				
//			if(m.getType()==Message.Query_Type) {//�жϸ���Ϣ�Ƿ�Ϊ��ѯ�¼�
//				Request q=(Request) m.getProperty("query");
//				
////				System.out.println("DTNHost�н�����Ϣ�ж���Ϣ����Ϊ��ѯ��Ϣ�����ͷ�Ϊ��"+m.getFrom().name+"�����շ�Ϊ��"+m.getTo().name);
//				//����ѯ���ݴ���RSU��
//				if(this.requests.containsKey(q)){
//					int num=this.requests.get(q);
//					this.requests.replace(q, num, num+1);
//				}else{
//					this.requests.put(q, 1);
//				}
////				System.out.println("testing=======================================");
//				//������Ϣ��ѯ,�����ظ���Ϣ
//						
//				/*���ڵ��д��������ѯ������������ظ���ѯ��
//				 * �����������ж����Ƿ�ΪRSU�ڵ㣬���ǣ���RSU�ڵ�Ҫ����Χ�ڵ㷢��ȥquery������Ӧ���ݣ�
//				 * Ȼ���ٻظ���ѯ����
//				 */
//				if(processQuery(m)==1){  //�ɹ���ȡ���ݣ��������ظ���Ϣ
////					System.out.println("test2:::::::::::::::::::::::::::::::::::");
//					if(m.getTo().getType()==1){
////						System.out.println(this.name+"�Ѿ������ظ����������ݣ�����Ӧ����"+m.getFrom().name+"�Ĳ�ѯ");
////						this.numOfExitQuery++;
////						m.getTo().numOfQuery++;
//					}
//					else if(m.getTo().getType()==0){
////						System.out.println(this.name+"�Ѿ��ɹ��������ݸ�"+m.getFrom().name);
//					}
//				}
//				else{
//					if(this.getType()==1) {
//						this.numOfQuery++;
//						//��ʱΪRSU�ڵ㣬���в���,��û�лظ���message���浽�����У�������һ�εĴ���
////						System.out.println(this.name+"��û�����ݿɹ�"+m.getFrom().name+"��ѯ");
//						if(q!=null && !this.waitMessages.contains(m)){
//							this.waitMessages.add(m);
//						}
////						System.out.println(this.waitMessages.size()+",test3:::::::::::::::::::::::::::::::::::");
//						//rsu���ܱ߽ڵ��ȡ����
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
	//��������filter�����µ�filter�ĺ���
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
//	//����filter������������
//	public void addTraffData(){
//			List<DTNHost> destin=MessageCenter.selectNodeForRSU(this);
//			 for(DTNHost d:destin){
//				 Message ret=new Message(this, d, "RSUQuery"+System.currentTimeMillis(), 1024,Message.Query_Type);
//				Request q=this.createNewRequest();
//				ret.addProperty("query", q);
//				System.out.println("�����������ݡ�����������");
//				this.createNewMessage(ret);
//			 }
//	}
	
	//�����ڵ���´洢����
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
	//����Request,(����������ݵص㣬�����ƶ˵�edge�ڵ�)
	public Request createNewRequest(){
		Random r=new Random(System.currentTimeMillis());
//		int type=r.nextInt(5);
		int type=1;
		int level=r.nextInt(2);
		Coord c=Cloud.getInstance().getRandLocation();
		double time=SimClock.getTime();
		int status=0;
		Request req=new Request(c,time,type,level,status);
		req.addDimensions();
		return req;	
	}
	
	//������������filter
	public Filter generFilterByRequest(Request r){
		Filter f =new Filter(r.getType(),r.getLocation(),r.getLevel(),0);
		return f;
	}
	//������������filter
	public Filter generFilterByData(Data d){
		Filter f=new Filter(d.getType(),d.getLocation(),d.getLevel(),0);
		return f;
	}
	/*
	 * rsu����ȡ�����ݺ���֮ǰ�޷��ظ�����Ϣ
	 */
	public void workOnWaitMessage(Data d){
		List<Message> dels=new ArrayList<Message>();
		for(Message m:this.waitDataMessages){
			Request r=(Request) m.getProperty("Query");
			if(r.judgeData(d)){
//				System.out.println("���ڴ����ظ���ѯ��Ϣ����������");
				this.filterCube.putData(d,this);
				Message ret=new Message(m.getTo(), m.getFrom(), "Reply"+m.getId(), 1024*100,Message.Reply_Type);
	    		ret.addProperty("Data"+0+System.currentTimeMillis(), d);
	    		ret.setSize((int)d.getSize());	
	    		this.createNewMessage(ret);
	    		this.numOfRepliedQuery++;
				dels.add(m);
			}
			System.out.println(m.getTo().name+"�ɹ��ظ�������"+m.getFrom().name+"�Ĳ�ѯ******************************");
			this.filterCube.putData(d,this);
			this.filterCube.putRequest(r);
			
		}
		this.waitDataMessages.remove(dels);
	}
	//���Ӳ�ѯ��Ϣ����Ϣ�б��У�ͬʱ���յĲ�ѯ����һ
	public void addMessageToWaitMessage(Message m){
		this.waitMessages.add(m);
		this.numOfQuery++;
	}

	//�����޷������ظ�����Ϣ����Ϣ�б��У�ͬʱ�����յĲ�ѯ����һ
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
		//����ǳ����ڵ�
		if(this.getType()==0)
			System.out.println(this.getName()+"�ڵ��ʣ��ռ����Ϊ��"+this.getRestSpaceRate()
				+"����Ϣ��ѯ�ɹ�ƽ��ʱ��Ϊ��"+this.getAverReplyTime()
				+"����Ϣ��ѯ�ɹ���Ϊ��"+this.getReplyRate()
				+"���ɹ��ظ�����Ϣ�����ñ�������ֱ�ӻظ��ı���"+this.getReplyByLocalRate());
		else if(this.getType()==1){//�����RSU
			System.out.println(this.getName()+"�ڵ��ʣ��ռ����Ϊ��"+this.getRestSpaceRate()
			+"����Ϣ��ѯ�ɹ�ƽ��ʱ��Ϊ��"+this.getAverReplyTime()
			+"����Ϣ��ѯ�ɹ���Ϊ��"+this.getReplyRate()
			+"���ɹ��ظ�����Ϣ�����ñ�������ֱ�ӻظ��ı���"+this.getReplyByLocalRate());
		}
	}
	/*
	 * ��ȡ�ռ�ռ����
	 */
	public double getRestSpaceRate() {
		// TODO Auto-generated method stub
		return this.filterCube.getRestSpace()/this.filterCube.fullSpace;
	}
	/*
	 * ������Ϣ��ѯƽ��ʱ��
	 */
	public double getAverReplyTime() {
		// TODO Auto-generated method stub
		return this.replyQueryTime/this.numOfRepliedQuery;
	}
	
	/*
	 * ������Ϣ��ѯ�ɹ���
	 */
	public double getReplyRate() {
		// TODO Auto-generated method stub
		if(this.numOfQuery==0) return 0;
		return (double)this.numOfRepliedQuery/this.numOfQuery;
	}
	/*
	 * ���سɹ���ѯ�ظ������ñ�������ֱ�ӻظ��ı���
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