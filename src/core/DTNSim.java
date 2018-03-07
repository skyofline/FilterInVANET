/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;
import gui.DTNSimGUI;
import constructions.BasicRequest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ui.DTNSimTextUI;

/**
 * Simulator's main class
 */
public class DTNSim {
	/** If this option ({@value}) is given to program, batch mode and
	 * Text UI are used*/
	public static final String BATCH_MODE_FLAG = "-b";
	/** Delimiter for batch mode index range values (colon) */
	public static final String RANGE_DELIMETER = ":";

	/** Name of the static method that all resettable classes must have
	 * @see #registerForReset(String) */
	public static final String RESET_METHOD_NAME = "reset";
	/** List of class names that should be reset between batch runs */
	private static List<Class<?>> resetList = new ArrayList<Class<?>>();

	/*
	 * �洢���������Ĳ�ͬʱ�̵�λ����Ϣ
	 */
	private static Map<String,List<String>> tracks=new LinkedHashMap<>();
	public static void fillTracksByFile(String fileName){
		Map<String,List<String>> trackss=new LinkedHashMap<>();
		File file=new File(fileName);
		
	    try{
	    	BufferedInputStream fis=new BufferedInputStream(new FileInputStream(file));
	        BufferedReader reader = new BufferedReader(new InputStreamReader(fis,"utf-8"),5*1024*1024);
	        
	        String str=null;
	        while ((str=reader.readLine())!=null) {
//	        	String[]  pros=str.split(",");
//	         	if(!processDatas.keySet().contains(pros[4])){
//	           		List<String> ss=new ArrayList<>();
//	           		ss.add(str);
//	            	processDatas.put(pros[4], ss);
//	           	}else{
//	           		processDatas.get(pros[4]).add(str);
//	           	}
	        	String[] s=str.split(",");
	        	if(tracks.containsKey(s[0])){
	        		trackss.get(s[0]).add(s[1]+","+s[2]+","+s[3]+","+s[4]+","+s[5]);
	        		
	        	}else{
	        		List<String> ns=new ArrayList<String>();
	        		ns.add(s[1]+","+s[2]+","+s[3]+","+s[4]+","+s[5]);
	        		trackss.put(s[0], ns);
	        	}
	         }
	         reader.close();
	      }catch (Exception e) {
	            e.printStackTrace();
	      }
	    tracks=trackss;
	}
//	//���ڴ洢���ⲿ����od���ݼ���ȡ�Ĳ�������
//	public static Map<Integer,List<BasicRequest>> m=new LinkedHashMap<Integer,List<BasicRequest>>();
//	public static void getDataFromOD(){
//		try{
//			File f=new File("C:\\Users\\19836\\Desktop\\��������\\OD����\\test.txt");
//			BufferedReader bufReader;
//			String read;
//			bufReader=new BufferedReader(new FileReader(f));
//			while((read=bufReader.readLine())!=null){
//				String[] items=read.split(",");
//				int id=Integer.parseInt(items[0]);
//				if(!m.containsKey(id)){
//					List<BasicRequest> lb=new ArrayList<BasicRequest>();
//					BasicRequest br=new BasicRequest(id,Double.parseDouble(items[1])
//							,Double.parseDouble(items[2]),Double.parseDouble(items[3])
//							,Double.parseDouble(items[4]),Double.parseDouble(items[5])
//							,Double.parseDouble(items[5]));
//					lb.add(br);
//					m.put(id, lb);
//				}else{
//					BasicRequest br=new BasicRequest(id,Double.parseDouble(items[1])
//							,Double.parseDouble(items[2]),Double.parseDouble(items[3])
//							,Double.parseDouble(items[4]),Double.parseDouble(items[5])
//							,Double.parseDouble(items[5]));
//					List<BasicRequest> lb=m.get(id);
//					lb.add(br);
//					m.replace(id, lb);
//				}
//			}
//			bufReader.close();
//		}catch(FileNotFoundException e){
//			e.printStackTrace();
//		}catch(IOException e){
//			e.printStackTrace();
//		}
//	}
//	//���ڴ洢�ⲿ���ݼ�od���ݼ����յ�ֲ����
//	public static double plaDisCounts=249216.0;
//	public static int[][] plaDistri;
//	public static void getPlacesDistribute() throws IOException{
//		File f=new File("C:\\Users\\19836\\Desktop\\��������\\OD����\\odExch.txt");
//		BufferedReader bufread;
//		String read;
//		bufread=new BufferedReader(new FileReader(f));
//		int[][] num=new int[10][10];
//		for(int i=0;i<10;i++)
//			for (int j=0;j<10;j++)
//				num[i][j]=0;
//		double minX=1.1669846680015326;
//		double minY=0.8009045347571373;
//		double maxX=1.1975577154722363E7;
//		double maxY=2.497099654427073E7;
//		double sizeX=(maxX-minX)/10;
//		double sizeY=(maxY-minY)/10;
//		while((read=bufread.readLine())!=null){
//			String[] items=read.split(",");
//			double a=Double.parseDouble(items[1]);
//			double b=Double.parseDouble(items[2]);
//			double c=Double.parseDouble(items[3]);
//			double d=Double.parseDouble(items[4]);
//			double ix=(c-minX)/sizeX;
//			double iy=(d-minY)/sizeY;
//			int i=(int)ix;
//			int j=(int)iy;
//			if(i>9) i=9;
//			if(j>9) j=9;
//			num[i][j]++;
//		}
//		bufread.close();
//		plaDistri=num;
//	}
	/**
	 * Starts the user interface with given arguments.
	 * If first argument is {@link #BATCH_MODE_FLAG}, the batch mode and text UI
	 * is started. The batch mode option must be followed by the number of runs,
	 * or a with a combination of starting run and the number of runs,
	 * delimited with a {@value #RANGE_DELIMETER}. Different settings from run
	 * arrays are used for different runs (see
	 * {@link Settings#setRunIndex(int)}). Following arguments are the settings
	 * files for the simulation run (if any). For GUI mode, the number before
	 * settings files (if given) is the run index to use for that run.
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		boolean batchMode = false;
		int nrofRuns[] = {0,1};
		String confFiles[];
		int firstConfIndex = 0;
		int guiIndex = 0;

		/* set US locale to parse decimals in consistent way */
		java.util.Locale.setDefault(java.util.Locale.US);

		if (args.length > 0) {
			if (args[0].equals(BATCH_MODE_FLAG)) {
				batchMode = true;
                if (args.length == 1) {
                    firstConfIndex = 1;
                }
                else {
                    nrofRuns = parseNrofRuns(args[1]);
                    firstConfIndex = 2;
                }
			}
			else { /* GUI mode */
				try { /* is there a run index for the GUI mode ? */
					guiIndex = Integer.parseInt(args[0]);
					firstConfIndex = 1;
				} catch (NumberFormatException e) {
					firstConfIndex = 0;
				}
			}
			confFiles = args;
		}
		else {
			confFiles = new String[] {null};
		}
		String testConfFiles[]=new String[1];
		testConfFiles[0]="final_setting.txt";//final_settings.txt
		initSettings(testConfFiles,0);
		
		/*
		 * ��ȡ�ⲿ����ΪONEģ�������켣��Ϣ
		 */
		String fileName="C:\\Users\\19836\\Desktop\\��������\\����2014��7��\\�켣\\data_all_sorted.txt";
		fillTracksByFile(fileName);
		//initSettings(confFiles, firstConfIndex);

//		//��ȡ�ⲿ���ݼ�od���ݼ������洢��m��
//		getDataFromOD();
//		//��od�����е��յ�ֲ�
//		try {
//			getPlacesDistribute();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		if (batchMode) {
			long startTime = System.currentTimeMillis();
			for (int i=nrofRuns[0]; i<nrofRuns[1]; i++) {
				print("Run " + (i+1) + "/" + nrofRuns[1]);
				Settings.setRunIndex(i);
				resetForNextRun();
				new DTNSimTextUI().start();
			}
			double duration = (System.currentTimeMillis() - startTime)/1000.0;
			print("---\nAll done in " + String.format("%.2f", duration) + "s");
		}
		else {
			Settings.setRunIndex(guiIndex);
			new DTNSimGUI().start();
		}
	}

	/**
	 * Initializes Settings
	 * @param confFiles File name paths where to read additional settings
	 * @param firstIndex Index of the first config file name
	 */
	private static void initSettings(String[] confFiles, int firstIndex) {
		int i = firstIndex;

        if (i >= confFiles.length) {
            return;
        }
        
		try {
			Settings.init(confFiles[i]);
			for (i=firstIndex+1; i<confFiles.length; i++) {
				Settings.addSettings(confFiles[i]);
			}
		}
		catch (SettingsError er) {
			try {
				Integer.parseInt(confFiles[i]);
			}
			catch (NumberFormatException nfe) {
				/* was not a numeric value */
				System.err.println("Failed to load settings: " + er);
				System.err.println("Caught at " + er.getStackTrace()[0]);
				System.exit(-1);
			}
			System.err.println("Warning: using deprecated way of " +
					"expressing run indexes. Run index should be the " +
					"first option, or right after -b option (optionally " +
					"as a range of start and end values).");
			System.exit(-1);
		}
	}

	/**
	 * Registers a class for resetting. Reset is performed after every
	 * batch run of the simulator to reset the class' state to initial
	 * state. All classes that have static fields that should be resetted
	 * to initial values between the batch runs should register using
	 * this method. The given class must have a static implementation
	 * for the resetting method (a method called {@value #RESET_METHOD_NAME}
	 * without any parameters).
	 * @param className Full name (i.e., containing the packet path)
	 * of the class to register. For example: <code>core.SimClock</code>
	 */
	public static void registerForReset(String className) {
		Class<?> c = null;
		try {
			c = Class.forName(className);
			c.getMethod(RESET_METHOD_NAME);
		} catch (ClassNotFoundException e) {
			System.err.println("Can't register class " + className +
					" for resetting; class not found");
			System.exit(-1);

		}
		catch (NoSuchMethodException e) {
			System.err.println("Can't register class " + className +
			" for resetting; class doesn't contain resetting method");
			System.exit(-1);
		}
		resetList.add(c);
	}

	/**
	 * Resets all registered classes.
	 */
	private static void resetForNextRun() {
		for (Class<?> c : resetList) {
			try {
				Method m = c.getMethod(RESET_METHOD_NAME);
				m.invoke(null);
			} catch (Exception e) {
				System.err.println("Failed to reset class " + c.getName());
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	/**
	 * Parses the number of runs, and an optional starting run index, from a
	 * command line argument
	 * @param arg The argument to parse
	 * @return The first and (last_run_index - 1) in an array
	 */
	private static int[] parseNrofRuns(String arg) {
		int val[] = {0,1};
		try {
			if (arg.contains(RANGE_DELIMETER)) {
				val[0] = Integer.parseInt(arg.substring(0,
						arg.indexOf(RANGE_DELIMETER))) - 1;
				val[1] = Integer.parseInt(arg.substring(arg.
						indexOf(RANGE_DELIMETER) + 1, arg.length()));
			}
			else {
				val[0] = 0;
				val[1] = Integer.parseInt(arg);
			}
		} catch (NumberFormatException e) {
			System.err.println("Invalid argument '" + arg + "' for" +
					" number of runs");
			System.err.println("The argument must be either a single value, " +
					"or a range of values (e.g., '2:5'). Note that this " +
					"option has changed in version 1.3.");
			System.exit(-1);
		}

		if (val[0] < 0) {
			System.err.println("Starting run value can't be smaller than 1");
			System.exit(-1);
		}
		if (val[0] >= val[1]) {
			System.err.println("Starting run value can't be bigger than the " +
					"last run value");
			System.exit(-1);
		}

		return val;
	}

	/**
	 * Prints text to stdout
	 * @param txt Text to print
	 */
	private static void print(String txt) {
		System.out.println(txt);
	}

	public static Map<String,List<String>> getTracks() {
		return tracks;
	}

	public static void setTracks(Map<String,List<String>> tracks) {
		DTNSim.tracks = tracks;
	}
}