package name.caiyao.yuedongeditor;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    EditText et_step;
    Button btn_edit;
    TextView tv_result;
    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSharedPreferences = getSharedPreferences("yuedong",MODE_PRIVATE);

        et_step = (EditText) findViewById(R.id.et_step);
        btn_edit = (Button) findViewById(R.id.btn_edit);
        tv_result = (TextView) findViewById(R.id.tv_result);

        et_step.setText(mSharedPreferences.getString("step",""));

        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        String step = et_step.getText().toString().trim();
                        mSharedPreferences.edit().putString("step",step).apply();
                        ArrayList<String> arrayList = new ArrayList<>();
                        arrayList.add("chmod 777 /data/data/com.yuedong.sport/databases/*");
                        arrayList.add("am force-stop com.yuedong.sport");
                        runSu(arrayList);
                        arrayList.clear();
                        File file = new File("/data/data/com.yuedong.sport/databases/deamon_foot_record");
                        if (!file.exists()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_result.setText("文件不存在,修改失败！请检查root权限");
                                }
                            });
                            return;
                        }
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                        String date = simpleDateFormat.format(new Date());
                        String tableName = null;
                        try{
                            SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase("/data/data/com.yuedong.sport/databases/deamon_foot_record", null, SQLiteDatabase.OPEN_READWRITE);
                            Cursor tableCursor = sqLiteDatabase.rawQuery("select name from sqlite_master where type='table' order by name",null);
                            while (tableCursor.moveToNext()){
                                String name = tableCursor.getString(0);
                                if (name.contains(date)){
                                    tableName = name;
                                    break;
                                }
                            }
                            tableCursor.close();
                            if (TextUtils.isEmpty(tableName)){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_result.setText("未查询到今日计步数据，请产生数据后修改！");
                                    }
                                });
                                return;
                            }
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("measure", step);
                            Cursor cursor = sqLiteDatabase.query(tableName, new String[]{"start_time"}, null, null, null, null, "start_time desc", "1");
                            String start_time = null;
                            while (cursor.moveToNext()) {
                                start_time = cursor.getString(cursor.getColumnIndex("start_time"));
                            }

                            cursor.close();
                            sqLiteDatabase.update(tableName, contentValues, "start_time=?", new String[]{start_time});
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_result.setText("修改成功！");
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_result.setText("数据库修改失败！");
                                }
                            });
                        }finally {
                            arrayList.add("am start com.yuedong.sport/.main.WelcomeActivity_");
                            runSu(arrayList);
                            arrayList.clear();
                        }
                    }
                }.start();
            }
        });
    }

    public void runSu(ArrayList<String> commands) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            BufferedOutputStream shellInput = new BufferedOutputStream(
                    process.getOutputStream());
            BufferedReader shellOutput = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            for (String command : commands) {
                shellInput.write((command + " 2>&1\n").getBytes());
            }
            shellInput.write("exit\n".getBytes());
            shellInput.flush();

            String line;
            while ((line = shellOutput.readLine()) != null) {
                Log.i("TAG", "command output: " + line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
