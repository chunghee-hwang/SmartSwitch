package com.smartswitch.bluetoothclient;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

//켜고 끈 통계를 전체 기간으로 조회할 지, 선택 기간으로 조회할지 선택하는 스피너의 어댑터
public class DateTypeAdapter extends BaseAdapter
{
    private Context mContext;
    private List<DateType> mList; //조회종류, 조회기간에 대한 정보를 가지는 리스트
    private View.OnClickListener dateClickListener; //조회기간 텍스트뷰를 클릭했을 때 사용될 리스너
    DateTypeAdapter(@NonNull Context context, @NonNull List<DateType> list, View.OnClickListener dateClickListener)
    {
        mContext = context;
        mList = list;
        this.dateClickListener = dateClickListener; //리스너를 생성자로부터 받는다.
    }
    @Override
    public int getCount() {
        return mList.size(); //스피너 항목의 개수는 리스트의 길이로 설정
    }
    @Override
    public DateType getItem(int position) {
        return mList.get(position); //조회 기간 정보는 리스트로부터 가져온다.
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    //스피너의 항목들에 대한 뷰가 만들어질 때 호출
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull final ViewGroup parent)
    {
        View v = convertView; //스피너 한 줄에 대한 뷰
        ViewHolder viewHolder; //스피너 한 줄에 들어가는 뷰들을 저장하는 객체
        if(v==null)
        {
            viewHolder = new ViewHolder();
            //레이아웃(xml 파일)을 리스트뷰에 연결
            v = LayoutInflater.from(mContext).inflate(R.layout.date_type_view,parent,false);
            viewHolder.dateTypeText = v.findViewById(R.id.dateTypeText); //조회 종류 텍스트 초기화
            viewHolder.dateText = v.findViewById(R.id.dateText); //조회 기간 텍스트 초기화
            viewHolder.dateText.setOnClickListener(new View.OnClickListener() {
                //조회기간 텍스트(예 : 2018-07-28~2018-08-05)를 클릭했을 시 호출
                @Override
                public void onClick(View v) {
                    dateClickListener.onClick(v); //생성자로 넘어온 리스너가 실행되도록 한다.
                }
            });
            v.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder) v.getTag();
        }

        DateType dateType = getItem(position); //조회기간 객체 가져오기
        if(dateType!=null) {
            //조회기간 객체로부터 조회 종류, 기간을 받아 각 텍스트뷰에 설정 
            viewHolder.dateTypeText.setText(dateType.getDateType());
            viewHolder.dateText.setText(dateType.getDate());
            viewHolder.dateText.setFocusable(false);
            viewHolder.dateText.setClickable(true);
        }
        return v;
    }
    //스피너 한 줄에 들어가는 뷰들을 저장하는 클래스(메모리 절약 목적)
    class ViewHolder{
        TextView dateTypeText; //조회 종류 텍스트뷰(예 : 전체 기간)
        TextView dateText; //조회 기간 텍스트뷰(예 : 2018-07-28~2018-08-05)
    }
}

//조회 종류, 조회 기간을 가지고 있는 클래스
class DateType
{
    private String dateType;
    private String date;
    DateType(String dateType, String date){
        this.dateType = dateType; this.date = date;
    }
    String getDateType() {
        return dateType;
    }
    String getDate() {
        return date;
    }
}

