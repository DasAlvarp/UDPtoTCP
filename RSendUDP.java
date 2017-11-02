import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; 
//import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Time;

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
			byte [][] buffer = makeBuffer();
			UDPSocket socket = new UDPSocket(port);
			boolean shouldSend = true;
			int packetIndex = 0;
			int maxNum = buffer.length;
			while(shouldSend)
			{
				socket.send(new DatagramPacket(buffer[packetIndex], buffer[packetIndex].length, InetAddress.getByName(SERVER), port ));
				float time = 0;
				float lastTime = System.currentTimeMillis();
				float curTime = System.currentTimeMillis();
				float deltaTime = 0;
				boolean acked = false;
				while(deltaTime < timeout && !acked)
				{
					curTime = System.currentTimeMillis();

					deltaTime += curTime - lastTime;
					lastTime = curTime;
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

	private int readAck(UDPSocket socket)
	{
		if(socket.ready())
		{
			return 1;
		}
		return -1;
	}

	private byte[][] makeBuffer()
	{
		try{
			Path path = Paths.get(filename);
			byte[] file = Files.readAllBytes(path);
			int size = file.length;
			int buffSize = (int)Math.ceil((double)size / ((double)modeParameter - 20.0));
			byte [][] wholeBuffer = new byte[buffSize][(int)modeParameter];//20 because sectionNum+maxSize+mode. Wasting space, but in this case it doesn't really matter, it's literally ALL 0s if it's this mode.
			int fileCount = 0;
			for(int index = 0; index < buffSize; index++){
				//fist, let's get the mode in there
				//first 20 spaces are added
				byte [] byteMode = ByteBuffer.allocate(4).putInt(mode).array();
				for(int x = 0; x < 4; x++)
				{
					wholeBuffer[index][x] = byteMode[x];
				}
				byte [] byteSize = ByteBuffer.allocate(8).putLong(modeParameter).array();
				for(int x = 4; x < 12; x++)
				{
					wholeBuffer[index][x] = byteSize[x - 4];
				}
				byte [] indexNum = ByteBuffer.allocate(4).putInt(index).array();
				for(int x = 12; x < 16; x++)
				{
					wholeBuffer[index][x] = indexNum[x - 12];
				}
				byte [] maxSize = ByteBuffer.allocate(4).putInt(buffSize).array();
				for(int x = 16; x < 20; x++)
				{
					wholeBuffer[index][x] = indexNum[x - 16];
				}
				int count = 20;
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
				index++;
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
