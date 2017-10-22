import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; //import java.net.DatagramSocket;
import java.net.InetAddress;

public class RReceiveUDP implements edu.utulsa.unet.RReceiveUDPI {
	
	int port = 32456;
	int mode;
	long modeParameter;
	String filename;

	public static void main(String[] args)
	{

	}

	public boolean setMode(int mode)
	{
		this.mode = mode;
		return this.mode > 0;
	}

	public int getMode()
	{
		return mode;
	}

	public boolean setModeParameter(long n)
	{
		modeParameter = n;
		return true;
	}
	
	public long getModeParameter()
	{
		return modeParameter;
	}
	
	public void setFilename(String fname)
	{
		filename = fname;
	}

	public String getFilename()
	{
		return filename;
	}

	public boolean setLocalPort(int port)
	{
		if(port > 10000)
		{
			this.port = port;
			return true;
		}
		else
		{
			return false;
		}
	}

	public int getLocalPort()
	{
		return port;
	}

	public boolean receiveFile()
	{
		try
		{
			byte [] buffer = new byte[11];
			UDPSocket socket = new UDPSocket(port);
			DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
			socket.receive(packet);
			InetAddress client = packet.getAddress();
			System.out.println("Received'" + new String(buffer) + "' from " + packet.getAddress().getHostAddress() + " with sender port " + packet.getPort());
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}