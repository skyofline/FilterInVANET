package constructions;

public class BasicRequest {
	private int carId;
	private double fromX;
	private double fromY;
	private double toX;
	private double toY;
	private double fromT;
	private double toT;
	public BasicRequest(int id,double fx,double fy,double tx,double ty,double ft,double tt){
		this.carId=id;
		this.fromX=fx;
		this.fromY=fy;
		this.toX=tx;
		this.toY=ty;
		this.fromT=ft;
		this.toT=tt;
	}
	public int getCarId() {
		return carId;
	}
	public void setCarId(int carId) {
		this.carId = carId;
	}
	public double getFromX() {
		return fromX;
	}
	public void setFromX(double fromX) {
		this.fromX = fromX;
	}
	public double getFromY() {
		return fromY;
	}
	public void setFromY(double fromY) {
		this.fromY = fromY;
	}
	public double getToX() {
		return toX;
	}
	public void setToX(double toX) {
		this.toX = toX;
	}
	public double getToY() {
		return toY;
	}
	public void setToY(double toY) {
		this.toY = toY;
	}
	public double getFromT() {
		return fromT;
	}
	public void setFromT(double fromT) {
		this.fromT = fromT;
	}
	public double getToT() {
		return toT;
	}
	public void setToT(double toT) {
		this.toT = toT;
	}
}
