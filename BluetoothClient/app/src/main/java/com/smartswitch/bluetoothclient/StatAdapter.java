package com.smartswitch.bluetoothclient;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;
//통계 내용을 보여주는 리스트뷰의 어댑터
public class StatAdapter extends ArrayAdapter
{
    private List<Stat> list; //통계들을 담은 리스트
    private Context mContext; 
    StatAdapter(@NonNull Context context, int resource, @NonNull List <Stat>list)
    {
        super(context, resource, list);
        this.list = list;
        mContext = context;
    }

    //리스트뷰의 한 줄에 대한 뷰가 만들어질 때 호출됨
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        View v = convertView;
        ViewHolder viewHolder; //리스트뷰 한줄에 들어가는 뷰들을 저장하는 객체(메모리 절약 목적) 
        if(v==null)
        {
            viewHolder = new ViewHolder();
            //레이아웃(xml 파일)을 리스트뷰에 연결
            v = LayoutInflater.from(mContext).inflate(R.layout.stat_list_view,parent,false);
            viewHolder.dateText = v.findViewById(R.id.dateText); //날짜 텍스트뷰 초기화
            viewHolder.timeText = v.findViewById(R.id.timeText); //시간 텍스트뷰 초기화
            viewHolder.onoffText = v.findViewById(R.id.onoffText); //켜짐/꺼짐 텍스트뷰 초기화
            v.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder) v.getTag();
        }
        Stat stat = list.get(position); //리스트에서 통계 객체 뽑기
        if(stat!=null) {
            //통계 객체에서 가져온 날짜, 시간, 켜지거나 꺼진 여부로 각 텍스트뷰의 글자 설정
            viewHolder.dateText.setText(stat.getDate());
            viewHolder.timeText.setText(stat.getTime());
            viewHolder.onoffText.setText(stat.getOnoff());
            if(stat.getOnoff().equals("꺼짐")){ //꺼짐 글자는 
                viewHolder.onoffText.setTextColor(Color.GRAY); //회색으로 
            }
            else{ //켜짐 글자는 
                viewHolder.onoffText.setTextColor(Color.BLUE); //파란색으로 설정
            }
        }
        return v;
    }
    //리스트뷰 한 줄에 들어가는 뷰들을 저장하는 클래스
    private class ViewHolder{
        private TextView dateText; //날짜 텍스트뷰
        private TextView timeText; //시간 텍스트뷰
        private TextView onoffText; // 켜짐 / 꺼짐 텍스트뷰
    }
}

//켜고 끈 통계 클래스 
//들어갈 내용 예 : date : 2018-10-11 ; time : 08:35:33 ; onoff : "켜짐" / "꺼짐"
class Stat{
    private String date; //날짜 
    private String time; //시간
    private String onoff; //켜고 끈 여부
    Stat(String date, String time, String onoff) {
        this.date = date;
        this.time = time;
        this.onoff = onoff;
    }
    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getOnoff() {
        return onoff;
    }
}
