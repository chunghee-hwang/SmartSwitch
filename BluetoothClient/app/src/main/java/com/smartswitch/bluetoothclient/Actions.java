package com.smartswitch.bluetoothclient;

//서로 다른 클래스에 있는 브로드캐스트 리시버들끼리 통신할 수 있도록 인텐트 액션을 정의해놓는다.
/*
아래 예시처럼 인텐트 필터로 액션을 구분한다.
* IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Actions.TO_MAIN_ACTIVITY);
        msgReceiver = new MessageReceiver();
        registerReceiver(msgReceiver, intentFilter);
* */
public class Actions {
    final static String TO_STAT_ACTIVITY ="TO_STAT_ACTIVITY"; //다른 클래스에서 통계액티비티로 메시지를 보낼때 사용
    final static String TO_CLIENT_SERVICE ="TO_CLIENT_SERVICE"; //다른 클래스에서 클라이언트서비스로 메시지를 보낼때 사용
    final static String TO_MAIN_ACTIVITY="TO_MAIN_ACTIVITY"; //다른 클래스에서 메인액티비티로 메시지를 보낼때 사용
}
