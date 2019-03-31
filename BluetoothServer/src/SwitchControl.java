public class SwitchControl extends Motor //모터 클래스를 상속받아 모터를 제어하도록 한다.
{
	private int status=-1; //스위치를 켰다면 1, 껐다면 0, 한 번도 켜거나 끄지 않았다면 -1
	SwitchControl(int pinNumber)
	{	
		//모터의 신호(PWM)선의 위치 지정 (Motor.java의 멤버함수)
		setPinNumber(pinNumber);
	}
	
	//전등 스위치를 끄는 함수(10도 까지 회전)
	void turnOffSwitch()
	{
		if(status==0)return;
		BluetoothServer.log("turning off switch..");
		rotateTo(10); //Motor.java의 멤버함수를 사용해서 10도까지 회전
		status = 0;
	}
	//전등 스위치를 켜는 함수(70도 까지 회전)
	void turnOnSwitch()
	{
		if(status==1)return;
		BluetoothServer.log("turning on switch..");
		rotateTo(70); //Motor.java의 멤버함수를 사용해서 70도까지 회전
		status = 1;
	}
}
