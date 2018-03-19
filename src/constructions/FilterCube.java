package constructions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import core.Cloud;
import core.DTNHost;
import core.MessageCenter;
import core.SimClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class FilterCube {
	/*
	 * ���ȶ���filter cube�е�key�ࡢvalue���split��
	 */
	
	
	private Map<Keys,Values> fc=new LinkedHashMap<Keys,Values>();

	public FilterCube(){
		
	}
	/*
	 * ����洢�������
	 */
	public double fullSpace=5*1024*1024*1024;
	private double restSpace=5*1024*1024*1024;//��¼filter cube�е�ʣ����ô洢�ռ䣬��ʼĬ��ֵΪ1GB
	
	private double updateThreshold1=0.5;
	private double updateThreshold2=1;
	//�жϴ洢�ռ��Ƿ���µ���ֵ
	private double spaceThreshold=0.05;
	/*
	 * ����洢������ϵ����
	 */
	private double numOfData=0;
	private double numOfRequest=0;
	private double numOfFilter=0;
	private long numOfExpandDatas=0;
	/*
     * ����dimensional split factor��balanced factor,��ʱ�ٶ�Ϊ0.5
     */
    private double balanceFactor=0.5;
	/*
	 *��һ��������ӵ�filter cube��,ͬʱ�ж��Ƿ񴫲�
	 */
    public void putData(Data d,DTNHost dtn){
  		Set<Keys> ks=this.fc.keySet();
  		if(ks.size()==0) System.out.println(dtn.getName()+"Filter cube δ��������Ҫ����������bug");
//    	System.out.println(dtn.getName()+"�е�filter cube�Ĳ㼶����"+ks.size());
//  		System.out.println(d.toString());
    	int i=0;
  		for(Keys k:ks){
    		if(k.inRange(d)){
    			//�������ǰ����filter �ж����������Ƿ��ϴ�״̬
    			for(Filter f:this.fc.get(k).getFilters()){
    				if(f.judgeData(d)) f.resetDataStatus(d);
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				this.updateDatas();
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				this.clearDatas();
    			}
    			this.fc.get(k).addData(d);
    			//�������ʱ����filter cube��ʣ��ռ�
    			this.restSpace=this.restSpace-d.getSize();
    			//������ݺ��жϳ��Խ������ϴ�
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
    			break;
    		}else{
    			i++;
    		}
    	}
  		if(i==ks.size()){
  			System.out.println(dtn.getName()+"��filter cube�в�������ʧ�ܣ�����");
  			System.out.println("ʧ�����ݣ�"+d.toString());
  		}
  		else
  			this.numOfData=this.numOfData+1;
//    	System.out.println("���ݴ�СΪ��"+d.getSize()
//    		+"��"+dtn.getName()+"���ǰʣ���СΪ��"+this.getRestSpace()
//    		+"����Ӻ��ʣ���СΪ��"+this.getRestSpace());
    }
    /*
	 *��һ��������ӵ�filter cube��,���ж��Ƿ񴫲�
	 */
    public void putDataForCloud(Data d){
  		Set<Keys> ks=this.fc.keySet();
  		if(ks.size()==0) System.out.println("Cloud Filter cube δ��������Ҫ����������bug");
    	int i=0;
  		for(Keys k:ks){
    		
    		if(k.inRange(d)){
    			//�������ǰ����filter �ж����������Ƿ��ϴ�״̬
    			for(Filter f:this.fc.get(k).getFilters()){
    				if(f.judgeData(d)) f.resetDataStatus(d);
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				System.out.println("Cloud���ڸ�������");
    				this.updateDatas();
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				this.clearDatas();
    			}
    			this.fc.get(k).addData(d);
    			//�������ʱ����filter cube��ʣ��ռ�
    			System.out.print("Cloud�������ǰ��ʣ��ռ�"+this.restSpace);
    			this.restSpace=this.restSpace-d.getSize();
    			System.out.println("����Ӻ��ʣ��ռ�Ϊ"+this.restSpace);
    			if(d.getExpandState()==0&&d.getUsageCount()==0) this.fc.get(k).getFilters().get(0).addUnusedPassDatas(1);
    			if(d.getExpandState()==1&&d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedBlockDatas(1);
    			if(d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedDatas(1);
    			break;
    		}else{
    			i++;
    		}
    	}
  		if(ks.size()==i){
  			System.out.println("Cloud��filter cube�в�������ʧ��");
  			System.out.println("���ݽṹ��"+d.toString());
  			System.out.println("keyset�ṹ��"+ks.size());
  			for(Keys k:ks){
  				if(k.inRange(d)) System.out.println(k.toString()+"inRandge");
  				else System.out.println(k.toString()+"not in range");
  			}
  		}
  		else {
  			System.out.println("�ɹ���������");
  			this.numOfData=this.numOfData+1;
  		}
    	
    }
    /*
	 * ��filter cube�����request
	 */
	public void putRequest(Request r){
		Set<Keys> ks=this.fc.keySet();
    	for(Keys k:ks){
    		
    		if(k.inRange(r)){
    			this.fc.get(k).addRequest(r);
    			break;
    		}
    	}
    	this.numOfRequest=this.numOfRequest+1;
	}
    //����filter cube��ÿһ�е�filter
	public void update(){
		double beginTime=SimClock.getTime();
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		Set<Keys> k=this.fc.keySet();
		Iterator<Keys> it=k.iterator();
		while(it.hasNext()){
			Keys nk=(Keys) it.next();
			Values v=this.fc.get(nk);
			Filter f=v.getFilters().get(0);//��ȡ��һ��filter
			//���filter����basicFilter,����м����жϸ���״̬����Ϊbasic
			if(f.getBasicStatus()==0){
				double mf=f.getMistakeFactor(v.getDatas());
				if(mf<this.getUpdateThreshold1()){
//					double radioCost=f.calRadioCost(v.getDatas(),v.getRequests());
					f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
				}else{
//					FilterCube newFilterCube=new FilterCube();
//					newFilterCube.addFilter(f);
					//����basic filter ����ʼʱ��
					f.setBasicTime(SimClock.getTime());
					//����filterΪbasic filter
					f.setBasicStatus(1);
				}
			}else{
				//��basicFilter���д���
				if(Timer.judgeFilter(f, f.getPeriodTime())){
					//���ȼ���avg�����ں������������Դ���������ݹ��
					double misDAF=0;
					double sum=0;
					for(Data d:v.getDatas()){
						if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
						sum=sum+1;
					}
					double avg=misDAF/sum;
					//����mismatch factor
					double mismatchs=this.calMismatchFactor(v.getDatas());
					double er=avg/mismatchs;
					if(er>this.getUpdateThreshold2()){
						f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
						//��ʱҪ��basic״̬ȡ��
						f.setBasicStatus(0);
					}else{
						//��filter��Ϊmore״̬����ʱû��Ҫ���ݲ����в���
						//ȡ��filter��basic״̬
						f.setBasicStatus(0);
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
				    		for(int j=1;j<=FilterCube.getMaxSplits(dim);j++){
				    			double dimSplitFac=this.getDimSplitFactor( dim, j);
				    			if(dimSplitFac<min[i]){
				    				min[i]=dimSplitFac;
				    				split[i]=j;
				    			}
				    		}
				    		Map<Keys,Values> newss=this.splitDimension(nk, dim, split[i]);
				    		//����зֺ��filter��������һ�����������з֣�������µķ�Ƭ��ɾ���ɵķ�Ƭ
				    		if(newss.size()>1){
				    			adds.putAll(newss);
				    			//������ԭ�е�keys��ӵ�delKeys��������ɾ����
				    			delKeys.add(nk);
				    		}
				    	}
					}
				}
			}
			
		}
		//ɾ�����зֵķ�Ƭ����ӷֳ��ķ�Ƭ
		if(delKeys.size()>0){
			for(Keys kk:delKeys){
				this.fc.remove(kk);
			}
		}
		if(adds.size()>0){
			this.fc.putAll(adds);
		}
			
		MessageCenter.filterCubeUpdateTime=SimClock.getTime()-beginTime;
		MessageCenter.filterCubeUpdates=MessageCenter.filterCubeUpdates+1;
		System.out.println("����ʱ��Ϊ��"+MessageCenter.filterCubeUpdateTime+"�����´���Ϊ��"+MessageCenter.filterCubeUpdates);
	}
	/*
	 * ����filter cube�е�ÿһ�����ݣ��ж������ɾ��
	 */
	public void updateDatas(){
		Set<Keys>k= this.fc.keySet();
		Iterator<Keys> it=k.iterator();
		while(it.hasNext()){
			Keys nk=(Keys) it.next();
			Values v=this.fc.get(nk);
			List<Data> datas=v.getDatas();
			List<Data> dels=new ArrayList<Data>();
			double releaseSpace=0;
			double delNum=0;
			for(Data d:datas){
				/*
				 * ������ʱʹ������ʱ�������ʹ�ô�����Ϊɾ�����ݵ�����
				 */
				
				if((SimClock.getTime()-d.getTime()>3600&&d.getExpandState()==0)||(SimClock.getTime()-d.getTime()>7200&&d.getUsageCount()<5)){
					dels.add(d);
					releaseSpace=releaseSpace+d.getSize();
					delNum=delNum+1;
					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
				}
				/*
				 * �����������ϵĴ���
				 */
				
			}
			if(dels.size()>0){
				datas.removeAll(dels);
				System.out.println("����ɾ�����ݡ�������");
			}
			this.numOfData=this.numOfData-delNum;
			this.restSpace=this.restSpace+releaseSpace;
		}
	}
	/*
	 * ɾ���������ݣ����޷�ɾ������ʱ������ɾ���߶�ɾ����������
	 */
	public void clearDatas(){
		Set<Keys>k= this.fc.keySet();
		Iterator<Keys> it=k.iterator();
		while(it.hasNext()){
			Keys nk=(Keys) it.next();
			Values v=this.fc.get(nk);
			List<Data> datas=v.getDatas();
			List<Data> dels=new ArrayList<Data>();
			double releaseSpace=0;
			double delNum=0;
			for(Data d:datas){
				/*
				 * ������ʱʹ������ʱ����Ϊɾ�����ݵ�����
				 */
				
				if(SimClock.getTime()-d.getTime()>1800||d.getUsageCount()<5){
					dels.add(d);
					releaseSpace=releaseSpace+d.getSize();
					delNum=delNum+1;
					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
				}
				/*
				 * �����������ϵĴ���
				 */
				
			}
			if(dels.size()>0){
				datas.removeAll(dels);
				System.out.println("����ɾ�����ݡ�������");
			}
			this.numOfData=this.numOfData-delNum;
			this.restSpace=this.restSpace+releaseSpace;
		}
	}
//	/*
//	 * �����ж�data��״̬�������ݵ�ǰDTNHost�����rsu�������ȣ�
//	 * ������
//	 * ���д���
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
//					//����ýڵ���RSU�ڵ�
//					if(dtn.getType()==1){
//						dtn.uploadDataToCloud(d);
//					}else if(dtn.getType()==0){
//						//����ڵ��ǳ����ڵ�
//						dtn.uploadDataToEdge(d);
//					}
//				}
//			}
//			
//		}
//	}
//	/*
//	 * �����ж�data��״̬����cloud�˵ľ��Ĵ���
//	 * ������
//	 * ���д���
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
	 * ��DTNHost(Edge��Car)
	 * ��request���д����ҳ�filtercube���ܹ������request������
	 * ͬʱ��request�������ݵ�ʹ��������������ǣ������������ݴ���״̬
	 * ��������Ҫ���д���
	 */
	public List<Data> answerRequest(Request r,DTNHost dtn){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		List<Data> res=new ArrayList<Data>();
		Set<Keys> ks=this.fc.keySet();
    	for(Keys k:ks){
    		Values v=this.fc.get(k);
    		Filter f=this.fc.get(k).getFilters().get(0);
    		if(f.judgeRequest(r)){
    			List<Data> s=this.fc.get(k).getDatas();
    			for(Data t:s){
    				if(r.judgeData(t)) {
    					t.addUsageCount();//�������ʹ�ô���
    					int sign=0;//�ڴ˴����ڱ���Ƿ�������Ƿ��Ǵ���״̬
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
    						f.updateStatusByRadioCost(v.getDatas(), v.getRequests());
    					}
    					//�ж��Ƿ��ϴ�
    					if(t.getExpandState()==0&&sign==0){
    					
    						if(dtn.getType()==0) dtn.uploadDataToEdge(t);
    						else if(dtn.getType()==1) dtn.uploadDataToCloud(t);
    					}
    					res.add(t);
    				}
    			}
    			//�����filter��һ��basic filter����Ҫ�ж��Ƿ��ڿ���ʱ����
    			if(f.getBasicStatus()==1){
    				//��basicFilter���д���
    				if(Timer.judgeFilter(f, f.getPeriodTime())){
    					//���ȼ���avg�����ں������������Դ���������ݹ��
    					double misDAF=0;
    					double sum=0;
    					for(Data d:v.getDatas()){
    						if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
    						sum=sum+1;
    					}
    					double avg=misDAF/sum;
    					//����mismatch factor
    					double mismatchs=this.calMismatchFactor(v.getDatas());
    					double er=avg/mismatchs;
    					if(er>this.getUpdateThreshold2()){
//    						double radioCost=f.calRadioCost(v.getDatas(),v.getRequests());
    						f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
    						//��ʱҪ��basic״̬ȡ��
    						f.setBasicStatus(0);
    					}else{
    						//��filter��Ϊmore״̬����ʱû��Ҫ���ݲ����в���
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
    				    		for(int j=1;j<=FilterCube.getMaxSplits(dim);j++){
    				    			double dimSplitFac=this.getDimSplitFactor( dim, j);
    				    			if(dimSplitFac<min[i]){
    				    				min[i]=dimSplitFac;
    				    				split[i]=j;
    				    			}
    				    		}
    				    		Map<Keys,Values> newss=this.splitDimension(k, dim, split[i]);
    				    		//����зֺ��filter��������һ�����������з֣�������µķ�Ƭ��ɾ���ɵķ�Ƭ
    				    		if(newss.size()>1){
    				    			adds.putAll(newss);
    				    			//������ԭ�е�keys��ӵ�delKeys��������ɾ����
    				    			delKeys.add(k);
    				    		}
    				    	}
    					}
    				}
    			}
    			if(res.size()>0) break;
    		}
    	}
    	//ɾ�����зֵķ�Ƭ����ӷֳ��ķ�Ƭ
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
	 * ��request���д����ҳ�filtercube���ܹ������request������
	 * ͬʱ��request�������ݵ�ʹ��������������ǣ������������ݴ���״̬
	 * ��������Ҫ���д���
	 */
	public List<Data> answerRequest(Request r,Cloud dtn){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		List<Data> res=new ArrayList<Data>();
		Set<Keys> ks=this.fc.keySet();
    	for(Keys k:ks){
    		Values v=this.fc.get(k);
    		Filter f=this.fc.get(k).getFilters().get(0);
    		if(f.judgeRequest(r)){
    			List<Data> s=this.fc.get(k).getDatas();
    			for(Data t:s){
    				if(r.judgeData(t)) {
    					t.addUsageCount();//�������ʹ�ô���
    					int sign=0;//�ڴ˴����ڱ���Ƿ�������Ƿ��Ǵ���״̬
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
    						f.updateStatusByRadioCost(v.getDatas(), v.getRequests());
    					}
    					res.add(t);
    				}
    			}
    			//�����filter��һ��basic filter����Ҫ�ж��Ƿ��ڿ���ʱ����
    			if(f.getBasicStatus()==1){
    				//��basicFilter���д���
    				if(Timer.judgeFilter(f, f.getPeriodTime())){
    					//���ȼ���avg�����ں������������Դ���������ݹ��
    					double misDAF=0;
    					double sum=0;
    					for(Data d:v.getDatas()){
    						if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
    						sum=sum+1;
    					}
    					double avg=misDAF/sum;
    					//����mismatch factor
    					double mismatchs=this.calMismatchFactor(v.getDatas());
    					double er=avg/mismatchs;
    					if(er>this.getUpdateThreshold2()){
    						f.updateStatusByRadioCost(v.getDatas(),v.getRequests());
    						//��ʱҪ��basic״̬ȡ��
    						f.setBasicStatus(0);
    					}else{
    						//��filter��Ϊmore״̬����ʱû��Ҫ���ݲ����в���
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
    				    		for(int j=1;j<=FilterCube.getMaxSplits(dim);j++){
    				    			double dimSplitFac=this.getDimSplitFactor( dim, j);
    				    			if(dimSplitFac<min[i]){
    				    				min[i]=dimSplitFac;
    				    				split[i]=j;
    				    			}
    				    		}
    				    		Map<Keys,Values> newss=this.splitDimension(k, dim, split[i]);
    				    		//����зֺ��filter��������һ�����������з֣�������µķ�Ƭ��ɾ���ɵķ�Ƭ
    				    		if(newss.size()>1){
    				    			adds.putAll(newss);
    				    			//������ԭ�е�keys��ӵ�delKeys��������ɾ����
    				    			delKeys.add(k);
    				    		}
    				    	}
    					}
    				}
    			}
    			if(res.size()>0) break;
    		}
    	}
    	//ɾ�����зֵķ�Ƭ����ӷֳ��ķ�Ƭ
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
		Map<Keys,Values> addKV=new LinkedHashMap<>();
		if(num>1){
//			List<Keys> del=new ArrayList<>();
			if(k.getKey().containsKey(dimension)){
				
				//��dimensionά�Ƚ����з�,ƽ���зֳ�num��filter
				//���Ȼ�õ�ǰfilter�ڸ�ά�ȵĿ��ֵ��Ȼ������з�
				double max=k.getKey().get(dimension).getMaxBord()-k.getKey().get(dimension).getMinBord();
				double aver=max/num;
				for(int i=0;i<num;i++){
					Keys nKey=new Keys(k);
					nKey.changeDimensionValue(dimension, k.getKey().get(dimension).getMinBord()+i*aver,k.getKey().get(dimension).getMinBord()+(i+1)*aver);
					Values newValues=new Values(this.fc.get(k));
					//�����newValues�е����ݣ�Ȼ���ԭ����������ѡ���ϵķ���newValues��
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
					//���зֺ��filter���뵽filtercube��
					addKV.put(nKey, newValues);
				}
			}
			return addKV;
		}else{
			addKV.put(k, this.fc.get(k));
			return addKV;
		}
	}
	/*
	 * ��filter cube�����һ��filter
	 */
	public void addFilter(Filter f){
		for(Keys k:this.fc.keySet()){
			if(k.inRange(f)) this.fc.get(k).addFilter(f);
			this.numOfFilter=this.numOfFilter+1;
		}
	}
	/*
	 * �����ȡһ��ά�ȵ�����Ƭ����
	 * ������ʱʹ�õ�·����ͼ������type==1�Ƚ��й���
	 */
	public static int getMaxSplits(String dimension){
		if(dimension.equals("Weather")) return 3;
		else if(dimension.equals("Time")) return 5;
		else if(dimension.equals("TrafficCondition")) return 3;
		else if(dimension.equals("Size")) return 1;
		else return 3;
	}
	/*
	 * ��������ȡfilter cubeĳ��ά�ȵ�mis(f,s)ֵ
	 */
	public int getSumOfMis(String dimension,double num){
		double max=this.getMaxNumInDim(dimension);
		double aver=max/num;
		int sum=0;
		for(int i=0;i<num;i++){
    		int numOfOpen=0,numOfClose=0;
    		Iterator<Keys> it=this.fc.keySet().iterator();
    		while(it.hasNext()){
    			Keys k=(Keys) it.next();
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
	 * ����һ��ά�ȵ����ֵ�������ڼ�����η�Ƭ
	 * ������ʱʹ�õ�·����ͼ������type==1�Ƚ��й���
	 */
	public double getMaxNumInDim(String dimension){
		if(dimension.equals("Weather")) return 2;
		else if(dimension.equals("Time")) return 4;
		else if(dimension.equals("TrafficCondition")) return 2;
		else return 2*1024*1024;
	}
	/*
	 * ���filter cube �е�dimension �б�
	 */
	public List<String> getDimensions(){
		List<String> l=new ArrayList<String>();
		l.add("Weather");
		l.add("Time");
		l.add("TrafficCondition");
		l.add("size");
		return l;
	}
	/*
	 * �����filter cube��ĳһ�еĸ���
	 * 
	 */
	public void copyFromKeyValue(Keys k,Values v){
		
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
	public double getRestSpace(){
		return this.restSpace;
	}
	public void setRestSpace(double s){
		this.restSpace=s;
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
		System.out.println("��filter cube��size��СΪ��"+this.fc.size());
		for(Keys k:this.fc.keySet()){
			System.out.println(k.toString());
		}
	}
	/*
	 * Ϊfiltercube���ά�ȿ�ܣ�����ԭʼfilter��
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
	 * ����mismatch factor
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
}
