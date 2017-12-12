import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; //import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public class RReceiveUDP implements edu.utulsa.unet.RReceiveUDPI {
	
	int port = 123;
	int mode;
	long modeParameter;
	String filename;
	byte[] file;
	byte[] fileBits;

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
		fileBits = new byte[4];
		try
		{
			UDPSocket socket = new UDPSocket(port);
			byte [][] window = new byte[mode][(int)modeParameter];
			boolean stillReceiving = true;
			int floor = 0;
			DatagramPacket samplePacket = null;
			boolean samplePacketed = false;
			int maxTop = floor + mode;
			while(stillReceiving)
			{
				int curFloor = floor;
				int []recieved = new int[mode];

				for(int x = curFloor; (x < curFloor + mode && x < maxTop); x++)
				{
					for(int y = 0; y < mode; y++)
					{
						recieved[y] = -1;
					}

					try
					{

						DatagramPacket packet = new DatagramPacket(window[x - curFloor], window[x - curFloor].length);
						//don't want a timeout for first packet, but the rest make sense.
						if(x != curFloor)
						{
							socket.setSoTimeout(100);//recieve up to 3 packets, but need some kind of time out mechanic
						}
						socket.receive(packet);
						InetAddress client = packet.getAddress();
						port = packet.getPort();
						System.out.println("Received'" + new String(window[x - curFloor]) + "' from " + packet.getAddress().getHostAddress() + " with sender port " + packet.getPort());
						if(!samplePacketed){
							samplePacketed = true;
							samplePacket = packet;
						}
						//figuring out if I'm done reading.
						byte[] indexArr = new byte[4];
						byte[] buffSizeArr = new byte[4];
						for(int y = 0; y < 4; y++)
						{
							indexArr[y] = window[x - curFloor][y + 12];
							buffSizeArr[y] = window[x - curFloor][y + 16];
						}
						ByteBuffer wrap = ByteBuffer.wrap(indexArr);
						int index = wrap.getInt();
						ByteBuffer wrap2 = ByteBuffer.wrap(buffSizeArr);
						int buffsize = wrap2.getInt();
						
						//every time we get one, put it in the recieved array.
						recieved[x - curFloor] = index;

						//can't recieve more than mode packet
						maxTop = buffsize;

						//increment floor if we've recieved the next packet, plus process the array
						floor = getFloor(recieved, mode, curFloor);
						
						updateFile(window[x - curFloor]);
	
						if(floor >= buffsize - 1)
						{
							System.out.println(buffsize + "BOOM");
							stillReceiving = false;
							x = maxTop + 1;
							break;
						}

					}
					catch(Exception e)
					{
						System.out.println("timed out");
						e.printStackTrace();
					}
				}
				System.out.println("did a loop " + floor + ", " + maxTop);
				if(floor + 1 >= maxTop){
					stillReceiving = false;
				}
				//floor sent as index, send next #mode (going to pretend that out sliding window is size 3)
				byte[] ack = sendAck(mode, floor + 1);
				if(samplePacket != null)
				{
					socket.send(new DatagramPacket(ack, ack.length, InetAddress.getByName(samplePacket.getAddress().getHostAddress()), samplePacket.getPort()));
					//System.out.println(mode + ", " + floor);
				}
				else
				{
					System.out.println("Send failed.");
					System.exit(1);
				}
			}
			makeFile();
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
		int pastIndex = 0;//number greater than the index

		for(int x = 0; x < size; x++)
		{
			if(arr[x] != -1)
			{
				if(arr[x] > index)
				{
					pastIndex++;
				}
			}
		}
		
		//gets rid of annoying edge case.
		if(pastIndex == 0)
		{
			return index;
		}

		int[] posIndex = new int[pastIndex];
		int count2 = 0;
		for(int x = 0; x < size; x++)
		{
			if(arr[x] > index){
				posIndex[count2] = arr[x];
				count2++;
			}
		}

		//now we're down to only relevant stuff.
		boolean noNew = false;
		int temp = index;
		while(!noNew)
		{
			noNew = true;
			for(int x = 0; x < count2; x++)
			{
				if(posIndex[x] == temp + 1)
				{
					noNew = false;
					temp++;
				}
			}
		}
		return temp;
	}

	//reading what I get
	private boolean recieveStopAndWait()
	{
		fileBits = new byte[4];
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
				port = packet.getPort();
				System.out.println("Received'" + new String(buffer) + "' from " + packet.getAddress().getHostAddress() + " with sender port " + packet.getPort());
				
				//figuring out if I'm done reading.
				byte[] indexArr = new byte[4];
				byte[] buffSizeArr = new byte[4];
				byte[] totalLength = new byte[4];
				for(int x = 0; x < 4; x++)
				{
					indexArr[x] = buffer[x + 12];
					buffSizeArr[x] = buffer[x + 16];
					totalLength[x] = buffer[x + 20];
				}

				ByteBuffer wrap = ByteBuffer.wrap(indexArr);
				int index = wrap.getInt();
				ByteBuffer wrap2 = ByteBuffer.wrap(buffSizeArr);
				int buffsize = wrap2.getInt();
				ByteBuffer wrap3 = ByteBuffer.wrap(totalLength);
				int msgSize = wrap3.getInt();

				updateFile(buffer);

				byte[] ack = sendAck(0, index % 2);
				socket.send(new DatagramPacket(ack, ack.length, InetAddress.getByName(packet.getAddress().getHostAddress()), packet.getPort()));
				System.out.println(index + ", " + buffsize);
				if(index == buffsize - 1){
					makeFile();
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

	//update 2d byte array to fill file.
	private boolean updateFile(byte[] buffer)
	{
		//figuring out if I'm done reading.
		byte[] indexArr = new byte[4];
		byte[] buffSizeArr = new byte[4];
		byte[] totalLength = new byte[4];
		for(int x = 0; x < 4; x++)
		{
			indexArr[x] = buffer[x + 12];
			buffSizeArr[x] = buffer[x + 16];
			totalLength[x] = buffer[x + 20];
		}

		ByteBuffer wrap = ByteBuffer.wrap(indexArr);
		int index = wrap.getInt();
		ByteBuffer wrap2 = ByteBuffer.wrap(buffSizeArr);
		int buffsize = wrap2.getInt();
		ByteBuffer wrap3 = ByteBuffer.wrap(totalLength);
		int msgSize = wrap3.getInt();

		if(fileBits.length != msgSize)
		{
			fileBits = new byte[msgSize];
		}
		System.out.println(msgSize);
		int initIndex = (buffer.length - 24) * index;
		System.out.println("initIndex: " + initIndex);
		for(int x = 24; x < buffer.length && x - 24 + initIndex < msgSize; x++)
		{
			fileBits[initIndex + x - 24] = buffer[x];
		}
		return true;
	}

	private boolean makeFile()
	{
		String output = new String(fileBits);
		try{
			PrintStream console = System.out;
			File file = new File(filename);
			FileOutputStream fos = new FileOutputStream(file);
			PrintStream ps = new PrintStream(fos);
			System.setOut(ps);
			System.out.print(output);

			System.setOut(console);
			System.out.println(output);
		}catch(Exception e){
			return false;
		}
		return true;
	}
}