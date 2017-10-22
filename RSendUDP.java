import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; 
//import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class RSendUDP implements edu.utulsa.unet.RSendUDPI
{
	static final String SERVER = "localhost";
	static final int PORT = 32456;

	int mode;
	long modeParameter;
	String filename;
	long timeout;
	int localPort;
	InetSocketAddress receiver;

	public RSendUDP(){
		mode = 0;
		timeout = 100;
		localPort = 32456;
	}

	public boolean setMode(int mode)
	{
		this.mode = mode;
		return mode > 0;
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

	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	public String getFilename()
	{
		return filename;
	}

	public boolean setTimeout(long timeout)
	{
		if(timeout > 0)
		{
			this.timeout = timeout;
			return true;
		}
		else
		{
			return false;
		}
	}

	public long getTimeout()
	{
		return timeout;
	}

	public boolean setLocalPort(int port)
	{
		if(port > 10000)
		{
			localPort = port;
			return true;
		}
		else
		{
			return false;
		}
	}

	public int getLocalPort()
	{
		return localPort;
	}

	public boolean setReceiver(InetSocketAddress receiver)
	{
		this.receiver = receiver;
		return true;
	}

	public InetSocketAddress getReceiver()
	{
		return receiver;
	}

	public boolean sendFile()
	{
		try {

			byte [] buffer = ("Hello World- or rather Mauricio saying hello through UDP").getBytes();
			UDPSocket socket = new UDPSocket(23456);
			//DatagramSocket socket = new DatagramSocket(23456);
			socket.send(new DatagramPacket(buffer, buffer.length,
 				InetAddress.getByName(SERVER), PORT));
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}

	}
}
