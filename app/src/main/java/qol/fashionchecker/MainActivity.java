package qol.fashionchecker;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.wooplr.spotlight.prefs.PreferencesManager;
import com.wooplr.spotlight.utils.SpotlightSequence;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

/////
//Main Activity Settings.
/////
public class MainActivity extends AppCompatActivity{
    private static final int MY_PERMISSION_CAMERA = 1;
    private static final int REQUEST_TAKE_PHOTO = 2;
    private static final int REQUEST_TAKE_ALBUM = 3;
    private static final int REQUEST_IMAGE_CROP = 4;

    //Help
    private static final String INTRO_CARD = "fab_intro";
    private static final String INTRO_SWITCH = "switch_intro";
    private static final String INTRO_RESET = "reset_intro";
    private static final String INTRO_REPEAT = "repeat_intro";
    private static final String INTRO_CHANGE_POSITION = "change_position_intro";
    private static final String INTRO_SEQUENCE = "sequence_intro";

    static{
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    ImageView iv_view;
    String mCurrentPhotoPath;
    TextView user_id;
    Uri imageUri;
    Uri photoURI, albumURI;
    String resultPath;

    //Progress Handler / Dialog
    private Handler mHandler;
    private ProgressDialog mProgressDialog;

    ImageButton btn_upload;
    ImageButton btn_checkfashion;
    ImageButton id_select;
    ImageButton menu_select;
    ImageButton list_select;

    //Create the App.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        iv_view = (ImageView) this.findViewById(R.id.user_image);
        user_id = this.findViewById(R.id.userid);



        btn_upload = this.findViewById(R.id.btn_UploadPicture);
        btn_checkfashion = this.findViewById(R.id.btn_checkfashion);
        id_select = this.findViewById(R.id.btn_UserInfoIcon);
        menu_select = this.findViewById(R.id.btn_option);
        list_select = this.findViewById(R.id.btn_ScoreList);

        //ID Setting Alert Pop Up
        id_select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showSetID();
            }
        });

        //Menu Bar
        menu_select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                pulseAnimation();
                //팝업 메뉴 객체 만듬
                PopupMenu popup = new PopupMenu(getApplicationContext(), v);
                //xml파일에 메뉴 정의한것을 가져오기위해서 전개자 선언
                MenuInflater inflater = popup.getMenuInflater();
                Menu menu = popup.getMenu();
                //실제 메뉴 정의한것을 가져오는 부분 menu 객체에 넣어줌
                inflater.inflate(R.menu.popupmenu, menu);
                //메뉴가 클릭했을때 처리하는 부분

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        // TODO Auto-generated method stub
                        //각 메뉴별 아이디를 조사한후 할일을 적어줌
                        switch(item.getItemId()){
                            case R.id.popup_info:
                                showInfoMenu();
                                break;

                            case R.id.popup_help:
                                helpHighlight();
                                break;

                            case R.id.popup_exit:
                                showExitMenu();
                                break;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        list_select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                pulseAnimation();
            }
        });

        btn_checkfashion.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(resultPath != null){
                    Intent intent = new Intent(
                            getApplicationContext(), // 현재 화면의 제어권자
                            ResultActivity.class); // 다음 넘어갈 클래스 지정

                    intent.setAction("android.intent.action.RESULT");
                    intent.putExtra("imgPath", resultPath);

                    mHandler = new Handler();
                    mProgressDialog = ProgressDialog.show(MainActivity.this,"",
                            "사진을 분석중입니다..",true);
                    mHandler.postDelayed( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                if (mProgressDialog!=null&&mProgressDialog.isShowing()){
                                    mProgressDialog.dismiss();
                                }
                            }
                            catch ( Exception e )
                            {
                                e.printStackTrace();
                            }
                        }
                    }, 3000);

                    startActivity(intent); // 다음 화면으로 넘어간다
                }
                else{
                    Toast.makeText(getApplicationContext(), "사진을 설정하시지 않으셨는데요!" ,Toast.LENGTH_LONG).show();
                }

            }
        });


        btn_upload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        captureCamera();
                    }
                };

                DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getAlbum();
                    }
                };


                DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                };

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("업로드할 이미지 선택")
                        .setPositiveButton("사진촬영", cameraListener)
                        .setNeutralButton("앨범선택", albumListener)
                        .setNegativeButton("취소", cancelListener)
                        .show();
            }
        });

        checkPermission();

        helpHighlight();
    }

    //Button Animation
    ObjectAnimator objAnim;
    private void pulseAnimation(){
        objAnim= ObjectAnimator.ofPropertyValuesHolder(list_select,
                PropertyValuesHolder.ofFloat("scaleX", 1.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1.5f));
        objAnim.setDuration(300);
        objAnim.setRepeatCount(ObjectAnimator.RESTART);
        objAnim.setRepeatMode(ObjectAnimator.REVERSE);
        objAnim.start();
    }

    void helpHighlight(){
        PreferencesManager mPreferencesManager = new PreferencesManager(MainActivity.this);
        mPreferencesManager.resetAll();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SpotlightSequence.getInstance(MainActivity.this,null)
                        .addSpotlight(id_select, "User Name", "이곳에 당신의 아이디가 나타납니다.", INTRO_SWITCH)
                        .addSpotlight(menu_select, "Menu", "이곳을 클릭하여 메뉴를 확인하세요.", INTRO_RESET)
                        .addSpotlight(iv_view, "Profile Image", "이곳에는 프로필 이미지가 나타납니다.", INTRO_REPEAT)
                        .addSpotlight(list_select, "List", "이곳에는 현재까지의 패션 점수 목록이 나타납니다.", INTRO_CHANGE_POSITION)
                        .addSpotlight(btn_upload, "Image Upload", "이곳을 눌러 이미지를 업로드하세요.", INTRO_SEQUENCE)
                        .addSpotlight(btn_checkfashion,"Check", "프로필 사진을 올렸나요?\n" + "분석을 시작합시다!", INTRO_CARD)
                        .startSequence();
            }
        },400);
    }

    void showSetID()    {
        final EditText edittext = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("프로필 정보 설정");
        builder.setMessage("당신의 이름을 입력해 주세요.");
        builder.setView(edittext);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        user_id.setText(edittext.getText().toString());
                        Toast.makeText(getApplicationContext(), "유저 이름을 변경했습니다." ,Toast.LENGTH_LONG).show();
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "변경을 취소했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.show();
    }

    void showExitMenu(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("패션 감지기");
        builder.setMessage("정말로 어플리케이션을 종료하겠습니까?");
        builder.setPositiveButton("네",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Exit Program.
                        moveTaskToBack(true);
                        finish();
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Nothing.
                    }
                });
        builder.show();
    }

    void showHelpMenu(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("도움말");
        builder.setMessage("카메라 버튼을 통해 사진을 찍고, 우하단 체크 아이콘을 눌러 자신의 패션 감각을 확인하면 됩니다.");
        builder.setNeutralButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                });
        builder.show();
    }

    void showInfoMenu(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Credit");
        builder.setMessage("김대흠 이성제 천예지");
        builder.setNeutralButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                });
        builder.show();
    }


    private void captureCamera(){
        String state = Environment.getExternalStorageState();
        // 외장 메모리 검사
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e("captureCamera Error", ex.toString());
                }
                if (photoFile != null) {
                    // getUriForFile의 두 번째 인자는 Manifest provider의 authorites와 일치해야 함

                    Uri providerURI = FileProvider.getUriForFile(this, getPackageName(), photoFile);
                    imageUri = providerURI;

                    // 인텐트에 전달할 때는 FileProvier의 Return값인 content://로만!!, providerURI의 값에 카메라 데이터를 넣어 보냄
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI);

                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        } else {
            Toast.makeText(this, "저장공간이 접근 불가능한 기기입니다", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "checkFashion");

        if (!storageDir.exists()) {
            Log.i("mCurrentPhotoPath1", storageDir.toString());
            storageDir.mkdirs();
        }

        imageFile = new File(storageDir, imageFileName);
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }


    private void getAlbum(){
        Log.i("getAlbum", "Call");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_TAKE_ALBUM);
    }

    private void galleryAddPic(){
        Log.i("galleryAddPic", "Call");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        // 해당 경로에 있는 파일을 객체화(새로 파일을 만든다는 것으로 이해하면 안 됨)
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    // 카메라 전용 크랍
    public void cropImage(){
        Log.i("cropImage", "Call");
        Log.i("cropImage", "photoURI : " + photoURI + " / albumURI : " + albumURI);

        Intent cropIntent = new Intent("com.android.camera.action.CROP");

        // 50x50픽셀미만은 편집할 수 없다는 문구 처리 + 갤러리, 포토 둘다 호환하는 방법
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.setDataAndType(photoURI, "image/*");
        cropIntent.putExtra("aspectX", 2); // crop 박스의 x축 비율, 1&1이면 정사각형
        cropIntent.putExtra("aspectY", 3); // crop 박스의 y축 비율
        cropIntent.putExtra("scale", true);
        cropIntent.putExtra("output", albumURI); // 크랍된 이미지를 해당 경로에 저장
        startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Log.i("REQUEST_TAKE_PHOTO", "OK");
                        galleryAddPic();
                        iv_view.setImageURI(imageUri);
                        resultPath = mCurrentPhotoPath;
                    } catch (Exception e) {
                        Log.e("REQUEST_TAKE_PHOTO", e.toString());
                    }
                } else {
                    Toast.makeText(MainActivity.this, "사진찍기를 취소하였습니다.", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_TAKE_ALBUM:
                if (resultCode == Activity.RESULT_OK) {

                    if(data.getData() != null){
                        try {
                            File albumFile;
                            albumFile = createImageFile();
                            photoURI = data.getData();
                            albumURI = Uri.fromFile(albumFile);
                            cropImage();
                        }catch (Exception e){
                            Log.e("TAKE_ALBUM_SINGLE ERROR", e.toString());
                        }
                    }
                }
                break;

            case REQUEST_IMAGE_CROP:
                if (resultCode == Activity.RESULT_OK) {

                    galleryAddPic();
                    iv_view.setImageURI(albumURI);
                    resultPath = albumURI.getPath();
                }
                break;
        }
    }

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 처음 호출시엔 if()안의 부분은 false로 리턴 됨 -> else{..}의 요청으로 넘어감
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                    (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))) {
                new AlertDialog.Builder(this)
                        .setTitle("알림")
                        .setMessage("저장소 권한이 거부되었습니다. 사용을 원하시면 설정에서 해당 권한을 직접 허용하셔야 합니다.")
                        .setNeutralButton("설정", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, MY_PERMISSION_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_CAMERA:
                for (int i = 0; i < grantResults.length; i++) {
                    // grantResults[] : 허용된 권한은 0, 거부한 권한은 -1
                    if (grantResults[i] < 0) {
                        Toast.makeText(MainActivity.this, "해당 권한을 활성화 하셔야 합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // 허용했다면 이 부분에서..

                break;
        }
    }


}
