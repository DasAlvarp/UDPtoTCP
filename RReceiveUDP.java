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
		try
		{
			int startPacket = 0;

			UDPSocket[] socket = new UDPSocket(port);
			byte [] window = new byte[mode][modeParameter];
			boolean stillReceiving = true;
			int floor = 0;
			int ceil = floor + mode;
			while(stillReceiving)
			{
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				InetAddress client = packet.getAddress();
				System.out.println("Received'" + new String(buffer) + "' from " + packet.getAddress().getHostAddress() + " with sender port " + packet.getPort());
				
				//figuring out if I'm done reading.
				byte[] indexArr = new byte[4];
				byte[] buffZiseArr = new byte[4];
				for(int x = 0; x < 4; x++)
				{
					indexArr[x] = buffer[x + 12];
					buffZiseArr[x] = buffer[x + 16];
				}
				ByteBuffer wrap = ByteBuffer.wrap(indexArr);
				int index = wrap.getInt();
				ByteBuffer wrap2 = ByteBuffer.wrap(buffZiseArr);
				int buffsize = wrap2.getInt();
				byte[] ack = sendAck(mode, index);
				socket.send(new DatagramPacket(ack, ack.length, InetAddress.getByName(packet.getAddress().getHostAddress()), packet.getPort()));
				System.out.println(index + ", " + buffsize);
				if(index == buffsize - 1){
					System.out.println(buffsize);
					stillReceiving = false;
				}
			}
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	//reading what I get
	private boolean recieveStopAndWait()
	{
		try
		{
			int startPacket = 0;

			byte [] buffer = new byte[(int)modeParameter];
			UDPSocket socket = new UDPSocket(port);
			boolean stillReceiving = true;
			while(stillReceiving)
			{
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				InetAddress client = packet.getAddress();
				System.out.println("Received'" + new String(buffer) + "' from " + packet.getAddress().getHostAddress() + " with sender port " + packet.getPort());
				
				//figuring out if I'm done reading.
				byte[] indexArr = new byte[4];
				byte[] buffZiseArr = new byte[4];
				for(int x = 0; x < 4; x++)
				{
					indexArr[x] = buffer[x + 12];
					buffZiseArr[x] = buffer[x + 16];
				}
				ByteBuffer wrap = ByteBuffer.wrap(indexArr);
				int index = wrap.getInt();
				ByteBuffer wrap2 = ByteBuffer.wrap(buffZiseArr);
				int buffsize = wrap2.getInt();
				byte[] ack = sendAck(0, index % 2);
				socket.send(new DatagramPacket(ack, ack.length, InetAddress.getByName(packet.getAddress().getHostAddress()), packet.getPort()));
				System.out.println(index + ", " + buffsize);
				if(index == buffsize - 1){
					System.out.println(buffsize);
					stillReceiving = false;
				}
			}
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}