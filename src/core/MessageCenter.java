package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import constructions.Data;
import constructions.Request;

public class MessageCenter {
    public static int deleteCount = 0; //删除的消息数
    public static int deliveredCount = 0; //投递成功的消息数
    public static List<Double> duringTimes = new ArrayList<>();
    public static List<Integer> hops = new ArrayList<>();
    public static double rsuThreshold = 50;
    public static double carThreshold = 40;
    public static double GodThreshold = 60;
    public static double oldTime = 0;
    public static int eventNumber = 0;
    public static int trueEvent = 0;
    public static int falseEvent = 0;
    public static int definateTrueTrueEvent = 0;
    public static int definateTrueFalseEvent = 0;
    public static int definateFalseTrueEvent = 0;
    public static int definateFalseFalseEvent = 0;
    public static List<Integer> definateEvent = new ArrayList<>();
    public static List<DTNHost> definatedEvent = new ArrayList<>();
    public static double cost = 0;//事件传输总消耗
    public static Map<DTNHost,Double> transformTime = new HashMap<>();//事件传输时间，从产生到判定所需时间
    
    /*
     * 接下来的是性能影响因素metric
     */
    //消息传送量
    public static double messageTransmission=0;
    //数据向上推送的数量
    public static double pushUpDatas=0;
    //数据向下拉的数量
    public static double pullDownDatas=0;
    //成功回复查询的比例
    public static double repliedQueryRate=0;
    //查询数量
    public static double querys;
    //成功查询数量
    public static double repliedQuerys=0;
    //计算成功回复查询比例的函数
    public static void calReplyRate(){
    	if(MessageCenter.querys==0) MessageCenter.repliedQueryRate=0;
    	else MessageCenter.repliedQueryRate=MessageCenter.repliedQuerys/MessageCenter.querys;
    }
    //filter cube 更新的计算时间
    public static double filterCubeUpdateTime=0;
   //filter cube的更新次数
    public static double filterCubeUpdates=0;
    
    //暂时添加的
    //查询的最长存在时间
    public static double exitTime=900;
    //有效距离
    public static double dis=500;
    //符合数据查询的时间间距长度
    public static double okTime=300;
    //为类别为交通等数据的查询进行选择目的RSU
    public static DTNHost selectRSUForTraffic(Request q){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	DTNHost des=null;
    	double near=20000;
    	for(DTNHost s:d){
    		//对RSU节点进行操作,判断query中的location最近的RSU
    		if(s.getType()==1){
    			double charge=s.getLocation().distance(q.getLocation());
    			if(charge<near){
    				near=charge;
    				des=s;
    			}
    		}
    	}
    	return des;
    }
    //为类别为娱乐等数据的查询进行选择目的节点
    public static DTNHost selectRSUForAmuse(DTNHost h){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	DTNHost res=null;
    	double ndis=2000;
    	for(DTNHost s:d){
    		//对RSU节点进行操作，选择一个距离最近的RSU
    		if(s.getType()==1){
    			
    			double dis=h.getLocation().distance(s.getLocation());
    			if(dis<ndis){
    				res=s;  
    				ndis=dis;
    			}
    				
    		}
    	}
    	return res;
    }
    /*
     * 为一个移动的车辆获取最近的路边节点，用来查询
     */
    public static DTNHost getNearestEdge(DTNHost h){
    	List<DTNHost> ds=SimScenario.getInstance().getEdges();
    	DTNHost res=null;
    	double minDis=2000000000;
    	for(DTNHost d:ds){
    		double dis=h.getLocation().distance(d.getLocation());
    		if(minDis>dis){
    			res=d;
    			minDis=dis;
    		}
    	}
    	return res;
    }
    //为RSU选取目的车辆以便向其发送请求以获取数据
    public static List<DTNHost> selectNodeForRSU(DTNHost h){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	List<DTNHost> res=new ArrayList<>();
    	int num=0;
    	for(DTNHost s:d){
    		//对不为RSU的节点进行操作
    		if(s.getType()!=1) {
    			if(s.getLocation().distance(h.getLocation())<400){
    				res.add(s);
    				num=num+1;
    			}
    		}
    		//设置RSU向车辆发送请求获取数据时选取三个车辆节点
    		if(num>2) return res;
    	}
    	return res;
    }
    //获取RSU节点的平均剩余空间占用率
    public static String showRestSpaceRateOfRSU(){
    	String res="";
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;//记录RSU个数
    	double allRate=0;//记录总的空间占用率
    	double minRate=1;
    	double maxRate=0;
    	for(DTNHost s:d){
    		if(s.getType()==1){
    			num++;
    			allRate+=s.getRestSpaceRate();
    			if(s.getRestSpaceRate()>maxRate) maxRate=s.getRestSpaceRate();
    			else if(s.getRestSpaceRate()<minRate) minRate=s.getRestSpaceRate();
    		}
    	}
    	allRate=allRate/num;
    	res=res+"所有RSU的平均剩余空间比例为："+allRate+"，最大剩余空间比例为："+maxRate
    			+"，最小剩余空间比例为："+minRate;
    	return res;
    }
    //获取RSU节点的平均回复查询成功率
    public static String showReplyRateOfRSU(){
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;
    	double allRate=0;
    	for(DTNHost s:d){
    		if(s.getType()==1&&s.getNumOfQuery()>0){
    			num++;
    			allRate+=s.getReplyRate();
    		}
    	}
    	allRate=allRate/num;
    	return "RSU节点的平均回复查询成功率为："+allRate;
    }
    //获取RSU节点的平均满足率
    public static String showReplyByLocalRateOfRSU(){
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;//记录RSU个数
    	double allRate=0;//记录总的满足率
    	for(DTNHost s:d){
    		if(s.getType()==1&&s.getNumOfQuery()>0){
    			num++;
    			allRate+=s.getReplyByLocalRate();
    		}
    	}
    	allRate=allRate/num;
    	return "RSU节点平均的本地数据满足查询率为："+allRate+"其中rsu个数："+num;
    }
    //获取RSU回复查询的平均消耗时间
    public static String showAverTimeOfRSU(){
    	String res="";
    	List<DTNHost> d=SimScenario.getInstance().getEdges();
    	int num=0;
    	double allTime=0;
    	for(DTNHost e:d){
    		if(e.getAverReplyTime()>0){
    			num++;
    			allTime=allTime+e.getAverReplyTime();
    		}
    	}
    	allTime=allTime/num;
    	res=res+"RSU回应查询的平均消耗时间为："+allTime;
    	return res;
    }
  //获取车辆的平均查询成功率
    public static String showReplyRateOfCar(){
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	double nums=0;
    	double allNum=0;//记录总的查询数
    	double allRate=0;//记录总的查询成功数
    	for(DTNHost s:d){
    		if(s.getType()==0&&s.getNumOfQuery()>0){
    			allNum+=1;
    			nums=nums+s.getNumOfQuery();
    			allRate+=s.getReplyRate();
    		}
    	}
    	
    	return "车辆的平均查询成功率为："+(double)allRate/allNum
    			+"发起查询的车辆中的查询车辆总数："+allNum
    			+"，查询总数："+nums;
    }
    
    //获取车辆查询的平均等待时间
    public static String showAverTimeOfCar(){
    	String res="";
    	List<DTNHost> d=SimScenario.getInstance().getHosts();
    	int num=0;//记录车辆个数
    	double allTime=0;//记录总的等待时间
    	for(DTNHost s:d){
    		if(s.getType()==0 &&s.getAverReplyTime()>0){
    			num++;
    			allTime+=s.getAverReplyTime();
    		}
    	}
    	allTime=allTime/num;
    	res=res+"车辆查询的平均回复等待时间为："+allTime;
    	return res;
    }
    
    //获取cloud端的直接响应查询成功率
    public static String showReplyByLocalRateOfCloud(){
    	String res="";
    	res=res+"Cloud端利用本地数据库直接回复查询的比例："+Cloud.getInstance().getReplyByLocalRate();
    	return res;
    }
    
    //获取cloud端的空间利用率
    public static String showRestSpaceRateOfCloud(){
    	String res="";
    	res=res+"Cloud 端的剩余空间比率为："+Cloud.getInstance().getRestSpaceRate();
    	return res;
    }
    //获取cloud端回复数据平均使用时长
    public static String showAverTimeOfCloud(){
    	String res="";
    	res=res+"Cloud端的回复查询平均使用时长为："+Cloud.getInstance().getAverReplyTime();
    	return res;
    }
   

  
    //为娱乐请求生成数据
    public static Data generDataForEnterQuery(Request q,int id){
    	return new Data(q.getTime(),id,q.getType(),q.getLevel(),"娱乐",q.getLocation());
    }
}
