package com.smartswitch.bluetoothclient;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
/*
 * 통계액티비티 : 전등을 켜고 끈 기록을 서버 데이터베이스로부터 받아 사용자에게 보여주는 클래스
 * */
public class StatisticActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, AdapterView.OnItemSelectedListener {
    private List<DateType> dateTypes = new ArrayList<DateType>(); //전체 기간 / 사용자설정 기간
    private List<Stat> stats = new ArrayList<Stat>(); //통계 기록

    //스피너, 리스트뷰 어댑터들
    DateTypeAdapter dateTypeAdapter;
    StatAdapter statAdapter;

    private BroadcastReceiver msgReceiver; //다른 클래스로부터 온 메시지를 처리하는 리시버
    private Calendar mCalendar = Calendar.getInstance(); //시간 정보를 받는 캘린더 객체
    private boolean firstDateSelected;
    private Date firstDate, secondDate;
    private Spinner dateSpinner; //켜고 끈 기록을 전체 기간 또는 사용자설정 기간으로 설정할 수 있게 하는 선택 위젯
    private DatePickerDialog dpdialog; //날짜를 선택할 수 있게 하는 다이얼로그

    //액티비티가 처음 실행될 때 호출
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //기간 선택 위젯(스피너) 초기화
        dateSpinner = findViewById(R.id.dateSpinner);
        dateTypes.add(new DateType("전체 기간", ""));
        dateTypes.add(new DateType("사용자설정", ""));
        dateTypeAdapter = new DateTypeAdapter(this, dateTypes, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setListVisibility(View.GONE, View.VISIBLE);
                configureTwoDates();
                dateSpinner.setSelection(1);
            }
        });
        dateSpinner.setOnItemSelectedListener(this);
        dateSpinner.setAdapter(dateTypeAdapter);

        //통계 리스트뷰 초기화
        final ListView statListView = findViewById(R.id.statListView);
        statAdapter = new StatAdapter(this, R.layout.stat_list_view, stats);
        statListView.setAdapter(statAdapter);

        //브로드캐스트 리시버 등록
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Actions.TO_STAT_ACTIVITY);
        msgReceiver = new MessageReceiver();
        registerReceiver(msgReceiver, intentFilter);
    }

    //시간 선택 다이얼로그로 두 개의 날짜를 받는 함수
    //예 : 2018년 11월 10일 ~ 2018 11월 11일
    private void configureTwoDates() {
        dpdialog = new DatePickerDialog(StatisticActivity.this, StatisticActivity.this,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));
        dpdialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                firstDate = null;
                secondDate = null;
                setListVisibility(View.GONE, View.GONE);
                dateSpinner.setSelection(0);
            }
        });
        dpdialog.show();
        Toast.makeText(getApplicationContext(), "시작 날짜를 선택해주세요.", Toast.LENGTH_SHORT).show();
    }

    //기간 선택 위젯(스피너)의 항목을 선택했을 때 호출
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setListVisibility(View.GONE, View.VISIBLE);
        switch (position) {
            case 0: //전체 기간 선택시
                //서버에 전체 기간 기록 쿼리 요청
                sendMsgToOther(Actions.TO_CLIENT_SERVICE, "sendMsgToServer", "showStatAll");
                break;
            case 1: //사용자설정 기간 선택시
                configureTwoDates(); //두개의 날짜 선택
                break;
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    //날짜 선택 다이얼로그에서 날짜 선택 완료했을 때 호출됨
    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        if (!firstDateSelected) {
            firstDateSelected = true;
            dpdialog = new DatePickerDialog(StatisticActivity.this, StatisticActivity.this,
                    mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH));
            dpdialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    firstDate = null;
                    secondDate = null;
                    setListVisibility(View.GONE, View.GONE);
                    dateSpinner.setSelection(0);
                }
            });
            dpdialog.show();
            firstDate = mCalendar.getTime(); //첫번째 날짜 설정
            Toast.makeText(getApplicationContext(), "끝 날짜를 선택해주세요.", Toast.LENGTH_SHORT).show();
        } else if (firstDate != null) {
            secondDate = mCalendar.getTime(); //두번째 날짜 설정
            String date1, date2;
            if (firstDate.compareTo(secondDate) > 0) { //예 11월 10일 ~ 11월 8일처럼 날짜가 작은게 뒤로가있으면
                //순서를 바꿈
                date2 = sdf.format(firstDate);
                date1 = sdf.format(secondDate);
            } else { //순서가 정상적으로 되어있으면
                date1 = sdf.format(firstDate);
                date2 = sdf.format(secondDate);
            }
            //서버에 선택 기간 기록 쿼리 요청
            sendMsgToOther(Actions.TO_CLIENT_SERVICE, "sendMsgToServer", "showStat " + date1 + " " + date2);
            dateTypes.set(1, new DateType("사용자설정", date1 + "~" + date2));
            dateTypeAdapter.notifyDataSetChanged();
            setListVisibility(View.VISIBLE, View.GONE);
            firstDateSelected = false;
            firstDate = secondDate = null;
        }
    }

    //액티비티가 종료될 때 호출됨
    @Override
    protected void onDestroy() {
        if (msgReceiver != null)
            unregisterReceiver(msgReceiver);
        super.onDestroy();
    }
    private void setListVisibility(int listVisible, int pbVisible) {
        findViewById(R.id.statListView).setVisibility(listVisible);
        findViewById(R.id.statProgressBar).setVisibility(pbVisible);
    }

    //켜고 끈 기록을 리스트뷰에 출력하고 해당 기간동안 사용한 총 시간을 계산하는 함수
    //파라미터에 들어오는 값 예시: 2018-08-30 22:05:33 on\n2018-08-30 23:15:55 off
    private void analyzeStat(String msg) { 

        //리스트뷰에 켜고 끈 기록 출력============================================== 
        String tuples[] = msg.split("\n"); //들어온 기록을 줄바꿈을 기준으로 자름
        DateOnOff[] dateOnoff = new DateOnOff[tuples.length];//날짜 및 시간, 켜고 끈 여부를 임시 저장
        stats.clear();
        stats.add(new Stat("날짜        ", "시간       ", "내용"));
        int idx = 0;
        for (String tuple : tuples) {
            String str[] = tuple.split(" ");
            if (str.length != 3) continue;
            String dateStr = str[0]; //날짜
            String timeStr = str[1]; //시간
            String onoffStr = str[2]; //켜거나 끈 여부
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            try {
                dateOnoff[idx++] = new DateOnOff(sdf.parse(dateStr + " " + timeStr), onoffStr);
                stats.add(new Stat(dateStr, timeStr, onoffStr.equals("on") ? "켜짐" : "꺼짐"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        statAdapter.notifyDataSetChanged(); //통계 리스트뷰 새로고침

        //해당 기간동안 사용한 총 시간을 계산하는 함수============================================== 
        setListVisibility(View.VISIBLE, View.GONE);
        long secondsTot = 0;
        for (int i = 0; i < dateOnoff.length; i++) {
            if (i < dateOnoff.length - 1) {
                //위쪽 기록이 켜짐이고 아래 기록이 꺼짐이면
                //예 : 2018-10-11 01:01:01 on
                //     2018-10-11 01:40:00 off
                if (dateOnoff[i].onoff.equals("on") && dateOnoff[i + 1].onoff.equals("off")) {
                    Date date1 = dateOnoff[i].date;
                    Date date2 = dateOnoff[i + 1].date;
                    long diff = date2.getTime() - date1.getTime(); //사용시간 = 아래 기록 날짜 - 위쪽 기록 날짜
                    long seconds = TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS); //밀리세컨드-->초로 단위 바꿈
                    secondsTot += seconds; //총 사용시간 += 사용시간
                }
            }
        }
        //켜져있던 시간을 텍스트뷰에 출력
        ((TextView) findViewById(R.id.usedTimeText)).setText("켜져있던 시간: " + secondsToTime(secondsTot));
    }

    //날짜 및 시간, 켜고 끈 여부를 저장하는 임시 클래스
    private class DateOnOff {
        Date date;
        String onoff;

        DateOnOff(Date date, String onoff) {
            this.date = date;
            this.onoff = onoff;
        }
    }

    //몇 초를 분, 시 분, 일 시 분 초, 년 일 시 분 초로 바꾸는 함수
    private String secondsToTime(long seconds) {
        String str = "";
        final int minUnit = 60;
        final int hourUnit = minUnit * 60;
        final int dayUnit = hourUnit * 24;
        final int yearUnit = dayUnit * 365;

        if (seconds < minUnit)
            str = seconds + "초";
        else if (seconds < hourUnit) {
            long min = seconds / minUnit;
            seconds %= minUnit;
            str = min + "분 " + seconds + "초";
        } else if (seconds < dayUnit) {
            long hour = seconds / hourUnit;
            seconds %= hourUnit;
            long min = seconds / minUnit;
            seconds %= minUnit;
            str = hour + "시간 " + min + "분 " + seconds + "초";
        } else if (seconds < yearUnit) {
            long day = seconds / dayUnit;
            seconds %= dayUnit;
            long hour = seconds / hourUnit;
            seconds %= hourUnit;
            long min = seconds / minUnit;
            seconds %= minUnit;
            str = day + "일 " + hour + "시간 " + min + "분 " + seconds + "초";
        } else {
            long year = seconds / yearUnit;
            seconds %= year;
            long day = seconds / dayUnit;
            seconds %= dayUnit;
            long hour = seconds / hourUnit;
            seconds %= hourUnit;
            long min = seconds / minUnit;
            seconds %= minUnit;
            str = year + "년 " + day + "일 " + hour + "시간 " + min + "분 " + seconds + "초";
        }
        return str;
    }
    //메뉴(점 세 개) 생성
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_statistic, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //메뉴중 하나를 선택했을 때 호출됨
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //뒤로가기 버튼을 눌렀을 경우
                onBackPressed();
                break;
            case R.id.delStat: //"기록 지우기" 메뉴를 선택했을 경우
                new AlertDialog.Builder(this).setTitle("확인").setMessage("정말 기록을 삭제할까요?").setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //서버에게 모든 튜플을 삭제하는 쿼리 요청
                        sendMsgToOther(Actions.TO_CLIENT_SERVICE, "sendMsgToServer", "deleteAllStat");
                    }
                }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create().show();

                break;
        }
        return super.onOptionsItemSelected(item);
    }
   
    //다른 클래스로부터 온 메시지를 받는 브로드캐스트리시버
    private class MessageReceiver extends BroadcastReceiver
    {
        private String wholeStat="";
        private Timer waitStatComeStop;
        MessageReceiver(){
            waitStatComeStop = new Timer();
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            String msgs[] = intent.getStringArrayExtra("msg");
            if (msgs[0].equals("addStat")) //서버에서 통계 기록 한 줄이 오면
            {
                wholeStat+=(msgs[1]+"\n");//총 통계 기록에 줄 추가
                waitStatComeStop.cancel(); //데이터가 오면 타이머 취소
                waitStatComeStop.purge();
                waitStatComeStop = new Timer();
                //서버에서 통계 데이터가 오는게 그칠때까지 기다렸다가 다 오면 통계를 분석하도록 한다.
                waitStatComeStop.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                analyzeStat(wholeStat);
                                wholeStat="";
                            }
                        });
                    }
                },1000);//데이터가 1초동안 안오면 통계 분석 시작
            }
            else if(msgs[0].equals("clear stat")) //서버에서 통계 기록이 없거나 모두 지웠다고 메시지가 왔다면
            {
                if(stats!=null) {
                   analyzeStat("");
                }
            }
            else if(msgs[0].equals("finish")){
                finish();
            }
        }
    }
    //다른 클래스의 브로드캐스트로 메시지를 보내는 함수
    private void sendMsgToOther(String action, String... msg) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("msg", msg);
        sendBroadcast(intent);
    }
}
