/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import constructions.Request;
import core.DTNHost;
import core.Message;
import core.MessageCenter;
import core.SimClock;
import core.World;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	//消息类型，0:一般消息，1：查询消息，2：回复消息，3：数据传送消息
	private int eventType;

	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Where the message is destined to
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 */
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
		this.eventType=0;
	}
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time,int type) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
		this.eventType=type;
	}


	


	/**
	 * Creates the message this event represents.
	 */
	@Override
	public void processEvent(World world) {
		DTNHost to = world.getNodeByAddress(this.toAddr);
		DTNHost from = world.getNodeByAddress(this.fromAddr);

		
		
		//判断是否为查询事件，若eventType为1 ，视为查询事件。
		if(this.eventType==Message.Query_Type) {
			//若事件是查询创建事件，则重新设置消息的id,格式为（“query”+创建查询的节点名称+开始创建该消息的时间）
			Message m = new Message(from, to, "query"+from.getName()+System.currentTimeMillis(), this.size);
			m.setResponseSize(this.responseSize);
			
			//设置消息类型为query
			m.setType(Message.Query_Type);
			
			from.createNewMessage(m);
		}
		else if(this.eventType==0){
			Message m = new Message(from, to, this.id, this.size);
			m.setResponseSize(this.responseSize);
//			System.out.println("消息创建事件中设置一般事件");
			m.setType(Message.Gener_Type);
			from.createNewMessage(m);
		}else if(this.eventType==2){
			Message m = new Message(from, to, this.id, this.size);
			m.setResponseSize(this.responseSize);
//			System.out.println("消息创建事件中设置事件类型为回复事件");
			m.setType(Message.Reply_Type);
			from.createNewMessage(m);
		}
		
	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		"size:" + size + " CREATE";
	}
}
