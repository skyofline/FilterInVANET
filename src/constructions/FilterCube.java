package constructions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.corba.se.impl.oa.poa.ActiveObjectMap.Key;

import core.Cloud;
import core.DTNHost;
import core.MessageCenter;
import core.SimClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class FilterCube {
	public Map<String,Map<Integer,Splits>> dimNum=new LinkedHashMap<String,Map<Integer,Splits>>();
	public Map<Integer,Keys> keysNum=new LinkedHashMap<Integer,Keys>();
	/*
	 * 首先定义filter cube中的key类、value类和split类
	 */
	
	
	private Map<Keys,Values> fc=new LinkedHashMap<Keys,Values>();

	public FilterCube(){
		
	}
	/*
	 * 初始化filtercube中的dimNum和keyNum，
	 */
	public void initFilterCube(){
		List<String> dims=this.getDimensions();
		for(String s:dims){
			Map<Integer,Splits> is=new LinkedHashMap<Integer,Splits>();
			dimNum.put(s, is);
		}
		for(Keys k:this.fc.keySet()){
			int kPlace=1;
			for(String s:k.getDimensions()){
				int n=this.putSplits(k.getKey().get(s));
				kPlace=kPlace*n;
			}
			if(!this.keysNum.containsKey(kPlace)){
				Keys head=Keys.creatHeadKey();
				this.putKeyInKeyNum(kPlace, head);
			}
			this.putKeyInKeyNum(kPlace, k);
		}
		
	}
	/*
	 * 这里存储输入参数
	 */
	public double fullSpace=0.5*1024*1024;
	private double restSpace=0.5*1024*1024;//记录filter cube中的剩余可用存储空间，初始默认值为1GB
	
	private double updateThreshold1=0.3;
	private double updateThreshold2=1.5;	//判断存储空间是否更新的阈值
	private double spaceThreshold=0.2;
	/*
	 * 这里存储评价体系参数
	 */
	private double numOfData=0;
	private double numOfRequest=0;
	private double numOfFilter=0;
	private long numOfExpandDatas=0;
	/*
     * 计算dimensional split factor的balanced factor,暂时假定为0.5
     */
    private double balanceFactor=0.5;
	/*
	 *将一条数据添加到filter cube中,同时判断是否传播
	 */
    public void putData(Data d,DTNHost dtn){
  		if(this.fc.keySet().size()==0) System.out.println(dtn.getName()+"Filter cube 未建立，需要重启或修正bug");
//    	System.out.println(dtn.getName()+"中的filter cube的层级数："+ks.size());
//  		System.out.println(d.toString());
  		Keys k=this.getKeysByData(d);
  		if(k==null) System.err.println("filter cube放入数据到DTNHost出错，未找到相应key");
    	if(k.inRange(d)){
    		if(!this.fc.containsKey(k)){
    			System.err.println("wrong in there,putData function");
    			
    		}
    		Values v=this.fc.get(k);
    		//添加数据前根据filter 判断重设数据是否上传状态
    		for(Filter f:v.getFilters()){
    			if(f.judgeData(d)) f.resetDataStatus(d);
    		}
    		if(this.getRestSpaceRate()<this.spaceThreshold){
    			this.updateDatas();
    		}
    		if(this.getRestSpaceRate()<this.spaceThreshold){
    			this.clearDatas();
    		}
    		v.addData(d);
    		//添加数据时更新filter cube的剩余空间
    		this.restSpace=this.restSpace-d.getSize();
    		//添加数据后，判断尝试将数据上传
    		if(dtn.getType()==0){
    			this.numOfExpandDatas++;
    			if(d.getExpandState()==0) dtn.uploadDataToEdge(d);
    			 
    		}
    		else if(dtn.getType()==1) {
    			this.numOfExpandDatas++;
    			if(d.getExpandState()==0) dtn.uploadDataToCloud(d);
    		}
    		
    		if(d.getExpandState()==0&&d.getUsageCount()==0) this.fc.get(k).getFilters().get(0).addUnusedPassDatas(1);
    		if(d.getExpandState()==1&&d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedBlockDatas(1);
    		if(d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedDatas(1);
//    			this.updateFilter(k, this.fc.get(k));
   			this.numOfData=this.numOfData+1;  
   		}else{
   			System.err.println("插入数据失败");
   		}
  			
//    	System.out.println("数据大小为："+d.getSize()
//    		+"，"+dtn.getName()+"添加前剩余大小为："+this.getRestSpace()
//    		+"，添加后的剩余大小为："+this.getRestSpace());
    }
    /*
	 *将一条数据添加到filter cube中,不判断是否传播
	 */
    public void putDataForCloud(Data d){
  		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube 未建立，需要重启或修正bug");
  		Keys k=this.getKeysByData(d);
  		if(k==null) System.err.println("放入数据到cloud中，filter cube放入数据出错,未找到相应key");
    		if(k.inRange(d)){
    			//添加数据前根据filter 判断重设数据是否上传状态
    			Values v=this.fc.get(k);
    			for(Filter f:v.getFilters()){
    				if(f.judgeData(d)) f.resetDataStatus(d);
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				this.updateDatas();
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				this.clearDatas();
    			}
    			v.addData(d);
    			//添加数据时更新filter cube的剩余空间
    			this.restSpace=this.restSpace-d.getSize();
    			if(d.getExpandState()==0&&d.getUsageCount()==0) this.fc.get(k).getFilters().get(0).addUnusedPassDatas(1);
    			if(d.getExpandState()==1&&d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedBlockDatas(1);
    			if(d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedDatas(1);
//    			this.updateFilter(k, this.fc.get(k));
    			this.numOfData=this.numOfData+1;    
    		}
    }
    /*
	 * 向filter cube中添加request
	 */
	public void putRequest(Request r){
		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube 未建立，需要重启或修正bug");
  		List<Keys> ks=this.getKeysByRequest(r);
  		if(ks.size()==0){
  			System.err.println("filter cube放入request出错,未找到相应key"+r.toString()+"request类型"+r.getType()+"\n");
  			System.err.println(this.dimNumString());
  			for(Keys k:this.keysNum.values()){
  				while(k!=null){
  					System.err.println(k.toString());
  					k=k.next;
  				}
  			}
  			
//  			for(Keys k:this.keysNum.values()){
//  				while(k!=null){
//  					System.err.println(k.toString());
//  					k=k.next;
//  				}
//  			}
  		}
    	for(Keys k:ks)
    		if(k.inRange(r)){
    			this.fc.get(k).addRequest(r);			
    		}
    	this.numOfRequest=this.numOfRequest+1;
    	
	}
    //更新filter cube中每一行的filter
	public void update(){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		for(Entry<Keys, Values> entry:this.fc.entrySet()){
			Keys nk=entry.getKey();
			Values v=entry.getValue();
			Filter f=v.getFilters().get(0);//获取第一个filter
			//如果filter不是basicFilter,则进行计算判断更新状态或置为basic
			if(f.getBasicStatus()==0){
				double mf=f.getMismatchFactor(v.getDatas());
				if(mf<this.getUpdateThreshold1()){
//					double radioCost=f.calRadioCost(v.getDatas(),v.getRequests());
					f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
				}else{
//					FilterCube newFilterCube=new FilterCube();
//					newFilterCube.addFilter(f);
					//设置basic filter 的起始时间
					f.setBasicTime(SimClock.getTime());
					//设置filter为basic filter
					f.setBasicStatus(1);
				}
			}else{
				//对basicFilter进行处理
				if(Timer.judgeFilter(f, f.getPeriodTime())){
					//首先计算avg，用于衡量数据量，以此来表达数据广度
					double misDAF=0;
					double sum=0;
					for(Data d:v.getDatas()){
						if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
						sum=sum+1;
					}
					double avg=misDAF/sum;
					//计算mismatch factor
					double mismatchs=f.getMismatchFactor(v.getDatas());
					double er=avg/mismatchs;
					if(er>this.getUpdateThreshold2()){
						f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
						//此时要把basic状态取消
						f.setBasicStatus(0);
					}else{
						//将filter置为more状态，此时没必要，暂不进行操作
						//取消filter的basic状态
						f.setBasicStatus(0);
						int len=f.getDims().size();
						double[] min=new double[len];
				    	int[] split=new int[len];
				    	for(int i=0;i<len;i++){
				    		min[i]=1000000;
				    		split[i]=1;
				    	}
				    	List<String> l=this.getDimensions();
				    	for(int i=0;i<len;i++){
				    		String dim= l.get(i);
				    		for(int j=1,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
				    			double dimSplitFac=this.getDimSplitFactor( dim, j);
				    			if(dimSplitFac<min[i]){
				    				min[i]=dimSplitFac;
				    				split[i]=j;
				    			}
				    		}
				    		Map<Keys,Values> newss=this.splitDimension(nk, dim, split[i]);
				    		//如果切分后的filter个数大于一个，即进行切分，则添加新的分片，删除旧的分片
				    		if(newss.size()>1){
				    			adds.putAll(newss);
				    			//将所有原有的keys添加到delKeys，并最终删除掉
				    			delKeys.add(nk);
				    			MessageCenter.splitedFilters=MessageCenter.splitedFilters+1;
				    		}
				    	}
					}
				}
			}
			
		}
		//删除被切分的分片和添加分出的分片
		if(delKeys.size()>0){
			for(Keys kk:delKeys){
				this.fc.remove(kk);
			}
		}
		if(adds.size()>0){
			this.fc.putAll(adds);
			System.out.println("切分filter.....");
		}
	}
	
	/*
	 * 更新filter cube中的一行filter
	 */
	public void updateFilter(Keys k,Values v){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		Filter f=v.getFilters().get(0);//获取第一个filter
		//如果filter不是basicFilter,则进行计算判断更新状态或置为basic
		if(f.getBasicStatus()==0){
			double mf=f.getMismatchFactor(v.getDatas());
			if(mf<this.getUpdateThreshold1()){
//				double radioCost=f.calRadioCost(v.getDatas(),v.getRequests());
				f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
			}else{
//				FilterCube newFilterCube=new FilterCube();
//				newFilterCube.addFilter(f);
				//设置basic filter 的起始时间
				f.setBasicTime(SimClock.getTime());
				//设置filter为basic filter
				f.setBasicStatus(1);
			}
		}else{
			//对basicFilter进行处理
			if(Timer.judgeFilter(f, f.getPeriodTime())){
				//首先计算avg，用于衡量数据量，以此来表达数据广度
				double misDAF=0;
				double sum=0;
				for(Data d:v.getDatas()){
					if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
					sum=sum+1;
				}
				double avg=misDAF/sum;
				//计算mismatch factor
				double mismatchs=f.getMismatchFactor(v.getDatas());
				double er=avg/mismatchs;
				if(er>this.getUpdateThreshold2()){
					f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
					//此时要把basic状态取消
					f.setBasicStatus(0);
				}else{
					//将filter置为more状态，此时没必要，暂不进行操作
					//取消filter的basic状态
					f.setBasicStatus(0);
					int len=f.getDims().size();
					double[] min=new double[len];
			    	int[] split=new int[len];
			    	for(int i=0;i<len;i++){
			    		min[i]=1000000;
			    		split[i]=1;
			    	}
			    	List<String> l=this.getDimensions();
			    	for(int i=0;i<len;i++){
			    		String dim= l.get(i);
			    		for(int j=1,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
			    			double dimSplitFac=this.getDimSplitFactor( dim, j);
			    			if(dimSplitFac<min[i]){
			    				min[i]=dimSplitFac;
			    				split[i]=j;
			    			}
			    		}
			    		Map<Keys,Values> newss=this.splitDimension(k, dim, split[i]);
			    		//如果切分后的filter个数大于一个，即进行切分，则添加新的分片，删除旧的分片
			    		if(newss.size()>1){
			    			adds.putAll(newss);
			    			//将所有原有的keys添加到delKeys，并最终删除掉
			    			delKeys.add(k);
			    		}
			    	}
				}
			}
		}
			
		//删除被切分的分片和添加分出的分片
		if(delKeys.size()>0){
			for(Keys kk:delKeys){
				this.fc.remove(kk);
			}
		}
		if(adds.size()>0){
			this.fc.putAll(adds);
		}
	}
	/*
	 * 更新filter cube中的每一行数据，判断留存或删除
	 */
	public void updateDatas(){
		for(Entry<Keys, Values> entry:this.fc.entrySet()){
			Keys nk=entry.getKey();
			Values v=entry.getValue();
			List<Data> datas=v.getDatas();
			List<Data> dels=new ArrayList<Data>();
			double releaseSpace=0;
			double delNum=0;
			int sum=datas.size();
			for(Data d:datas){
				/*
				 * 这里暂时使用数据时间和数据使用次数作为删除数据的依据
				 */
				
				if((SimClock.getTime()-d.getTime()>5400&&d.getExpandState()==0)||(SimClock.getTime()-d.getTime()>7200&&d.getUsageCount()<5)){
					dels.add(d);
					releaseSpace=releaseSpace+d.getSize();
					delNum=delNum+1;
					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
				}
				if(delNum/sum>0.1) break;
			}
			if(dels.size()>0){
				datas.removeAll(dels);
				System.out.println("正在清理数据.....");
			}
			this.numOfData=this.numOfData-delNum;
			this.restSpace=this.restSpace+releaseSpace;
			
		}
	}
	/*
	 * 删除大量数据，当无法删除数据时，增大删除尺度删除大量数据
	 */
	public void clearDatas(){
		for(Entry<Keys, Values> entry:this.fc.entrySet()){
			Keys nk=entry.getKey();
			Values v=entry.getValue();
			List<Data> datas=v.getDatas();
			List<Data> dels=new ArrayList<Data>();
			double releaseSpace=0;
			double delNum=0;
			int sum=datas.size();
			for(Data d:datas){
				/*
				 * 这里暂时使用数据时间作为删除数据的依据
				 */
				
				if(SimClock.getTime()-d.getTime()>1800||d.getUsageCount()<5){
					dels.add(d);
					releaseSpace=releaseSpace+d.getSize();
					delNum=delNum+1;
					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
				}
				
				if(delNum/sum>0.1) break;
				/*
				 * 进行数据整合的代码
				 */
				
			}
			if(dels.size()>0){
				datas.removeAll(dels);
			}
			this.numOfData=this.numOfData-delNum;
			this.restSpace=this.restSpace+releaseSpace;
		}
	}
//	/*
//	 * 更新判断data的状态，并根据当前DTNHost的类别（rsu，车辆等）
//	 * 对数据
//	 * 进行传送
//	 */
//	public void updateDataStatusByEdgeOCar(DTNHost dtn,Data d){
//		Set<Keys> k=this.fc.keySet();
//		Iterator it=k.iterator();
//		while(it.hasNext()){
//			Keys key=(Keys) it.next();
//			Values v=this.fc.get(key);
//			List<Filter> fs=v.getFilters();
//			for(Filter f:fs){
//				for(Data d:v.getDatas()){
//					f.resetDataStatus(d);
//					//如果该节点是RSU节点
//					if(dtn.getType()==1){
//						dtn.uploadDataToCloud(d);
//					}else if(dtn.getType()==0){
//						//如果节点是车辆节点
//						dtn.uploadDataToEdge(d);
//					}
//				}
//			}
//			
//		}
//	}
//	/*
//	 * 更新判断data的状态，对cloud端的尽心处理
//	 * 对数据
//	 * 进行传送
//	 */
//	public void updateDataStatusByCloud(){
//		Set<Keys> k=this.fc.keySet();
//		Iterator it=k.iterator();
//		while(it.hasNext()){
//			Keys key=(Keys) it.next();
//			Values v=this.fc.get(key);
//			List<Filter> fs=v.getFilters();
//			for(Filter f:fs){
//				for(Data d:v.getDatas()){
//					f.resetDataStatus(d);
//					Cloud.getInstance().downloadDataToEdge(d);
//				}
//			}
//			
//		}
//	}
	
	/*
	 * 在DTNHost(Edge或Car)
	 * 对request进行处理，找出filtercube中能够满足该request的数据
	 * 同时当request到的数据的使用情况符合条件是，重新设置数据传播状态
	 * 并根据需要进行传播
	 */
	public List<Data> answerRequest(Request r,DTNHost dtn){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		List<Data> res=new ArrayList<Data>();
		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube 未建立，需要重启或修正bug");
  		List<Keys> keyss=this.getKeysByRequest(r);
  		if(keyss.size()==0) System.err.println("未找到相应request的key"+r.toString());
  		for(Keys k:keyss){
//  			Set<Keys> kt=this.fc.keySet();
    		Values v=this.fc.get(k);
    		Filter f=v.getFilters().get(0);
    		if(f.judgeRequest(r)){
    			List<Data> s=v.getDatas();
    			for(Data t:s){
    				if(r.judgeData(t)) {
    					t.addUsageCount();//数据添加使用次数
    					int sign=0;//在此处用于标记是否该数据是否是传播状态
    					if(t.getExpandState()==0) sign=1;
    					f.resetDataStatus(t);
    					if(t.getUsageCount()==1){
    						if(sign==0){
    							if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						else{
    							if(t.getExpandState()==0) f.addUnusedPassDatas(-1);
    							else if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						f.addUsedDatas(1);
    						this.updateFilter(k, v);
    					}
    					//判断是否上传
    					if(t.getExpandState()==0&&sign==0){
    					
    						if(dtn.getType()==0) dtn.uploadDataToEdge(t);
    						else if(dtn.getType()==1) dtn.uploadDataToCloud(t);
    					}
    					res.add(t);
    				}
    			}
    			//如果该filter是一个basic filter，则要判断是否还在考察时间内
    			if(f.getBasicStatus()==1){
    				//对basicFilter进行处理
    				if(Timer.judgeFilter(f, f.getPeriodTime())){
    					//首先计算avg，用于衡量数据量，以此来表达数据广度
    					double misDAF=0;
    					double sum=0;
    					for(Data d:v.getDatas()){
    						if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
    						sum=sum+1;
    					}
    					double avg=misDAF/sum;
    					//计算mismatch factor
    					double mismatchs=this.calMismatchFactor(v.getDatas());
    					double er=avg/mismatchs;
    					if(er>this.getUpdateThreshold2()){
//    						double radioCost=f.calRadioCost(v.getDatas(),v.getRequests());
    						f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
    						//此时要把basic状态取消
    						f.setBasicStatus(0);
    					}else{
    						//将filter置为more状态，此时没必要，暂不进行操作
    						int len=f.getDims().size();
    						double[] min=new double[len];
    				    	int[] split=new int[len];
    				    	for(int i=0;i<len;i++){
    				    		min[i]=1000000;
    				    		split[i]=1;
    				    	}
    				    	for(int i=0;i<len;i++){
    				    		List<String> l=new ArrayList<String>(f.getDims().keySet());
    				    		String dim= l.get(i);
    				    		for(int j=1,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
    				    			double dimSplitFac=this.getDimSplitFactor( dim, j);
    				    			if(dimSplitFac<min[i]){
    				    				min[i]=dimSplitFac;
    				    				split[i]=j;
    				    			}
    				    		}
    				    		Map<Keys,Values> newss=this.splitDimension(k, dim, split[i]);
    				    		//如果切分后的filter个数大于一个，即进行切分，则添加新的分片，删除旧的分片
    				    		if(newss.size()>1){
    				    			adds.putAll(newss);
    				    			//将所有原有的keys添加到delKeys，并最终删除掉
    				    			delKeys.add(k);
    				    		}
    				    	}
    					}
    				}
    			}
    		}
  		}
		
    	//删除被切分的分片和添加分出的分片
    	if(delKeys.size()>0){
    		for(Keys kk:delKeys){
    			this.fc.remove(kk);
    		}
    	}
    	if(adds.size()>0){
    		this.fc.putAll(adds);
    	}
		return res;
	}
	/*
	 * Cloud
	 * 对request进行处理，找出filtercube中能够满足该request的数据
	 * 同时当request到的数据的使用情况符合条件是，重新设置数据传播状态
	 * 并根据需要进行传播
	 */
	public List<Data> answerRequest(Request r,Cloud dtn){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		List<Data> res=new ArrayList<Data>();
		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube 未建立，需要重启或修正bug");
		List<Keys> keyss=this.getKeysByRequest(r);
  		if(keyss.size()==0) System.err.println("未找到相应request的key"+r.toString());
  		for(Keys k:keyss){
    		Values v=this.fc.get(k);
    		Filter f=v.getFilters().get(0);
    		if(f.judgeRequest(r)){
    			List<Data> s=v.getDatas();
    			for(Data t:s){
    				if(r.judgeData(t)) {
    					t.addUsageCount();//数据添加使用次数
    					int sign=0;//在此处用于标记是否该数据是否是传播状态
    					if(t.getExpandState()==0) sign=1;
    					f.resetDataStatus(t);
    					if(t.getUsageCount()==1){
    						if(sign==0){
    							if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						else{
    							if(t.getExpandState()==0) f.addUnusedPassDatas(-1);
    							else if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						f.addUsedDatas(1);
    						this.updateFilter(k, v);
    					}
    					res.add(t);
    				}
    			}
    			//如果该filter是一个basic filter，则要判断是否还在考察时间内
    			if(f.getBasicStatus()==1){
    				//对basicFilter进行处理
    				if(Timer.judgeFilter(f, f.getPeriodTime())){
    					//首先计算avg，用于衡量数据量，以此来表达数据广度
    					double misDAF=0;
    					double sum=0;
    					for(Data d:v.getDatas()){
    						if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
    						sum=sum+1;
    					}
    					double avg=misDAF/sum;
    					//计算mismatch factor
    					double mismatchs=this.calMismatchFactor(v.getDatas());
    					double er=avg/mismatchs;
    					if(er>this.getUpdateThreshold2()){
    						f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
    						//此时要把basic状态取消
    						f.setBasicStatus(0);
    					}else{
    						//将filter置为more状态，此时没必要，暂不进行操作
    				    	for(String dim:f.getDims().keySet()){
    				    		double min=1000000;
    				    		int split=1;
    				    		for(int j=1,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
    				    			double dimSplitFac=this.getDimSplitFactor( dim, j);
    				    			if(dimSplitFac<min){
    				    				min=dimSplitFac;
    				    				split=j;
    				    			}
    				    		}
    				    		Map<Keys,Values> newss=this.splitDimension(k, dim, split);
    				    		//如果切分后的filter个数大于一个，即进行切分，则添加新的分片，删除旧的分片
    				    		if(newss.size()>1){
    				    			adds.putAll(newss);
    				    			//将所有原有的keys添加到delKeys，并最终删除掉
    				    			delKeys.add(k);
    				    		}
    				    	}
    					}
    				}
    			}
    		
    		}
  		}
    	//删除被切分的分片和添加分出的分片
    	if(delKeys.size()>0){
    		for(Keys kk:delKeys){
    			this.fc.remove(kk);
    		}
    	}
    	if(adds.size()>0){
    		this.fc.putAll(adds);
    	}
		return res;
	}
	
	//dimension split
	public Map<Keys, Values> splitDimension(Keys k,String dimension,int num){
		/*
		 * 维度切分，首先要将旧的维度对应删除，再将新的维度分片添加进去
		 */
		
		Map<Keys,Values> addKV=new LinkedHashMap<>();
		
		if(num>1){
			List<Splits> addSp=new ArrayList<Splits>();
//			List<Splits> delSp=new ArrayList<Splits>();
//			delSp.add(k.getKey().get(dimension));
			int beNumKey=1;
			for(String di:k.getKey().keySet()){
				if(!di.equals(dimension)){
					int temp=this.getDimNum(di,k.getKey().get(di));
					beNumKey=beNumKey*temp;
				}
			}
			
//			List<Keys> del=new ArrayList<>();
			if(k.getKey().containsKey(dimension)){
				
				//将dimension维度进行切分,平均切分成num个filter
				//首先获得当前filter在该维度的跨度值，然后进行切分
				double max=k.getKey().get(dimension).getMaxBord()-k.getKey().get(dimension).getMinBord();
				double aver=max/num;
				for(int i=0;i<num;i++){
					Keys nKey=new Keys(k);
					nKey.changeDimensionValue(dimension, k.getKey().get(dimension).getMinBord()+i*aver,k.getKey().get(dimension).getMinBord()+(i+1)*aver);
					addSp.add(nKey.getKey().get(dimension));
					Values newValues=new Values(this.fc.get(k));
					//先清空newValues中的数据，然后从原来数据中挑选符合的放入newValues中
					newValues.clearAllDatas();
					newValues.clearAllRequests();
					newValues.changeDimensionValue(dimension, k.getKey().get(dimension).getMinBord()+i*aver,k.getKey().get(dimension).getMinBord()+(i+1)*aver);
					int usedDatas=0;
					int unusedPassDatas=0;
					int usedBlockDatas=0;
					for(Data d:this.fc.get(k).getDatas()){
						if(nKey.inRange(d)) newValues.getDatas().add(d);
						if(d.getUsageCount()>0) usedDatas++; 
						if(d.getExpandState()==0&&d.getUsageCount()==0) unusedPassDatas++;
						if(d.getExpandState()==1&&d.getUsageCount()>0) usedBlockDatas++;
					}
					newValues.getFilters().get(0).setUsedDatas(usedDatas);
					newValues.getFilters().get(0).setUnusedPassDatas(unusedPassDatas);
					newValues.getFilters().get(0).setUsedBlockDatas(usedBlockDatas);
					for(Request r:this.fc.get(k).getRequests()){
						if(nKey.inRange(r)) newValues.getRequests().add(r);
					}
					//将切分后的filter加入到filtercube中
					addKV.put(nKey, newValues);
				}
			}
//			for(Splits s:delSp){
//				this.delSplits(s);
//			}
			for(Splits s:addSp){
				int pl=this.putSplits(s);
				if(pl<=0) System.err.println("添加出错");
			}
			for(Keys nk:addKV.keySet()){
				int nums=this.getPosByKey(nk);
				this.putKeyInKeyNum(nums, nk);
				
			}
			this.delKeyInKeyNum(this.getPosByKey(k), k);
			return addKV;
		}else{
			addKV.put(k, this.fc.get(k));
			return addKV;
		}
	}
	/*
	 * 向filter cube中添加一个filter
	 */
	public void addFilter(Filter f){
		Keys k=this.getKeysByFIlter(f);
  		if(k==null) System.err.println("filter cube添加出错，未找到相应key");
		if(k.inRange(f)){
			this.fc.get(k).addFilter(f);
			this.numOfFilter=this.numOfFilter+1;
		}
	}
	/*
	 * 计算获取一个维度的最大分片数量
	 * 这里暂时使用道路场景图像数据type==1先进行构建
	 */
	public static int getMaxSplits(String dimension){
		if(dimension.equals("Weather")) return 3;
		else if(dimension.equals("Time")) return 5;
		else if(dimension.equals("TrafficCondition")) return 3;
		else if(dimension.equals("Size")) return 1;
		else return 3;
	}
	/*
	 * 这里计算获取filter cube某个维度的mis(f,s)值
	 */
	public int getSumOfMis(String dimension,double num){
		double max=this.getMaxNumInDim(dimension);
		double aver=max/num;
		int sum=0;
		for(int i=0;i<num;i++){
    		int numOfOpen=0,numOfClose=0;
    		for(Keys k:this.fc.keySet()){
    			if(k.getKey().containsKey(dimension)&&(k.getKey().get(dimension).getMinBord()>=i*aver)
    					&&(k.getKey().get(dimension).getMaxBord()<=(i+1)*aver))
    				if(this.fc.get(k).getStatus()==0) numOfOpen++;
    				else numOfClose++;
    		}
    		if(numOfOpen>numOfClose) sum=sum+numOfClose;
    		else sum=sum+numOfOpen;
    	}
		return sum;
	}
	/*
	 * 计算一个维度的最大值，将用于计算如何分片
	 * 这里暂时使用道路场景图像数据type==1先进行构建
	 */
	public double getMaxNumInDim(String dimension){
		if(dimension.equals("Weather")) return 3;
		else if(dimension.equals("Time")) return 4;
		else if(dimension.equals("TrafficCondition")) return 3;
		else return 2;
	}
	/*
	 * 获得filter cube 中的dimension 列表
	 */
	public List<String> getDimensions(){
		List<String> l=new ArrayList<String>();
		for(Keys k:this.fc.keySet()){
			l=k.getDimensions();
			break;
		}
		if(l.size()==0) System.err.println("filter cube获取维度错误");
		return l;
	}
//	/*
//	 * 制造该filter cube的某一行的副本
//	 * 
//	 */
//	public void copyFromKeyValue(Keys k,Values v){
//		
//	}
	/*
	 * 在dimNum中删除原有splits,
	 *  public int delSplits(Splits olds){
		int delNum=-1;
		for(Integer i:this.dimNum.get(olds.getDimension()).keySet()){
			if(this.dimNum.get(olds.getDimension()).get(i).isEqual(olds)){
				delNum=i;
				break;
			}
		}
		if(delNum==-1){
			System.err.println("wrong in deleting splits");
			return -1;
		}
		else {
			this.dimNum.get(olds.getDimension()).remove(delNum);
			return delNum;
		}
			
	}

	 */
	
	/*
	 * 在dimNum中添加新的splits
	 */
	public int  putSplits(Splits news){
		for(Splits s:this.dimNum.get(news.getDimension()).values()){
			if(s.equals(news)) return -1;
		}
		int len=this.dimNum.get(news.getDimension()).size()+1;
		while(this.dimNum.get(news.getDimension()).containsKey(len)) len++;
		this.dimNum.get(news.getDimension()).put(len, news);		
		return len;
	}

	/*
	 * 在keyNum中添加新的key
	 */
	public void putKeyInKeyNum(int pos,Keys k){
		if(this.keysNum.containsKey(pos)){
			Keys tk=this.keysNum.get(pos);
			while(tk.next!=null) tk=tk.next;
			tk.next=k;
		}else{
			Keys heads=Keys.creatHeadKey();
			this.keysNum.put(pos, heads);
			this.keysNum.get(pos).next=k;
		}
	}
	
	/*
	 * 在keyNum中删除原有的key
	 */
	public void delKeyInKeyNum(int pos, Keys k){
		if(this.keysNum.containsKey(pos)){
			Keys hkey=this.keysNum.get(pos);
			while(hkey.next!=null){
				if(hkey.next.isEqual(k)){
					hkey.next=hkey.next.next;
				}else{
					hkey=hkey.next;
				}
			}
		}
	}
	
	/*
	 * 根据dimension和值获取相应的dimNum
	 */
	public List<Integer> getDimNum(String dimension,double value){
		List<Integer> reslist=new ArrayList<Integer>();
		if(this.dimNum.keySet().contains(dimension)){
			for(Integer i:this.dimNum.get(dimension).keySet()){
				if(this.dimNum.get(dimension).get(i).inRange(value)){
					reslist.add(i);
					
				}
			}
		}
		return reslist;
	}
	/*
	 * 根据dimension和分片获取相应的dimNum
	 */
	public int getDimNum(String dimension,Splits s){
		int res=-1;
		if(this.dimNum.keySet().contains(dimension)){
			for(Integer i:this.dimNum.get(dimension).keySet()){
				if(this.dimNum.get(dimension).get(i).isEqual(s)){
					res=i;
					break;
				}
					
			}
		}
		return res;
	}
	/*
	 * 根据request获取key
	 */
	public List<Keys> getKeysByRequest(Request r){
		Keys k=null;
		List<Integer> keyNumList = new ArrayList<Integer>();
		for(String s:r.getDims().keySet()){
			List<Integer> num=this.getDimNum(s, r.getDims().get(s));
			if(keyNumList.size()==0){
				keyNumList=num;
			}else{
				List<Integer> temp=new ArrayList<Integer>();
				for(int i:keyNumList){
					for(int j:num){
						int t=i*j;
						if(!temp.contains(t)){
							temp.add(t);
						}
					}
				}
				keyNumList=temp;
			}
		}
		String anoDim="Size";
		if(this.dimNum.containsKey(anoDim)){
			List<Integer> num=new ArrayList<Integer>();
			for(int i:this.dimNum.get(anoDim).keySet()) num.add(i);
			if(keyNumList.size()==0){
				keyNumList=num;
			}else{
				List<Integer> temp=new ArrayList<Integer>();
				for(int i:keyNumList){
					for(int j:num){
						int t=i*j;
						if(!temp.contains(t)){
							temp.add(t);
						}
					}
				}
				keyNumList=temp;
			}
		}
		List<Keys> resK=new ArrayList<Keys>();
		for(int keynum:keyNumList){
			k=this.keysNum.get(keynum);
			if(!this.keysNum.containsKey(keynum)){
				continue;
			}
			while(!k.inRange(r)){
				k=k.next;
				if(k==null) break;
			}
			if(k==null) continue;
			else{
				resK.add(k);
				continue;
			}
		}
		return resK;
	}
	
	/*
	 * 根据data获取key
	 */
	public Keys getKeysByData(Data d){
		Keys k=null;
		List<Integer> keyNumList = new ArrayList<Integer>();
		List<Integer> num=new ArrayList<Integer>();
		List<Integer> temp=new ArrayList<Integer>();
		for(String s:d.getDimensions().keySet()){
			num=this.getDimNum(s, d.getDimensions().get(s));
			if(keyNumList.size()==0){
				keyNumList=num;
			}else{
				temp.clear();
				for(int i:keyNumList){
					for(int j:num){
						int t=i*j;
						if(!temp.contains(t)){
							temp.add(t);
						}
					}
				}
				keyNumList.clear();
				keyNumList.addAll(temp);
			}
		}
		Keys resK=null;
		for(int keynum:keyNumList){
			k=this.keysNum.get(keynum);
			resK=k;
			while(!resK.inRange(d)){
				resK=resK.next;
				if(resK==null) break;
			}
			if(resK==null) continue;
			else{
				break;
			}
		}
		return resK;
	}
	/*
	 * 根据filter获取key
	 */
	public Keys getKeysByFIlter(Filter f){
		Keys k=null;
		int keynum=1;
		for(String s:f.getDims().keySet()){
			int num=this.getDimNum(s,f.getDims().get(s));
			if(num==-1) System.err.println("出错，未能获取dimension的位置值");
			else keynum=keynum*num;
		}
		k=this.keysNum.get(keynum);
		Keys resK=k;
		while(!this.fc.containsKey(resK)||!resK.inRange(f)) resK=resK.next;
		return resK;
	}
	/*
	 * 获取keys应该所在的位置
	 */
	public int getPosByKey(Keys k){
		int res=1;
		for(String s:k.getDimensions()){
			int ns=this.getDimNum(s,k.getKey().get(s));
			if(ns<0){
				System.err.println("获取key所在的位置出错");
				System.exit(-1);
			}
			res=res*ns;
		}
		
		return res;
	}
	public String dimNumString(){
		StringBuilder res=new StringBuilder("");
		for(String s:this.dimNum.keySet()){
			res.append(s).append("\n");
			for(int i:this.dimNum.get(s).keySet()){
				res.append(i).append(":").append(this.dimNum.get(s).get(i).toString()).append(",");
			}
			res.append("\n");
		}
		return res.toString();
	}
	public Map<Keys,Values> getFc() {
		return fc;
	}
	public void setFc(Map<Keys,Values> fc) {
		this.fc = fc;
	}
	public double getUpdateThreshold1() {
		return updateThreshold1;
	}
	public void setUpdateThreshold1(double updateThreshold1) {
		this.updateThreshold1 = updateThreshold1;
	}
	public double getUpdateThreshold2() {
		return updateThreshold2;
	}
	public void setUpdateThreshold2(double updateThreshold2) {
		this.updateThreshold2 = updateThreshold2;
	}
	public double getDimSplitFactor(String di,int x) {
		double res=(this.getBalanceFactor()*(double)x)/FilterCube.getMaxSplits(di);
    	res=res+(1-this.getBalanceFactor())*this.getSumOfMis(di, x);
    	return res;
	}

	public double getBalanceFactor() {
		return balanceFactor;
	}
	public void setBalanceFactor(double balanceFactor) {
		this.balanceFactor = balanceFactor;
	}
	public double getNumOfData() {
		return numOfData;
	}
	public void setNumOfData(double numOfData) {
		this.numOfData = numOfData;
	}
	public double getNumOfRequest() {
		return numOfRequest;
	}
	public void setNumOfRequest(double numOfRequest) {
		this.numOfRequest = numOfRequest;
	}
	public double getNumOfFilter() {
		return numOfFilter;
	}
	public void setNumOfFilter(double numOfFilter) {
		this.numOfFilter = numOfFilter;
	}
	public void showFilterCubeStruct(){
		System.out.println("该filter cube的size大小为："+this.fc.size());
		for(Keys k:this.fc.keySet()){
			System.out.println(k.toString());
		}
	}
	/*
	 * 为filtercube添加维度框架（利用原始filter）
	 */
	public void addDimFrameByFilter(Filter f){
		Keys k=new Keys(f);
		
		Values v=new Values(f);
		this.fc.put(k, v);
	}
	public Map<Keys,Values> getFC(){
		return this.fc;
	}
	/*
	 * 计算mismatch factor
	 */
	public double calMismatchFactor(List<Data> dats){
		double numSum=0;
		double numPassNoReq=0;
		double numBlockReq=0;
		for(Data d:dats){
			if(d.getExpandState()==0&&d.getUsageCount()==0) numPassNoReq=numPassNoReq+1;
			else if(d.getExpandState()==1&&d.getUsageCount()>0) numBlockReq=numBlockReq+1;
			numSum=numSum+1;
		}
		return 0.5*(numPassNoReq+numBlockReq)/numSum;
	}
	public long getNumOfExpandDatas() {
		return numOfExpandDatas;
	}
	public void setNumOfExpandDatas(long numOfExpandDatas) {
		this.numOfExpandDatas = numOfExpandDatas;
	}
	public double getRestSpaceRate(){
		return this.restSpace/this.fullSpace;
	}
	public void setFullSpace(double sp){
		this.fullSpace=sp;
	}
	public double getFullSpace(){
		return this.fullSpace;
	}
	public void setRestSpace(double rp){
		this.restSpace=rp;
	}
	public double getRestSpace(){
		return this.restSpace;
	}
	public double getSpaceThreshold(){
		return this.spaceThreshold;
	}
	public void setSpaceThreshold(double t){
		this.spaceThreshold=t;
	}
}
