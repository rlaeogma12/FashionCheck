package qol.fashionchecker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

import com.skyfishjy.library.RippleBackground;
import com.wooplr.spotlight.SpotlightView;
import com.wooplr.spotlight.prefs.PreferencesManager;
import com.wooplr.spotlight.utils.SpotlightSequence;

public class WelcomeActivity extends AppCompatActivity{

    //for spotlight

    private boolean isRevealEnabled = true;
    private SpotlightView spotLight;
    private static final String INTRO_CARD = "fab_intro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.start_main);

        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);

        rippleBackground.startRippleAnimation();

        final ImageButton btn_goLogin = (ImageButton)this.findViewById(R.id.btn_goLogin);
        btn_goLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(
                        getApplicationContext(), // 현재 화면의 제어권자
                        LoginActivity.class); // 다음 넘어갈 클래스 지정
                startActivity(intent); // 다음 화면으로 넘어간다
            }
        });

        PreferencesManager mPreferencesManager = new PreferencesManager(WelcomeActivity.this);
        mPreferencesManager.resetAll();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SpotlightSequence.getInstance(WelcomeActivity.this,null)
                        .addSpotlight(btn_goLogin,"Fashion", "Like the fashion?\n" + "Let others know.", INTRO_CARD)
                        .startSequence();
            }
        },400);


    }

    private void showSpotlight(View view, String usageId) {
        spotLight = new SpotlightView.Builder(this)
                .introAnimationDuration(400)
                .enableRevealAnimation(isRevealEnabled)
                .performClick(true)
                .fadeinTextDuration(400)
                //.setTypeface(FontUtil.get(this, "RemachineScript_Personal_Use"))
                .headingTvColor(Color.parseColor("#eb273f"))
                .headingTvSize(32)
                .headingTvText("Fashion")
                .subHeadingTvColor(Color.parseColor("#ffffff"))
                .subHeadingTvSize(16)
                .subHeadingTvText("Like the picture?\nLet others know.")
                .maskColor(Color.parseColor("#dc000000"))
                .target(view)
                .lineAnimDuration(400)
                .lineAndArcColor(Color.parseColor("#eb273f"))
                .dismissOnTouch(false)
                .dismissOnBackPress(false)
                .enableDismissAfterShown(true)
                .usageId(usageId) //UNIQUE ID
                .show();
    }
}
