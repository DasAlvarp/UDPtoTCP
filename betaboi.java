public class betaboi{
	public static void main(String[] args){
		RReceiveUDP receiver = new RReceiveUDP();
		receiver.setMode(2);
		receiver.setModeParameter(512);
		receiver.setFilename("less_important.txt");
		receiver.setLocalPort(32456);
		receiver.receiveFile();
	}
}
