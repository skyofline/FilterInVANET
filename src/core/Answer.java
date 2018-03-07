package core;

public class Answer {
	private int type;
	private Long time;
	private int level;
	private int size;
	public Answer(int type,int size,int level){
		this.type=type;
		this.time=null;
		this.level=level;
		this.size=size;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public Long getTime() {
		return time;
	}
	public void setTime(Long time) {
		this.time = time;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
}
