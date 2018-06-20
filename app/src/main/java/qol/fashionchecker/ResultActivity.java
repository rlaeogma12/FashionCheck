package qol.fashionchecker;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.roger.catloadinglibrary.CatLoadingView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class ResultActivity extends AppCompatActivity {
    //Mat & imageView
    private ImageView iv_ProfilePhoto, iv_preferCloth, iv_preferPants, iv_preferBriefcase, iv_preferAccessary;
    private Mat img_input;
    private Mat[] img_output;
    private int matNumber;

    static{
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    private static final String TAG = "opencv";
    public long cascadeClassifier_face = 0;

    //Btn List
    ImageButton btn_return, btn_share;
    ProgressBar prg_color, prg_nanzab;

    //target image file path
    String filePath;

    //Use Thread-Handler for handling huge data process.
    private Handler handler;
    CatLoadingView mView;

    //Score Point
    class ScoreList{
        int score_color;
        int score_nanzab;
        int TPOtype;
    }
    ScoreList scoreObj;

    private TextView txt_score, txt_tpo;

    //User Setting
    String user_gender, user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.final_result);  // connection of layout xml & Java File.

        //Set Mat Default
        matNumber = 6;
        img_output = new Mat[matNumber];

        scoreObj = new ScoreList();
        scoreObj.score_color = -1;  //Default Value(-1 , non-setting)
        scoreObj.score_nanzab = -1;

        // Connect View to value.
        iv_ProfilePhoto = this.findViewById(R.id.Profile_Image);
        iv_preferCloth = this.findViewById(R.id.prefer_shirt);
        iv_preferPants = this.findViewById(R.id.prefer_pants);
        iv_preferBriefcase = this.findViewById(R.id.prefer_briefcase);
        iv_preferAccessary = this.findViewById(R.id.prefer_clean);
        prg_color = this.findViewById(R.id.progress1);
        prg_nanzab = this.findViewById(R.id.progress2);
        txt_score = this.findViewById(R.id.txt_score);
        txt_tpo = this.findViewById(R.id.txt_tpo);

        //Handler allocation.
        handler = new Handler();


        //Intent 받아오기
        Intent intent = getIntent();
        if(intent.getAction().equals("android.intent.action.RESULT")){
            filePath = intent.getStringExtra("imgPath");
            user_gender = intent.getStringExtra("gender");
            user_id = intent.getStringExtra("usersid");
            Log.d("File : ", filePath);
            File imgFile = new File(filePath);
            if(imgFile.exists()){
                Log.d(TAG, "성공적으로 파일 불러오기 성공");

                //Too heavy work.. Set Loading.
                mView = new CatLoadingView();
                mView.setCanceledOnTouchOutside(false);
                mView.show(getSupportFragmentManager(), "");

                //Works on Thread (Func getMatData)
                Thread thread = new Thread(null, getMatData);
                thread.start();
            }
            else{
                Log.d("Cannot found filePath:", filePath);
                Toast.makeText(this, "파일 불러오기에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        btn_return = this.findViewById(R.id.btn_Return);
        btn_return.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(ResultActivity.this, R.anim.pulse);
                btn_return.startAnimation(hyperspaceJumpAnimation);
                Intent intent = new Intent(
                        getApplicationContext(), // 현재 화면의 제어권자
                        WelcomeActivity.class); // 다음 넘어갈 클래스 지정
                startActivity(intent); // 다음 화면으로 넘어간다
            }
        });

        btn_share = this.findViewById(R.id.btn_Share);
        btn_share.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(ResultActivity.this, R.anim.pulse);
                btn_share.startAnimation(hyperspaceJumpAnimation);

                Intent msg = new Intent(Intent.ACTION_SEND);
                msg.addCategory(Intent.CATEGORY_DEFAULT);
                msg.putExtra(Intent.EXTRA_SUBJECT, "주제");
                msg.putExtra(Intent.EXTRA_TEXT, "내용");
                msg.putExtra(Intent.EXTRA_TITLE, "제목");
                msg.setType("text/plain");
                startActivity(Intent.createChooser(msg, "내 점수 공유하기"));
            }
        });

    } // end onCreate()

    //For Thread, get Mat data from C++ opencv native.
    private Runnable getMatData = new Runnable(){
        public void run(){
            try{
                //Heavy Data process Here.
                getMatDataFromNativeCpp();

                //End of Process. send message to handler.
                handler.post(updateResults);

            } catch (Exception e){
                Log.e("Thread getMatData Err: ", e.toString());
            }
        }
    };

    //Handler. notify END to Main Thread
    private Runnable updateResults = new Runnable(){
      public void run(){
          //Set Mat to ImageView
          setMatImageToView();

          //End of Loading.
          mView.onDismiss(mView.getDialog());
      }
    };

    //Access UI
    private void setMatImageToView(){
        if(scoreObj.score_color != -1){
            //Change BRR -> RGB (C++ : BGR , Java : RGB)
            cvtColor(img_input, img_input, COLOR_BGR2RGB);
            for(int i=0; i<matNumber; i++){
                cvtColor(img_output[i], img_output[i], COLOR_BGR2RGB);
            }

            Bitmap bitmapInput = Bitmap.createBitmap(img_input.cols(), img_input.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_input, bitmapInput);
            iv_ProfilePhoto.setImageBitmap(bitmapInput);

            Bitmap[] bitmapOutput = new Bitmap[matNumber];

            for(int i=0; i<matNumber; i++){
                bitmapOutput[i] = Bitmap.createBitmap(img_output[i].cols(), img_output[i].rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_output[i], bitmapOutput[i]);
            }

            iv_preferCloth.setImageBitmap(bitmapOutput[0]);
            iv_preferPants.setImageBitmap(bitmapOutput[1]);
            iv_preferBriefcase.setImageBitmap(bitmapOutput[2]);
            iv_preferAccessary.setImageBitmap(bitmapOutput[3]);
            //2 more settings.
            //Score Setting
            prg_color.setProgress(scoreObj.score_color);
            prg_nanzab.setProgress(scoreObj.score_nanzab);
            int totalScore = (scoreObj.score_color + scoreObj.score_nanzab) / 2;
            String scoreResult = "당신의 패션점수는\n" + totalScore + "/100 점 입니다";
            txt_score.setText(scoreResult);
            txt_tpo.setText(getTPO());  //TPO Set.

        }
        else{
            Toast.makeText(this, "얼굴 인식에 실패했습니다. 올바른 사진을 넣어주세요.", Toast.LENGTH_SHORT).show();
        }
    }
    private String getTPO(){
        switch(scoreObj.TPOtype){
            case 1: return "캠퍼스룩";
            case 2: return "사회생활룩";
            case 3: return "나들이룩";
            default : return "Error..";
        }
    }

    private void getMatDataFromNativeCpp(){
        initMat();
        //set default profile data.
        if(filePath != null){
            loadImage(filePath, img_input.getNativeObjAddr());
            read_cascade_file(); //Cascade Load
            checkFashion(cascadeClassifier_face,
                    img_input.getNativeObjAddr(),
                    img_output[0].getNativeObjAddr(),
                    img_output[1].getNativeObjAddr(),
                    img_output[2].getNativeObjAddr(),
                    img_output[3].getNativeObjAddr(),
                    img_output[4].getNativeObjAddr(),
                    img_output[5].getNativeObjAddr(),
                    scoreObj);
        }
    }

    private void initMat(){
        img_input = new Mat();
        for(int i=0; i<matNumber; i++){
            img_output[i] = new Mat();
        }
    }

    private void copyFile(String objFilename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + objFilename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(objFilename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }
    }

    private void read_cascade_file(){
        copyFile("haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");
        cascadeClassifier_face = loadCascade( "haarcascade_frontalface_alt.xml");
    }

    /*
        Native CPP Implements..
     */

    public native void checkFashion(long cascadeClassifier_face,
                                    long matAddrInput,
                                    long matAddrResult1,
                                    long matAddrResult2,
                                    long matAddrResult3,
                                    long matAddrResult4,
                                    long matAddrResult5,
                                    long matAddrResult6,
                                    ScoreList scoreObj
                                    );

    public native long loadCascade(String cascadeFileName );
    public native void loadImage(String imageFileName, long img);
}
