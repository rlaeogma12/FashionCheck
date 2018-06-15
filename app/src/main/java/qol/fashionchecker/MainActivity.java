package qol.fashionchecker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;
import android.widget.Toast;

import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomMenuButton;
import com.roger.catloadinglibrary.CatLoadingView;
import com.wooplr.spotlight.prefs.PreferencesManager;
import com.wooplr.spotlight.utils.SpotlightSequence;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

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

    //Progress Handler / Dialog (Loading progress)
    CatLoadingView mView;

    ImageButton btn_upload, btn_checkfashion, id_select;
    ImageButton menu_select, list_select;
    BoomMenuButton bmb, bmb2;
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

        /* Button & ImageView Setting */
        iv_view = (ImageView) this.findViewById(R.id.user_image);
        user_id = this.findViewById(R.id.userid);
        btn_upload = this.findViewById(R.id.btn_UploadPicture);
        btn_checkfashion = this.findViewById(R.id.btn_checkfashion);
        id_select = this.findViewById(R.id.btn_UserInfoIcon);
        menu_select = this.findViewById(R.id.btn_option);
        list_select = this.findViewById(R.id.btn_ScoreList);


        //Get user spec
        Intent intent = getIntent();
        String usersid = intent.getStringExtra("userid");
        String gender = intent.getStringExtra("gender");
        user_id.setText(usersid);

        //Boom button Trick
        bmb = this.findViewById(R.id.bmb);
        bmb2 = this.findViewById(R.id.bmb2);

        //Menu Bar
        menu_select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.pulse);
                menu_select.startAnimation(hyperspaceJumpAnimation);
                bmb.boom();
            }
        });

        list_select.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.pulse);
                list_select.startAnimation(hyperspaceJumpAnimation);
                openHistory();
            }
        });

        btn_checkfashion.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.pulse);
                btn_checkfashion.startAnimation(hyperspaceJumpAnimation);

                if(resultPath != null){
                    mView = new CatLoadingView();
                    mView.setCanceledOnTouchOutside(false);
                    mView.show(getSupportFragmentManager(), "");

                    Handler hd = new Handler();
                    hd.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(
                                    getApplicationContext(), // 현재 화면의 제어권자
                                    ResultActivity.class); // 다음 넘어갈 클래스 지정
                            intent.setAction("android.intent.action.RESULT");
                            intent.putExtra("imgPath", resultPath);
                            startActivity(intent);
                            mView.onDismiss(mView.getDialog());
                        }
                    }, 2000);
                }
                else{
                    Toast.makeText(getApplicationContext(), "사진을 설정하시지 않으셨는데요!" ,Toast.LENGTH_LONG).show();
                }

            }
        });

        btn_upload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.pulse);
                btn_upload.startAnimation(hyperspaceJumpAnimation);
                bmb2.boom();
            }
        });

        initSettingsBoom();
        checkPermission();
        helpHighlight();
    }

    private void openHistory(){
        Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
        intent.putExtra("data", "Test Popup");
        startActivityForResult(intent, 1);
    }

    private void initSettingsBoom(){
        initSettingsBoom1();
        initSettingsBoom2();
    }

    private void initSettingsBoom1() {
        int[] settingsBtns = {R.drawable.information, R.drawable.conversation, R.drawable.power_button};
        String[] settingsStrs = { "Info & Credits" , "Help", "Exit"};
        String[] settingSubStrs = { "프로그램 정보", "도움말", "프로그램 종료"};

        for (int i = 0; i < bmb.getPiecePlaceEnum().pieceNumber(); i++) {
            HamButton.Builder builder = new HamButton.Builder()
                    .normalImageRes(settingsBtns[i])
                    .normalText(settingsStrs[i])
                    .subNormalText(settingSubStrs[i])
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            // When the boom-button corresponding this builder is clicked.
                            showMenuDialog(index);
                        }
                    });
            bmb.addBuilder(builder);
        }
    }

    private void showMenuDialog(int id){
        switch(id){
            case 0:
                showInfoDialog();
                break;
            case 1:
                showHelpDialog();
                break;
            case 2:
                showExitDialog();
                break;
        }
    }

    private void showInfoDialog(){
        new LovelyStandardDialog(this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                .setTopColorRes(R.color.color_MAIN)
                .setButtonsColorRes(R.color.colorPrimary)
                .setIcon(R.drawable.information)
                .setTitle("Info & Credits")
                .setMessage("김대흠 이성제 천예지")
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                })
                .show();
    }

    private void showHelpDialog(){
        new LovelyStandardDialog(this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                .setTopColorRes(R.color.color_MAIN)
                .setButtonsColorRes(R.color.colorPrimary)
                .setIcon(R.drawable.conversation)
                .setTitle("Help")
                .setMessage("도움말을 다시 확인하시겠습니까?")
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        helpHighlight();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void showExitDialog(){
        new LovelyStandardDialog(this, LovelyStandardDialog.ButtonLayout.HORIZONTAL)
                .setTopColorRes(R.color.color_MAIN)
                .setButtonsColorRes(R.color.colorPrimary)
                .setIcon(R.drawable.power_button)
                .setTitle("Exit Program")
                .setMessage("정말로 프로그램을 종료하시겠습니까?")
                .setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Exit Program.
                        moveTaskToBack(true);
                        finish();
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void initSettingsBoom2() {
        int[] settingsBtns = {R.drawable.photo_camera, R.drawable.photo_album, R.drawable.close};
        String[] settingsStrs = { "Taking Photo" , "Select Album", "Close"};
        String[] settingSubStrs = { "카메라로 사진 촬영", "앨범에서 사진 선택", "닫기"};

        for (int i = 0; i < bmb2.getPiecePlaceEnum().pieceNumber(); i++) {
            HamButton.Builder builder = new HamButton.Builder()
                    .normalImageRes(settingsBtns[i])
                    .normalText(settingsStrs[i])
                    .subNormalText(settingSubStrs[i])
                    .listener(new OnBMClickListener() {
                        @Override
                        public void onBoomButtonClick(int index) {
                            // When the boom-button corresponding this builder is clicked.
                            showUploadDialog(index);
                        }
                    });
            bmb2.addBuilder(builder);
        }
    }

    private void showUploadDialog(int id){
        switch(id){
            case 0:
                captureCamera();
                break;
            case 1:
                getAlbum();
                break;
            case 2:
                break;
        }
    }

    private void helpHighlight(){
        PreferencesManager mPreferencesManager = new PreferencesManager(MainActivity.this);
        mPreferencesManager.resetAll();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SpotlightSequence.getInstance(MainActivity.this,null)
                        .addSpotlight(id_select, "User Name", "이곳에 당신의 아이디가 나타납니다.", INTRO_SWITCH)
                        .addSpotlight(menu_select, "Menu", "이곳을 클릭하여 메뉴를 확인하세요.", INTRO_RESET)
                        .addSpotlight(iv_view, "Profile Image", "이곳에는 프로필 이미지가 나타납니다.", INTRO_REPEAT)
                        .addSpotlight(list_select, "List", "이곳에는 현재까지의 \n" + "패션 점수 목록이 나타납니다.", INTRO_CHANGE_POSITION)
                        .addSpotlight(btn_upload, "Image Upload", "이곳을 눌러 이미지를 업로드하세요.", INTRO_SEQUENCE)
                        .addSpotlight(btn_checkfashion,"Check", "프로필 사진을 올렸나요?\n" + "분석을 시작합시다!", INTRO_CARD)
                        .startSequence();
            }
        },400);
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
