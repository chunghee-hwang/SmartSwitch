
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class BluetoothServer 
{
	private boolean isBluetoothEnable = false;
	
	public static void main(String[] args) 
	{
		new BluetoothServer();
	}
	BluetoothServer()
	{
		log("==현재 기기 정보==\n");
		LocalDevice local = null;
		BluetoothStatus bs = null;
		while(!isBluetoothEnable)
		{
			bs = checkBluetoothStatus();
			isBluetoothEnable = bs.isBluetoothEnable;
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
		}
		local = bs.local;
		log("블루투스 주소: " + local.getBluetoothAddress()); //라즈베리파이의 블루투스 주소
		log("이름: " + local.getFriendlyName()); //라즈베리파이 이름
		Runnable r = new ServerRunable();
		Thread thread = new Thread(r);
		thread.start();
	}

	static void log(String msg) 
	{

		System.out.println("[" + (new Date()) + "] " + msg);
	}


private class BluetoothStatus
{
	LocalDevice local = null;
	boolean isBluetoothEnable = false;
	BluetoothStatus(LocalDevice local, boolean isBluetoothEnable)
	{
		this.local = local; this.isBluetoothEnable = isBluetoothEnable;
	}
}

private BluetoothStatus checkBluetoothStatus()
{
	LocalDevice local=null;
	try
	{
		local = LocalDevice.getLocalDevice();
		isBluetoothEnable = true;
		log("Bluetooth is now enabled.");
	}
	catch(BluetoothStateException e)
	{
		local = null;
		isBluetoothEnable = false;
		log("Bluetooth is disabled.");
	}
	finally{
		return new BluetoothStatus(local, isBluetoothEnable);
	}
}

//블루투스 스트림을 가동하는 러너블
class ServerRunable implements Runnable {

	// SPP서버를 위한 uuid
	final UUID uuid = new UUID("5F9B34FB", false); //고유 식별 id 생성
	final String CONNECTION_URL_FOR_SPP = "btspp://localhost:" + uuid
			+ ";name=SPP Server"; //서버 주소

	private StreamConnectionNotifier mStreamConnectionNotifier = null;
	private StreamConnection mStreamConnection = null; 
	private int count = 0; //연결된 클라이언트 수
	private OnOffStatistic onoff = new OnOffStatistic();
	public void run() {

		try {
			//SPP 서버 열기
			mStreamConnectionNotifier = (StreamConnectionNotifier) Connector
					.open(CONNECTION_URL_FOR_SPP);

			log("서버 열기 성공");
		} catch (IOException e) {
			//서버 열기 실패
			log("서버를 열 수 없음: " + e.getMessage());
			return;
		}

		log("서버 가동중.");

		while (true) 
		{
			log("클라이언트를 기다리는 중...");
			
			try {
				//클라이언트를 받을 때까지 block 상태
				mStreamConnection = mStreamConnectionNotifier.acceptAndOpen(); //클라이언트 받음
			} catch (IOException e1) {

				log("클라이언트를 받을 수 없습니다: " + e1.getMessage());
			}
				
			count++;
			log("현재 접속 중인 클라이언트 수: " + count);
			new Receiver(mStreamConnection).start();
		
			
		}

	}
	//메시지를 받는 쓰레드
	class Receiver extends Thread {

		private InputStream mInputStream = null;
		private OutputStream mOutputStream = null;
		private String mRemoteDeviceString = null;
		private StreamConnection mStreamConnection = null; //bluecove에서 제공하는 소켓
		//private SwitchControl mSwitchControl = null;
		Receiver(StreamConnection streamConnection) 
		{
			mStreamConnection = streamConnection;
			//mSwitchControl = new SwitchControl(26);
			try {
				//소켓을 통해 입력, 출력 스트림 열기 시도
				mInputStream = mStreamConnection.openInputStream();
				mOutputStream = mStreamConnection.openOutputStream();

				log("스트림 여는 중...");
			} catch (IOException e) {
				//스트림 열기 실패
				log("스트림을 열 수 없습니다: " + e.getMessage());

				Thread.currentThread().interrupt();//쓰레드 종료
				return;
			}

			try {
				//라즈베리와 연결된 장치 가져오기
				RemoteDevice remoteDevice = RemoteDevice
						.getRemoteDevice(mStreamConnection);

				mRemoteDeviceString = remoteDevice.getBluetoothAddress(); //연결된 기기의 블루투스 주소 가져오기
				String deviceName = remoteDevice.getFriendlyName(true); //연결된 기기의 이름 가져오기
				log("연결된 장치:"+deviceName);
				log("블루투스 주소: " + mRemoteDeviceString);

			} catch (IOException e1) {
				//연결 실패
				log("장치를 찾았지만 연결할 수 없습니다: "
						+ e1.getMessage());
				return;
			}

			log("클라이언트 접속됨");
		}

		public synchronized void run() 
		{

			try {
				//소켓에 연결된 입력 스트림 객체를 통해 클라이언트에서 온 메시지를 읽음
				Reader mReader = new BufferedReader(new InputStreamReader(
						mInputStream, Charset.forName(StandardCharsets.UTF_8
								.name())));

				boolean isDisconnected = false;

				//Sender("connected to bluetooth server"); //연결이 성공했다는 메시지 클라이언트 전송
				Sender("lastStatus: "+onoff.selectLastOnoffStatus()); //마지막 전등 전원 상태 클라이언트로 전송
				while (true) 
				{
					log("메시지 받을 준비됨");
					StringBuilder stringBuilder = new StringBuilder();
					int c = 0;
					while ('\n' != (char) (c = mReader.read())) 
					{
						if (c == -1) {

							log("클라이언트 연결 끊어짐");

							count--; //연결된 클라이언트 수 감소
							log("현재 접속 중인 클라이언트 수: " + count);

							isDisconnected = true;
							Thread.currentThread().interrupt();

							break;
						}

						stringBuilder.append((char) c);
					}

					if (isDisconnected)
						break;

					//received msg from the app
					String recvMessage = stringBuilder.toString().trim();
					log(mRemoteDeviceString + ": " + recvMessage);
					
					//클라이언트가 turn off 요청 보내면
					if(recvMessage.equals("turn off"))
					{
						//스위치 끄기
						//mSwitchControl.turnOffSwitch();
						onoff.record(0);
						Sender("turn off complete");
					}
					//클라이언트가 turn on 요청 보내면
					else if(recvMessage.equals("turn on"))
					{
						//스위치 켜기
						//mSwitchControl.turnOnSwitch();
						onoff.record(1);
						Sender("turn on complete");
					}
					//클라이언트가 마지막으로 전등을 켰는지 껐는지 물어보면
					else if(recvMessage.equals("show last status")) {
						Sender("lastStatus: "+onoff.selectLastOnoffStatus()); //마지막 전등 전원 상태 클라이언트로 전송

					}
					
					//클라이언트가 켜지고 꺼진 모든 기록을 요청하면
					else if(recvMessage.equals("showStatAll")) 
					{
						//db를 조회하여 모든 기록을 클라이언트에게 보낸다.
						String stats = onoff.selectAll();
 						if(stats.equals("")) 
 							Sender("no stat");
 						else Sender(stats);
					}
					//클라이언트가 특정 기간에 대한 켜지고 꺼진 기록을 요청하면
					else if(recvMessage.startsWith("showStat")){
						//recvMessage 예 : showStat 2018-10-11 2018-10-12
						String commands[] = recvMessage.split(" ");
						String date1 = commands[1]; //2018-10-11
 						String date2 = commands[2]; //2018-10-12
 						//해당 기간으로 기록을 db에서 조회해서 클라이언틀로 전송
 						String stats = onoff.selectBetween(date1, date2);
 						if(stats.equals("")) 
 							Sender("no stat");
 						else Sender(stats);
						
					}
					//클라이언트가 모든 기록을 삭제하라고 요청하면
					else if(recvMessage.equals("deleteAllStat")) {
						onoff.clearRecord(); //db의 모든 튜플을 삭제한다.
						Sender("delete stat complete"); //클라이언트에게 삭제 결과 전송
					}
					
				}

			} catch (IOException e) {
				e.printStackTrace();
				log("Receiver 닫음" + e.getMessage());
				count--;
			}
		}
		
		//클라이언트로 메시지를 전송하는 메서드
		void Sender(String msg) {

			//소켓에 연결된 출력 스트림을 통해 클라이언트에게 메시지를 보냄
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(mOutputStream,
							Charset.forName(StandardCharsets.UTF_8.name()))));

			printWriter.write(msg + "\n"); //클라이언트에게 메시지 전송
			printWriter.flush();

			log("나 : " + msg);
		}
	}
}
}