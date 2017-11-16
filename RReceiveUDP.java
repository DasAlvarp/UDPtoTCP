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
			byte[] windowNum = ByteBuffer.allocate(4).putInt(pNum).array();
			for(int x = 0; x < 4; x++)
			{
				returnMan[x] = byteMode[x];
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
			UDPSocket socket = new UDPSocket(port);
			byte [][] window = new byte[mode][(int)modeParameter];
			boolean stillReceiving = true;
			int floor = 0;
			DatagramPacket samplePacket = null;
			int maxTop = floor + mode;
			while(stillReceiving)
			{
				int curFloor = floor;
				for(int x = curFloor; (x < curFloor + mode && x <= maxTop); x++)
				{
					int []recieved = new int[mode];
					for(int y = 0; y < mode; y++)
					{
						recieved[y] = -1;
					}
					try
					{
						DatagramPacket packet = new DatagramPacket(window[x - curFloor], window[x - curFloor].length);
						samplePacket = packet;
						//don't want a timeout for first packet, but the rest make sense.
						if(x != curFloor){
							socket.setSoTimeout(100);//recieve up to 3 packets, but need some kind of time out mechanic
						}
						socket.receive(packet);
						InetAddress client = packet.getAddress();
						System.out.println("Received'" + new String(window[x - curFloor]) + "' from " + packet.getAddress().getHostAddress() + " with sender port " + packet.getPort());
		
						//figuring out if I'm done reading.
						byte[] indexArr = new byte[4];
						byte[] buffZiseArr = new byte[4];
						for(int y = 0; y < 4; y++)
						{
							indexArr[y] = window[x - curFloor][y + 12];
							buffZiseArr[y] = window[x - curFloor][y + 16];
						}
						ByteBuffer wrap = ByteBuffer.wrap(indexArr);
						int index = wrap.getInt();
						ByteBuffer wrap2 = ByteBuffer.wrap(buffZiseArr);
						int buffsize = wrap2.getInt();
						
						//every time we get one, put it in the recieved array.
						recieved[x - curFloor] = index;

						//can't recieve more than mode packet
						maxTop = buffsize;

						//increment floor if we've recieved the next packet, plus process the array
						floor = getFloor(recieved, mode, index);
						
						if(floor > buffsize){
							System.out.println(buffsize);
							stillReceiving = false;
							break;
						}
					}
					catch(Exception e)
					{
						System.out.println("timed out");
					}
				}
				System.out.println("did a loop " + floor + ", " + maxTop);
				//floor sent as ijndex, send next #mode (going to pretend that out sliding window is size 3)
				byte[] ack = sendAck(mode, floor + 1);
				if(samplePacket != null)
				{
					socket.send(new DatagramPacket(ack, ack.length, InetAddress.getByName(samplePacket.getAddress().getHostAddress()), samplePacket.getPort()));
					System.out.println(mode + ", " + floor);
				}
				else
				{
					System.out.println("Send failed.");
					System.exit(1);
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

	private int getFloor(int[] arr, int size, int index)
	{
		//first, get the lowest that isn't -1
		int temp = 2147483647;
		for(int x = 0; x < size; x++)
		{
			if(temp > arr[x] && arr[x] <= index && arr[x] != -1)
			{
				temp = arr[x];
			}
		}

		//now that we've gotten the lowest:
		boolean noNew = false;
		while(!noNew)
		{
			noNew = true;
			for(int x = 0; x < size; x++)
			{
				if(arr[size] == temp + 1)
				{
					noNew = false;
					temp++;
				}
			}
		}
		if(temp == -1)
			return index;
		else
			return temp;
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