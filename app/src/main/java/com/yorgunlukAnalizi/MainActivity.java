package com.yorgunlukAnalizi;

import android.Manifest;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    Button btnScanAgain;
    boolean isDetected = false;

    FirebaseVisionFaceDetectorOptions firebaseVisionFaceDetectorOptions;
    FirebaseVisionFaceDetector firebaseVisionFaceDetector;

    public float leftEye;
    public float rightEye;

    private TextView leftText;
    private TextView rightText;

    private long warningStartTime = 0;

    public MediaPlayer mediaPlayer;


    private void processResult(List<FirebaseVisionFace> firebaseVisionFaces) {

        if (firebaseVisionFaces.size() > 0) {
            for (FirebaseVisionFace visionFace : firebaseVisionFaces) {
                leftEye = (visionFace.getLeftEyeOpenProbability() * 100);
                rightEye = (visionFace.getRightEyeOpenProbability() * 100);

                if (Math.max(leftEye, rightEye) < (float) 30.0) {
                    if (warningStartTime == 0) {
                        warningStartTime = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - warningStartTime > 2300) {
                        mediaPlayer.start();
                        warningStartTime = 0;
                    }
                } else {
                    warningStartTime = 0;
                }

                leftText.setText(String.valueOf(leftEye));
                rightText.setText(String.valueOf(rightEye));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer = MediaPlayer.create(this, R.raw.song);

        leftText = (TextView) findViewById(R.id.leftEyeText);
        rightText = (TextView) findViewById(R.id.rightEyeText);

        Dexter.withActivity(this).
                withPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        setUpCamera();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    }
                }).check();
    }

    private void setUpCamera() {
        cameraView = findViewById(R.id.camera_view);
        cameraView.setLifecycleOwner((LifecycleOwner) MainActivity.this);
        cameraView.setFacing(Facing.FRONT);
        cameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                processImg(getVisionImgFrame(frame));
            }
        });

        firebaseVisionFaceDetectorOptions = new FirebaseVisionFaceDetectorOptions.Builder().setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.2f)
                .enableTracking()
                .build();
        firebaseVisionFaceDetector = FirebaseVision.getInstance().getVisionFaceDetector(firebaseVisionFaceDetectorOptions);
    }

    private void processImg(FirebaseVisionImage visionImgFrame) {

        firebaseVisionFaceDetector.detectInImage(visionImgFrame)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        processResult(firebaseVisionFaces);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    private FirebaseVisionImage getVisionImgFrame(Frame frame) {
        byte[] data = frame.getData();
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setHeight(frame.getSize().getHeight())
                .setWidth(frame.getSize().getWidth())
                .build();
        return FirebaseVisionImage.fromByteArray(data, metadata);
    }

}

