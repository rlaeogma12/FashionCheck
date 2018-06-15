package qol.fashionchecker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    EditText userid;

    RadioGroup rg;

    String selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login_main);

        //ID Text
        userid = this.findViewById(R.id.txt_id);

        //RadioGroup
        rg = findViewById(R.id.radioGroup1);
        selected = "남자";
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
                switch(id){
                    case R.id.radio0: selected = "남자"; break;
                    case R.id.radio1: selected = "여자"; break;
                }
            }
        });

        //Login.
        final ImageButton btn_goMain = this.findViewById(R.id.btn_startUpload);
        btn_goMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.pulse);
                btn_goMain.startAnimation(hyperspaceJumpAnimation);

                if(userid.getText().equals("")) {
                    Toast.makeText(getApplicationContext(), "아이디를 설정해 주세요." ,Toast.LENGTH_LONG).show();
                }
                else{
                    Intent intent = new Intent(
                            getApplicationContext(), // 현재 화면의 제어권자
                            MainActivity.class); // 다음 넘어갈 클래스 지정
                    intent.putExtra("gender", selected);
                    intent.putExtra("userid", userid.getText().toString());
                    //데이터 전송 부분까지 끝마침.
                    startActivity(intent); // 다음 화면으로 넘어간다
                }
            }
        });
    }
}
