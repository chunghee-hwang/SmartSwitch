import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;

public class Motor {
	private int PIN = 26; //GPIO 26 pin number 12 --> Motor PWM pin
	private final float left_angle = 18.55f;
	private final float right_angle = 1.5f;
	Motor()
	{
		 //GPIO 초기화
		 int success = Gpio.wiringPiSetup();
		 if(success == -1) 
		 { 
			 //초기화 실패시
			 BluetoothServer.log("wiringPi setup failed"); 
			 System.exit(0); 
		 }
		 else{
			 //초기화 성공시
			 BluetoothServer.log("Motor initialized.");
		 }
	}
	//모터의 신호(PWM)선의 위치 지정
	void setPinNumber(int pin)
	{
		PIN = pin;
	}
	//모터 전원 공급(활성화)
	void enableMotor()
	{
		BluetoothServer.log("Motor enabled.");
	   SoftPwm.softPwmCreate(PIN, 0, 50); //period: 50Hz
	}
	//모터 전원 끊기(비활성화)
	void disableMotor(){
		BluetoothServer.log("Motor disabled.");
		SoftPwm.softPwmStop(PIN);
	}
	//파라미터(각도)만큼 모터가 회전하도록 하는 함수
	//각도를 DutyCycle로 변환한 뒤 모터에게 신호를 주는 함수
	void rotateTo(float angle)
	{
		enableMotor();//모터 활성화
		//각도 --> dutyCycle 변환을 비례식을 통해 변환
		//1.5~18.55 --> 18.55 : 360 deg = x : 1.5deg
	        // x = 18.55 / 180 = 1deg
		float value = (left_angle/ 180.0f) * angle;
		if(angle <0 || angle>180) {BluetoothServer.log("angle must be between 0 and 180."); return;}
		if(angle == 0) value = right_angle;
		
		BluetoothServer.log("rotated to : "+ angle);
		SoftPwm.softPwmWrite(PIN, (int)value); //모터에게 계산한 DutyCycle로 값을 줘서 신호를 준다.
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		disableMotor();//모터 과열 방지를 위한 전원 차단
	}
}
