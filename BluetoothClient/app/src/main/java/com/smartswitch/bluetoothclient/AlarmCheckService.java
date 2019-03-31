package com.smartswitch.bluetoothclient;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/*
 * 알람 시간에 맞춰서 불을 켜도록하기 위해 알람이 울리는지 감시하는 서비스(백그라운드 작업) 클래스
 * */
public class AlarmCheckService extends Service
{
    private static final String TAG = "BluetoothClient";
    private AlarmManager alarmManager; //안드로이드 시스템의 알람 관리자
    private BroadcastReceiver mTimeChangedReceiver; //사용자가 알람 시간을 바꾸는지를 감지하는 리시버

    @Override
    public void onCreate() {//서비스가 처음 실행될 때 호출됨
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        //사용자가 알람 시간을 바꾸는지 감지하는 리시버 설정 및 등록
        mTimeChangedReceiver = new AlarmChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        registerReceiver(mTimeChangedReceiver, intentFilter);
        super.onCreate();
    }

    //서비스가 시작될 때 호출
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY_COMPATIBILITY;
    }

    //서비스가 종료될 때 호출
    @Override
    public void onDestroy() {
        unregisterReceiver(mTimeChangedReceiver); //리시버 등록 해제
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    //사용자가 알람 시간을 바꾸는지를 감지하는 브로드캐스트 리시버
    private class AlarmChangeReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action==null) return;
            Log.i(TAG,"AlarmChangeReceiver-- action: "+action);
            if (action.equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
            {
                if(alarmManager==null) alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if(alarmManager!=null)
                {
                    AlarmManager.AlarmClockInfo info = alarmManager.getNextAlarmClock();
                    if(info!=null) {
                        //시스템 앱의 알람 시간으로 내 앱에도 똑같이 알람을 설정한다.
                        alarmManager.set(AlarmManager.RTC, info.getTriggerTime(), TAG, new AlarmManager.OnAlarmListener()
                        {
                            //시스템 앱에서 알람이 울리는 시간에 내 앱에서도 똑같이 반응하도록 한다.
                            @Override
                            public void onAlarm() {
                                //앱화면의 "기본 알람에 맞춰 불 켜기" 스위치가 켜져있는지 여부를 가져옴
                                SharedPreferences pref = getSharedPreferences("pref",MODE_PRIVATE);
                                boolean alarmChecked = pref.getBoolean("AlarmSwitch",false);
                                if(!alarmChecked)return;
                                Toast.makeText(getApplicationContext(),"기본 알람 시간에 맞춰 불을 켭니다.",Toast.LENGTH_LONG).show();
                                if(MainActivity.alive) //알람울릴때 앱이 켜져있다면
                                {
                                    //켜짐 버튼 자동으로 누르기
                                    sendMsgToOther(Actions.TO_MAIN_ACTIVITY, "clickButton", R.id.turnOnButton+"");
                                }
                                else //알람울릴때 앱이 꺼져있다면
                                {
                                    try
                                    {
                                        //메인 액티비티 실행(앱 실행)
                                        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                                        mainIntent.putExtra("alarmTriggered", true);
                                        startActivity(mainIntent);
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                        Log.i(TAG,"Exception : "+ e.getMessage());
                                    }
                                }
                            }
                        }, new Handler());

                        //앱이 켜져있다면 설정된 알람시간을 텍스트뷰에 보여줌
                        if(MainActivity.alive) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 a hh시 mm분", Locale.KOREA);
                            Date configuredAlarmDate = new Date(info.getTriggerTime());
                            sendMsgToOther(Actions.TO_MAIN_ACTIVITY, "setTextView", R.id.currentAlarmText + "", "설정된 알람 시간: " + sdf.format(configuredAlarmDate));
                        }
                    } //info != null
                    else{  //info == null : 설정된 알람이 없을 경우
                        if(MainActivity.alive) {
                            sendMsgToOther(Actions.TO_MAIN_ACTIVITY, "setTextView", R.id.currentAlarmText + "", "설정된 알람 시간이 없습니다.");
                        }
                    }
                }
            }
        }
        //다른 클래스의 브로드캐스트 리시버로 메시지를 보내는 함수
        private void sendMsgToOther(String action, String ...msg) {
            Intent intent = new Intent();
            intent.setAction(action);
            intent.putExtra("msg", msg);
            sendBroadcast(intent);
        }
    }
}
