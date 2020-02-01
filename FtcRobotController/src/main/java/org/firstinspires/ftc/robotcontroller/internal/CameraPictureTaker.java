package org.firstinspires.ftc.robotcontroller.internal;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CameraPictureTaker {

    static CameraDevice cameraDevice;
    static Activity activity;
    static TextureView imageView;
    static Handler backgroundHandler;
    static CaptureRequest.Builder captureRequestBuilder;
    static CameraCaptureSession cameraSession;

    public static void setVariables(CameraDevice cameraDevice, Activity activity, TextureView imageView, CaptureRequest.Builder captureRequestBuilder, CameraCaptureSession cameraSession){
        CameraPictureTaker.cameraDevice = cameraDevice;
        CameraPictureTaker.activity = activity;
        CameraPictureTaker.imageView = imageView;
        CameraPictureTaker.backgroundHandler = backgroundHandler;
        CameraPictureTaker.captureRequestBuilder = captureRequestBuilder;
        CameraPictureTaker.cameraSession = cameraSession;

    }

    public static Bitmap takePictureGodDammit() throws CameraAccessException {
        if (cameraDevice == null){
            Log.e("Camera Device", "Null");
            return null;
        }

        CameraManager manager = (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);

        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        Size[] jpegSize = null;

        jpegSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        int width = 100;
        int height =100;

        if (jpegSize!=null && jpegSize.length>0){
            width = jpegSize[0].getWidth();
            height = jpegSize[0].getHeight();
        }

        ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        List<Surface> outputSurface = new ArrayList<>(2);
        outputSurface.add(reader.getSurface());

        outputSurface.add(new Surface(imageView.getSurfaceTexture()));

        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        //int rotation = getWindowManager().getDefaultDisplay().getRotation();
        captureBuilder.set(CaptureRequest.CONTROL_MODE, 50);

        // File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
        //Environment.DIRECTORY_PICTURES), "CameraDemo");
       /* if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Toast.makeText(MainActivity.this, "This directory does not exist", Toast.LENGTH_LONG).show();
                return;
            }
        }

*/


        //filepath = new File(mediaStorageDir.getPath() + File.separator +
        //        "IMG_"+ timeStamp + ".jpg");
        final File filepath = getOutputMediaFile();

        ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;

                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];

                try {
                    save(bytes, filepath);
                    //b.compress(Bitmap.CompressFormat.JPEG, 100, null);
                    //finalImage = null;
                    //Toast.makeText(FtcRobotControllerActivity.this, pos+"", Toast.LENGTH_LONG).show();
                } catch (Exception e){
                    Toast.makeText(activity, e+"6465468465", Toast.LENGTH_LONG).show();
                } finally {
                    if (image != null){
                        image.close();
                    }
                }

            }
        };

        reader.setOnImageAvailableListener(readerListener, backgroundHandler);

        final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                //Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_LONG).show();
                //Toast.makeText(MainActivity.this, filepath+"", Toast.LENGTH_LONG).show();
                try {
                    startCameraPreview();
                } catch (Exception e){
                    Toast.makeText(activity, e+"", Toast.LENGTH_LONG).show();
                }



            }
        };


        cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                try {
                    session.capture(captureBuilder.build(), captureListener, backgroundHandler);
                } catch (Exception e){
                    Toast.makeText(activity, e+"", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        },  backgroundHandler);
        return null;
    }
    //check imageAnasdlfi because i think it's saving in there
    //idk if this save meathod even works
    public static Bitmap save(byte[] bytes, File filepath){
        OutputStream outputStream = null;
        //Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Bitmap b = imageView.getBitmap();
        try (OutputStream out = new FileOutputStream(filepath)){
            b.compress(Bitmap.CompressFormat.JPEG, 100, out);

        } catch (Exception e){
            Toast.makeText(activity, e+"", Toast.LENGTH_LONG).show();
        }

        /*try {
            outputStream = new FileOutputStream(filepath);
            outputStream.write(bytes);
            outputStream.close();
            Toast.makeText(FtcRobotControllerActivity.this, filepath+"", Toast.LENGTH_LONG).show();
        } catch (Exception e){
            Toast.makeText(FtcRobotControllerActivity.this, e+"", Toast.LENGTH_LONG).show();
        }*/
        return b;
    }

    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraCaptureApp");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }

    private static void startCameraPreview() throws  CameraAccessException{
        SurfaceTexture texture = imageView.getSurfaceTexture();
        texture.setDefaultBufferSize(imageView.getWidth(), imageView.getHeight());

        Surface surface = new Surface(texture);

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        captureRequestBuilder.addTarget(surface);

        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if(cameraDevice == null){
                    return;
                }

                cameraSession = session;
                try {
                    updatePreview();
                } catch (Exception e){
                    Toast.makeText(activity, e+"", Toast.LENGTH_LONG).show();
                }


            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        }, null);

    }

    private static void updatePreview() throws CameraAccessException{
        if(cameraDevice == null){
            return;
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO);
        cameraSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
    }
}
