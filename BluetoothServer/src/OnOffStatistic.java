import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OnOffStatistic {
	private Statement statement = null;
	//DB 초기화
	public OnOffStatistic()
	{
		try
		{
			Class.forName("org.sqlite.JDBC"); //JDBC 초기화
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace(); //초기화 실패
		}
		Connection connection = null;
		try
		{
			connection = DriverManager.getConnection("jdbc:sqlite:onoff.db"); //데이터베이스 이름은 onoff.db
			statement = connection.createStatement();
			statement.setQueryTimeout(30); //질의 타임아웃 30초로 설정
			//테이블 이름 :onoff
			//테이블 속성 --> 기본키: dt(시간 기록), 속성1 : onOrOff(켜지거나 꺼진 여부) 
			//statement.executeUpdate("DROP TABLE IF EXISTS onoff");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS onoff(dt DATE DEFAULT (DATETIME('now','localtime')), onOrOff INTEGER, PRIMARY KEY(dt))");
			System.out.println("SQLite 초기화 완료.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	//튜플을 추가, 삭제, 수정할 때 사용하는 함수
	private void updateQuery(String query)
	{
		try {
			statement.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	//튜플을 조회(select)할 때 사용하는 함수
	private String selectQuery(String query)
	{
		String resultStr = "";
		try 
		{
			ResultSet resultSet = statement.executeQuery(query+" ORDER BY dt ASC"); //날짜 순으로 오름차순 정렬
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			while(resultSet.next())
			{
				Date date = formatter.parse(resultSet.getString("dt")); //날짜
				int onoff = resultSet.getInt("onOrOff"); //켜거나 끈 여부
				resultStr+=formatter.format(date)+" "+(onoff==1?"on":"off")+"\n"; 
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return resultStr;
	}
	//전등 켜고 끈 기록 테이블에 저장
	void record(int onoff)//켰으면 onoff=1, 껐으면 onoff=2
	{
		updateQuery("INSERT INTO onoff(onOrOff) VALUES("+onoff+")");
	}
	//전등 켜고 끈 기록 모두 삭제
	void clearRecord()
	{
		updateQuery("DELETE from onoff");
	}
	//기록 모두 조회
	String selectAll()
	{
		return selectQuery("SELECT * FROM onoff");
	}
	//기록 중 date1년부터 date2년까지 조회
	String selectBetween(String date1, String date2)//date1<= dt <= date2
	{
		return selectQuery("SELECT * FROM onoff WHERE dt BETWEEN '"+date1+"' AND ('"+date2+"+1')");
	}
	//맨 마지막에 불을 껐는지 켰는지를 쿼리로 확인하는 작업
	//만약 켜진게 마지막이라면 안드로이드 앱에선 켜짐 버튼을 비활성화하여 터치할 수 없게 한다.
	int selectLastOnoffStatus()
	{
		try 
		{
			//날짜순으로 내림차순 정렬한 뒤 맨 위에 것을 가져옴
			ResultSet resultSet = statement.executeQuery("SELECT onOrOff FROM onoff ORDER BY dt DESC LIMIT 1"); 
			while(resultSet.next())
			{
				int onoff = resultSet.getInt("onOrOff"); //켜거나 끈 여부
				return onoff;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
}
