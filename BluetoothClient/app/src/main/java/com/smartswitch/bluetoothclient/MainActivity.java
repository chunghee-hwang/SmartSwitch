package com.smartswitch.bluetoothclient;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
/*
 * 메인 액티비티 : 클라이언트서비스, 알람체크서비스, 통계액티비티를 관리하는 메인 클래스
 * */
public class MainActivity extends AppCompatActivity {
    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    private static final String TAG = "BluetoothClient";
    static ProgressDialog pd;
    static boolean alive; //메인 액티비티가 실행중인지에 대한 여부
    private BroadcastReceiver msgReceiver; //다른 클래스에서 온 메시지를 받는 브로드캐스트 리시버
    private AlertDialog quitDialog; //종료 다이얼로그(메시지박스)

    //액티비티가 처음 실행될 때 호출 --> 버튼, 클릭이벤트 등 초기화 작업
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        alive = true;

        //리시버 설정 및 등록
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Actions.TO_MAIN_ACTIVITY);
        msgReceiver = new MessageReceiver();
        registerReceiver(msgReceiver, intentFilter);

        final Button turnOnButton = findViewById(R.id.turnOnButton); //전등 켜기 버튼
        final Button turnOffButton = findViewById(R.id.turnOffButton); //전등 끄기 버튼
        pd = new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        //전등 켜기 버튼을 눌렀을 때 호출
        turnOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClientService.connectedToServer) {
                    //서버에게 turn on 메시지를 보내라고 클라이언트 서비스에게 명령
                    sendMsgToOther(Actions.TO_CLIENT_SERVICE, "sendMsgToServer", "turn on");
                    v.setEnabled(false);
                    turnOffButton.setEnabled(true);
                    pd.setMessage("켜는 중");
                    pd.dismiss();
                    pd.show();
                }
            }
        });
        //전등 끄기 버튼을 눌렀을 때 호출
        turnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClientService.connectedToServer) {
                    //서버에게 turn off메시지를 보내라고 클라이언트 서비스에게 명령
                    sendMsgToOther(Actions.TO_CLIENT_SERVICE, "sendMsgToServer", "turn off");
                    v.setEnabled(false);
                    turnOnButton.setEnabled(true);
                    pd.setMessage("끄는 중");
                    pd.dismiss();
                    pd.show();
                }
            }
        });
        Switch alarmSwitch = findViewById(R.id.alarmSwitch); //"기본 알람에 맞춰 전등 켜기" 스위치

        //"기본 알람에 맞춰 전등 켜기" 스위치가 켜지거나 꺼질 때 호출
        alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //저장소에 스위치 상태 저장
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("AlarmSwitch", isChecked);
                editor.apply();
            }
        });
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        alarmSwitch.setChecked(pref.getBoolean("AlarmSwitch", false));

        //블루투스 어댑터 초기화
        Log.d(TAG, "Initalizing Bluetooth adapter...");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) { //블루투스를 지원하지 않는 스마트폰일경우
            showQuitDialog("이 핸드폰은 블루투스를 지원하지 않습니다."); //종료 다이얼로그 띄움
            return;
        }

        //블루투스가 켜져있지 않다면
        if (!bluetoothAdapter.isEnabled()) {
            ClientService.connectedToServer = false;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE); //사용자가 켜도록 요청
        } else {
            startServices(); //백그라운드 작업인 클라이언트서비스와 알람체크서비스 가동
        }
    }

    //메뉴 버튼(점 세 개) 만들기
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    //메뉴들 중 하나를 선택했을 때 호출
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showLogs: //"통계 보기" 메뉴 선택
                startActivity(new Intent(getApplicationContext(), StatisticActivity.class));
                return true;
            case R.id.exit: //"종료" 메뉴 선택
                stopServices();
                finish();
                return true;
            case android.R.id.home: //뒤로가기 버튼 클릭 시
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //앱이 꺼질 때 호출됨
    @Override
    protected void onDestroy() {
        if (pd != null) pd.dismiss();
        alive = false;
        if (msgReceiver != null)
            unregisterReceiver(msgReceiver); //리시버 등록 해제
        super.onDestroy();
    }

    //텍스트뷰의 텍스트를 바꿔주는 함수
    private void setTextView(int id, String str) {
        ((TextView) findViewById(id)).setText(str);
    }

    //버튼 클릭을 자동으로 해주는 함수
    private void clickButton(int id) {
        (findViewById(id)).performClick();
    }

    //버튼을 활성화하거나 비활성화해주는 함수
    private void setEnableButton(int id, boolean enable) {
        Button button = findViewById(id);
        button.setEnabled(enable);
    }

    //다른 액티비티에서 메인 액티비티로 돌아왔을 때 호출됨
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) { //블루투스 켜기 요청에서 돌아왔을 경우
            if (resultCode == RESULT_OK) { //사용자가 블루투스를 켰다면
                startServices(); //백그라운드 작업들 가동
            }
            if (resultCode == RESULT_CANCELED) { //사용자가 블루투스를 켜지 않았다면
                showQuitDialog("블루투스를 켜야됩니다."); //종료 다이얼로그 띄우기
            }
        }
    }

    //종료 다이얼로그(메시지박스) 띄우기
    public void showQuitDialog(String message) {
        if (pd != null) pd.dismiss();
        if (quitDialog != null) {
            quitDialog.dismiss();
            quitDialog.show();
        }
        else {
            quitDialog = new AlertDialog.Builder(this)
                    .setTitle("종료")
                    .setCancelable(false)
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            stopServices();
                            finish();
                        }
                    }).create();
            quitDialog.show();
        }
    }

    //백그라운드 작업인 클라이언트서비스와 알람체크서비스를 시작하는 함수
    private void startServices() {
        Log.d(TAG, "starting ClientService and AlarmCheckService.");
        //알람 시간에 맞춰서 불을 켜도록하기 위해 알람이 울리는지 감시하는 서비스 시작
        startService(new Intent(getApplicationContext(), AlarmCheckService.class));
        if (!ClientService.connectedToServer) //서버와 연결, 통신 작업을 하는 서비스 시작
            startService(new Intent(getApplicationContext(), ClientService.class));
        Thread thread = new WaitConnectedToServer(100); //100초안에 서버와 연결이 안되면 앱을 종료시키는 쓰레드 시작
        thread.start();
    }
    //백그라운드 작업인 클라이언트서비스와 알람체크서비스를 종료시키는 함수
    private void stopServices() {
        Log.d(TAG, "stopping ClientService and AlarmCheckService.");
        ClientService.isConnectionError = false;
        ClientService.connectedToServer = false;
        stopService(new Intent(getApplicationContext(), ClientService.class));
        stopService(new Intent(getApplicationContext(), AlarmCheckService.class));
    }

    //20초안에 서버와 연결이 안되면 앱을 종료시키고 연결이 되면 추가 작업을 하는 쓰레드
    private class WaitConnectedToServer extends Thread {
        private int timeCount = 0; //지난 시간
        private int timeOut; //타임아웃
        WaitConnectedToServer(int timeOut) {
            this.timeOut = timeOut;
        }
        @Override
        public void run() {
            Log.i(TAG, "WaitConnectedToServer thread is running...");

            //서버에 연결될때까지 혹은 타임아웃이 될때까지 기다린다.
            while (!ClientService.connectedToServer && timeCount < timeOut) {
                try {
                    Thread.sleep(1000);
                    timeCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "client is connected to Server? : " + ClientService.connectedToServer);
            if (!ClientService.connectedToServer) { //서버에 연결하지 못한 경우
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            showQuitDialog("스마트 스위치에 연결할 수 없습니다.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                return;
            }

            //서버에 연결된 경우


            Intent intent = getIntent();
            boolean alarmTriggered = intent.getBooleanExtra("alarmTriggered", false);
            Log.i(TAG, "alarm triggered? : " + alarmTriggered);
            if (alarmTriggered) {  //알람이 울려서 앱이 켜졌을 경우 전등 켜짐 버튼을 누른다.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.turnOnButton).performClick();
                    }
                });
            } else { //서버에서 마지막으로 전등이 켰는지 껐는지를 가져오라고 클라이언트서비스에게 명령
                if (ClientService.connectedToServer) {
                    //clientService.sendMsgToServer("show last status");
                    sendMsgToOther(Actions.TO_CLIENT_SERVICE, "sendMsgToServer", "show last status");
                }
            }
        }
    }

    //다른 클래스로부터 메시지를 받는 브로드캐스트리시버
    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(Actions.TO_MAIN_ACTIVITY)) {
                String msgs[] = intent.getStringArrayExtra("msg");
                if (msgs != null) {
                    switch (msgs[0]) {
                        case "setTextView": //텍스트뷰의 글자를 수정하라는 명령을 받을경우
                            int resId = Integer.parseInt(msgs[1]);
                            String text = msgs[2];
                            setTextView(resId, text);
                            break;
                        case "clickButton": //버튼을 클릭하라는 명령을 받을경우
                            clickButton(Integer.parseInt(msgs[1]));
                            break;
                        case "showQuitDialog": //다이얼로그를 띄우라는 명령을 받을경우
                            showQuitDialog(msgs[1]);
                            break;
                        case "setEnableButton": //버튼 활성화 명령을 받을 경우
                            int id = Integer.parseInt(msgs[1]);
                            boolean enable = msgs[2].equals("true");
                            setEnableButton(id, enable);
                            break;

                    }
                }
            }
        }
    }

    //다른 클래스의 브로드캐스트 리시버로 메시지를 보내는 함수
    private void sendMsgToOther(String action, String... msg) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("msg", msg);
        sendBroadcast(intent);
    }
}
