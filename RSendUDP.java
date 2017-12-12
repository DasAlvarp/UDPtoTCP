import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; 
//import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class RSendUDP implements edu.utulsa.unet.RSendUDPI
{
	String server = "localhost";
	int port = 32456;

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
		this.port = receiver.getPort();
		return true;
	}

	public InetSocketAddress getReceiver()
	{
		return receiver;
	}

	public boolean sendFile()
	{
		if(mode == 0)
		{
			return sendStopAndWait();
		}
		else
		{
			return sendWindow();
		}
	}

	private boolean sendStopAndWait()
	{
		try {
			byte [][] buffer = makeBuffer();
			UDPSocket socket = new UDPSocket(localPort);
			boolean shouldSend = true;
			int packetIndex = 0;
			int maxNum = buffer.length;
			while(shouldSend)
			{
				socket.send(new DatagramPacket(buffer[packetIndex], buffer[packetIndex].length, InetAddress.getByName(server), port));
				boolean acked = false;

				byte[] ack = new byte[5];
				DatagramPacket packet = new DatagramPacket(ack, ack.length);
				try{
					socket.setSoTimeout((int)timeout);
					socket.receive(packet);
					System.out.println("I got an ack!" + packetIndex +", " + ack[4] + ", " + maxNum);
					if((int)ack[4] == (packetIndex % 2))
					{
						System.out.println("it matched!");
						if(packetIndex == maxNum - 1)
						{
							shouldSend = false;
						}
						acked = true;
						packetIndex++;
					}
				}catch(Exception e){
					System.out.println("Ack not recieved. Sending again.");
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

	private boolean sendWindow()
	{
		try {
			byte [][] buffer = makeBuffer();
			UDPSocket socket = new UDPSocket(localPort);
			boolean shouldSend = true;
			int packetIndex = 0;//packet we're sending. 
			int maxNum = buffer.length;
			while(shouldSend)
			{
				for(int x = packetIndex; x < (packetIndex + mode > maxNum?packetIndex + mode:maxNum); x++)
				{
					socket.send(new DatagramPacket(buffer[x], buffer[x].length, InetAddress.getByName(server), port));
				}

				byte[] ack = new byte[8];
				DatagramPacket packet = new DatagramPacket(ack, ack.length);
				try{
					socket.setSoTimeout((int)timeout);
					socket.receive(packet);
					System.out.println("\nI got an ack!" + packetIndex + ", " + ack[7] + ", " + maxNum + "\n\n");

					byte[] pArray = new byte[4];
					for(int y = 0; y < 4; y++)
					{
						pArray[y] = ack[y + 4];
					}
					ByteBuffer wrap = ByteBuffer.wrap(pArray);
					packetIndex = wrap.getInt();

					if(packetIndex > maxNum)
					{
						System.out.println("Done sending!");
						return true;
					}
				}catch(Exception e){
					System.out.println("Ack not recieved. Sending again.");
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

	private byte[][] makeBuffer()
	{
		try{
			Path path = Paths.get(filename);
			byte[] file = Files.readAllBytes(path);
			int size = file.length;
			int buffSize = (int)Math.ceil((double)size / ((double)modeParameter - 20.0));
			System.out.println(buffSize);
			byte [][] wholeBuffer = new byte[buffSize][(int)modeParameter];//20 because sectionNum+maxSize+mode. Wasting space, but in this case it doesn't really matter, it's literally ALL 0s if it's this mode.
			int fileCount = 0;
			for(int index = 0; index < buffSize; index++){
				//fist, let's get the mode in there
				//first 20 spaces are added
				byte [] byteSize = ByteBuffer.allocate(8).putLong(modeParameter).array();
				for(int x = 4; x < 12; x++)
				{
					wholeBuffer[index][x] = byteSize[x - 4];
				}
				//throw all the ints here
				byte [] byteMode = ByteBuffer.allocate(4).putInt(mode).array();
				byte [] indexNum = ByteBuffer.allocate(4).putInt(index).array();
				byte [] maxSize = ByteBuffer.allocate(4).putInt(buffSize).array();
				byte [] fileSize = ByteBuffer.allocate(4).putInt(size).array();
				for(int x = 0; x < 4; x++)
				{
					wholeBuffer[index][x] = byteMode[x];
					wholeBuffer[index][x + 12] = indexNum[x];
					wholeBuffer[index][x + 16] = maxSize[x];
					wholeBuffer[index][x + 20] = fileSize[x];
				}
				int count = 24;
				//fill up rest of message
				while(count < modeParameter)
				{
					if(fileCount < size){
						//add byte to array
						wholeBuffer[index][count] = file[fileCount];
						fileCount++;
					}else{
						wholeBuffer[index][count] = (byte)0;
					}
					count++;
				}
			}
			return wholeBuffer;
		}catch(Exception e){
			System.out.println("there was an error and my error detection sucks.");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}
}
