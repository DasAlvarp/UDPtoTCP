import java.net.InetSocketAddress;

public class bigboss{
	public static void main(String[] args){
		RSendUDP sender = new RSendUDP();
		sender.setMode(1);
		sender.setModeParameter(40);
		sender.setTimeout(10000);
		sender.setFilename("important.txt");
		sender.setLocalPort(23456);
		sender.setReceiver(new InetSocketAddress("localhost", 32456));
		sender.sendFile();
	}
}