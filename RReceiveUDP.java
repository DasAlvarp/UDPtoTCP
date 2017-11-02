import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; //import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class RReceiveUDP implements edu.utulsa.unet.RReceiveUDPI {
	
	int port = 32456;
	int mode;
	long modeParameter;
	String filename;

	public RReceiveUDP()
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
		if(mode == 0)
		{
			return recieveStopAndWait();
		}
		else
		{
			return recieveWindow();
		}
	}

	private byte[] sendAck(int mode, int pNum)
	{
		if(mode == 0)
		{
			byte[] returnMan = new byte[5];
			byte [] byteMode = ByteBuffer.allocate(4).putInt(mode).array();
			for(int x = 0; x < 4; x++)
			{
				returnMan[x] = byteMode[x];
			}
			byte[] byte2 = ByteBuffer.allocate(4).putInt(pNum).array();
			returnMan[4] = byte2[3];//kinda weird, but mod 2 and gets teh 1 or 0 for stop and wait weirdness
			return returnMan;
		}
		else
		{
			byte[] returnMan = new byte[8];
			byte[] byteMode = ByteBuffer.allocate(4).putInt(mode).array();
			for(int x = 0; x < 4; x++)
			{
				returnMan[x] = byteMode[x];
			}
			byte[] windowNum = ByteBuffer.allocate(4).putInt(pNum).array();
			for(int x = 0; x < 4; x++)
			{
				returnMan[x + 4] = windowNum[x];
			}
			return returnMan;
		}
	}

	//reading with sliding window
	private boolean recieveWindow()
	{
		//stub
		return true;
	}

	//reading what I get
	private boolean recieveStopAndWait()
	{
		try
		{
			int startPacket = 0;

			byte [] buffer = new byte[11];
			UDPSocket socket = new UDPSocket(port);
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
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