package qol.fashionchecker;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class ResultActivity extends AppCompatActivity {
    private ImageView iv_ProfilePhoto;
    private Mat img_input, img_output1, img_output2;

    static{
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    private static final String TAG = "opencv";
    public long cascadeClassifier_face = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.final_result);  // layout xml 과 자바파일을 연결

        iv_ProfilePhoto = this.findViewById(R.id.Profile_Image);

        //Intent 받아오기
        Intent intent = getIntent();
        if(intent.getAction().equals("android.intent.action.RESULT")){
            String filePath = intent.getStringExtra("imgPath");
            Log.d("File : ", filePath);
            File imgFile = new File(filePath);
            if(imgFile.exists()){
                Log.d(TAG, "성공적으로 파일 불러오기 성공");
                checkProcessing(filePath);
            }
            else{
                Log.d("Cannot found filePath:", filePath);
                Toast.makeText(this, "파일 불러오기에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        ImageButton btn_return = this.findViewById(R.id.btn_Return);
        btn_return.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(
                        getApplicationContext(), // 현재 화면의 제어권자
                        MainActivity.class); // 다음 넘어갈 클래스 지정
                startActivity(intent); // 다음 화면으로 넘어간다
            }
        });

        /*
        ImageButton btn_colorHelp = this.findViewById(R.id.btn_colorHelp);
        btn_colorHelp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(getApplicationContext(), ColorHelpActivity.class);
                intent.putExtra("data", "Test Popup");
                startActivityForResult(intent, 1);
            }
        });
        */

    } // end onCreate()

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
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



    private void imageprocess_and_showResult() {

        checkFashion(cascadeClassifier_face, img_input.getNativeObjAddr(), img_output1.getNativeObjAddr(), img_output2.getNativeObjAddr());

        cvtColor(img_input, img_input, COLOR_BGR2RGB);

        Bitmap bitmapInput = Bitmap.createBitmap(img_input.cols(), img_input.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_input, bitmapInput);
        iv_ProfilePhoto.setImageBitmap(bitmapInput);

        /*
        Bitmap bitmapOutput1 = Bitmap.createBitmap(img_output1.cols(), img_output1.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_output1, bitmapOutput1);
        iv_TOTPhoto.setImageBitmap(bitmapOutput1);

        Bitmap bitmapOutput2 = Bitmap.createBitmap(img_output2.cols(), img_output2.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_output2, bitmapOutput2);
        iv_TITPhoto.setImageBitmap(bitmapOutput2);
        */
    }

    public void checkProcessing(String filePath){
        img_input = new Mat();
        img_output1 = new Mat();
        img_output2 = new Mat();

        if(filePath != null){
            loadImage(filePath, img_input.getNativeObjAddr());
            read_cascade_file(); //Cascade Load
            imageprocess_and_showResult();
        }

    }

    /*
        Native CPP Implements..
     */

    public native void checkFashion(long cascadeClassifier_face,
                                    long matAddrInput,
                                    long matAddrResult1,
                                    long matAddrResult2);

    public native long loadCascade(String cascadeFileName );
    public native void loadImage(String imageFileName, long img);
}
