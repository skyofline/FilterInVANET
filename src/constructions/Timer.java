package constructions;

import core.SimClock;

public class Timer {
	public static boolean judgeFilter(Filter f,double time){
		if((SimClock.getTime()-f.getBasicTime())>time) return true;
		else return false;
	}
}
