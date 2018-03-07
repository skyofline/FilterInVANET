package core;

public class TestApplication extends Application{
	
	public String appId="test";
	@Override
	public Message handle(Message msg, DTNHost host) {
		// TODO Auto-generated method stub
		System.out.println("test application,send: "+msg.getFrom().getAddress()+"to: "+msg.getTo().getAddress());
		
		return null;
	}

	@Override
	public void update(DTNHost host) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Application replicate() {
		// TODO Auto-generated method stub
		return null;
	}

}
