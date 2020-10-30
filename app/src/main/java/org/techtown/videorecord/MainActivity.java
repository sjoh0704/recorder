package org.techtown.videorecord;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {


//    private StorageReference mStorgeRef;
    private Camera camera;
    private MediaRecorder mediaRecorder;
    private Button btn_record, btn_upload;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean recording = false;
    private String filename = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TedPermission.with(this)
                .setPermissionListener(permission)
                .setRationaleMessage("녹화를 위하여 권한을 허용해주세요.")
                .setDeniedMessage("권한이 거부되었습니다.")
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO )
                .check();


        btn_record = findViewById(R.id.btn_record);
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recording){
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    recording  = false;
                    btn_upload.callOnClick();
                    Toast.makeText(MainActivity.this, "파이어베이스에 자동업로드", Toast.LENGTH_SHORT).show();

                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //과부화도 덜되고 동영상 처리는 여기서 하는게 좋다
                            Toast.makeText(MainActivity.this, "녹화가 시작되었습니다.", Toast.LENGTH_SHORT).show();
                            try {


                                mediaRecorder = new MediaRecorder();
                                camera.unlock();
                                mediaRecorder.setCamera(camera);
                                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                                mediaRecorder. setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
                                mediaRecorder.setOrientationHint(90);

                                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMHH_mmss");
                                Date now = new Date();
                                filename = formatter.format(now) + ".mp4";



                                mediaRecorder.setOutputFile("/sdcard/" + filename);
                                mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
                                mediaRecorder.prepare();
                                mediaRecorder.start();
                                recording = true;


                            }catch (Exception e){
                                e.printStackTrace();
                                mediaRecorder.release();
                            }
                        }
                    });
                }

            }
        });



        btn_upload = findViewById(R.id.btn_upload);

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(filename != null) {
                    Log.e("파일명 확인: ", filename);

                    FirebaseStorage storage = FirebaseStorage.getInstance();

                    Uri file = Uri.fromFile(new File("/sdcard/" + filename));
                    StorageReference storageRef = storage.getReferenceFromUrl("gs://videorecording-e653f.appspot.com/").child("/sdcard/" + filename);



                    Log.e("URi 확인: ", String.valueOf(file));
                    storageRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Toast.makeText(MainActivity.this, "업로드 성공", Toast.LENGTH_SHORT).show();
                                    filename = null;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "업로드 실패", Toast.LENGTH_SHORT).show();
                                }
                            });


                }
                else{
                    Toast.makeText(MainActivity.this, "파일 없음", Toast.LENGTH_SHORT).show();
                }
            }
        });





    }
    PermissionListener permission = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Toast.makeText(MainActivity.this, "권한 허가", Toast.LENGTH_SHORT).show();

            camera = Camera.open();
            camera.setDisplayOrientation(90);
            surfaceView = findViewById(R.id.surfaceView);
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(MainActivity.this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(MainActivity.this, "권한 거부", Toast.LENGTH_SHORT).show();
        }
    };
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        refreshCamera(camera);
    }

    private void refreshCamera(Camera camera) {
    if(surfaceHolder.getSurface() == null){
        return ;
    }
    try {
        camera.stopPreview();
    }
    catch (Exception e){
        e.printStackTrace();
    }

    setCamera(camera);


    }

    private void setCamera(Camera cam) {
        camera = cam;

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

//    public static Uri getUriFromPath(ContentResolver cr, String path) {
//        Log.e("getUriFromPath",  "start");
//        Uri fileUri = Uri.parse(path);
//        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                null, "_data = '" + filePath + "'", null, null);
//
//        cursor.moveToNext();
//        int id = cursor.getInt(cursor.getColumnIndex("_id"));
//        Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
//        Log.e("getUriFromPath",  "end");
//        return uri;
//    }

}