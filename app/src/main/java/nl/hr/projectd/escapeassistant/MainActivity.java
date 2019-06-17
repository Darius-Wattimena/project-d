package nl.hr.projectd.escapeassistant;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.YuvImage;
import android.graphics.ImageFormat;
import android.graphics.Rect;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.samples.escapeassistant.R;
import com.google.ar.sceneform.ux.ArFragment;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import nl.hr.projectd.escapeassistant.Services.PythonService;
import nl.hr.projectd.escapeassistant.Utils.FileUtil;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private String fileName;

    private Boolean placeArrows = false;

    private ArFragment arFragment;
    private ModelRenderable arrowRenderable, starRenderable;
    private Button pythonButton;

    private Button btn_startNav, btn_stopNav;

    private ArrayList<Node> navigation_nodes; // Om de geplaatste nodes in memory bij te houden

    public static final String MAP_FILENAME = "output.bin";

    public int MY_PERMISSIONS_REQUEST_WRITE_FILE = 1;
    public boolean permissionGranted = false;

    private AnchorNode origin_mem = null;

    @Override
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // Load arrow model
        ModelRenderable.builder()
                .setSource(this, R.raw.arrow)
                .build()
                .thenAccept(renderable -> arrowRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load arrow renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        // Load star model
        ModelRenderable.builder()
                .setSource(this, R.raw.star)
                .build()
                .thenAccept(renderable -> starRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load star renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        btn_startNav = findViewById(R.id.btn_start_navigation);
        btn_stopNav = findViewById(R.id.btn_stop_navigation);
        // Start navigation click
        btn_startNav.setOnClickListener(v -> {
            placeArrows = true;
            btn_startNav.setVisibility(View.INVISIBLE);
            pythonButton.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Starting navigation...", Toast.LENGTH_SHORT)
                    .show();
            btn_stopNav.setVisibility(View.VISIBLE);
        });
        btn_stopNav.setOnClickListener(v -> {
            placeArrows = false;
            pythonButton.setVisibility(View.VISIBLE);
            btn_startNav.setVisibility(View.INVISIBLE);
            btn_stopNav.setVisibility(View.INVISIBLE);
            arFragment.getArSceneView().getScene().removeChild(origin_mem);
        });

        pythonButton = findViewById(R.id.btn_python_test);
        pythonButton.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_FILE);
            } else {
                takePhoto(view);
                btn_startNav.setVisibility(View.VISIBLE);
                pythonButton.setVisibility(View.INVISIBLE);

                //TODO: Foto doorsturen naar python activity
            }

            Intent i = new Intent(MainActivity.this, PythonService.class);
            i.putExtra("fileName", fileName);
            MainActivity.this.startService(i);
        });

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            arFragment.onUpdate(frameTime);
            onUpdate();
        });
    }

    protected void onUpdate() {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Camera cam = frame.getCamera();
        Pose camPose = cam.getPose();

        switch(cam.getTrackingState()) {
            case TRACKING:
                {
                    if (placeArrows) {

                        ArrayList<Tile> mapTiles = null;
                        navigation_nodes = new ArrayList<>();

                        // De origin is de virtuele wereld positie van de camera
                        AnchorNode origin = new AnchorNode();
                        origin_mem = origin;
                        origin.setParent(arFragment.getArSceneView().getScene());
                        origin.setWorldPosition(new Vector3(camPose.tx(), camPose.ty(), camPose.tz()));
                        origin.setWorldRotation(new Quaternion(0, camPose.qy(), 0, camPose.qw()));

                        try {
                            // Load the map
                            File saveDirectory = FileUtil.getStorageDir("test", this);
                            File f = new File(saveDirectory, "output.bin");

                            mapTiles = Map.generate(this, f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (mapTiles == null) {
                            Log.e(TAG, "onUpdate: Could not load map.");
                            Toast.makeText(this, "ERROR! Could not find map. ☹", Toast.LENGTH_LONG)
                                    .show();
                        } else {

                            for (Tile t : mapTiles) {
                                if (t.symbol == MapSymbols.END) {
                                    // END TILE
                                    AnchorNode an = new AnchorNode();
                                    an.setLocalPosition(new Vector3(t.x, -2, t.y));
                                    an.setParent(origin);
                                    an.setRenderable(starRenderable);

                                    CompletableFuture<Material> materialCompletableFuture =
                                            MaterialFactory.makeOpaqueWithColor(this, new Color(255, 0, 0));

                                    materialCompletableFuture.thenAccept(material -> {
                                        Renderable r2 = an.getRenderable().makeCopy();
                                        r2.setMaterial(material);
                                        an.setRenderable(r2);
                                    });
                                }
                                else {
                                    // Als dit een pijl is
                                    if (t.symbol >= ArrowSymbol.NORTH && t.symbol <= ArrowSymbol.SOUTHWEST) {
                                        // Plaats een pijl voor deze tile
                                        Node n = placeArrowNode(origin, t.x, t.y, Direction.fromArrowSymbol(t.symbol));
                                        navigation_nodes.add(n);
                                    }
                                    else if (t.symbol == MapSymbols.START) {
                                        AnchorNode an = new AnchorNode();
                                        an.setLocalPosition(new Vector3(t.x, -2, t.y));
                                        an.setParent(origin);
                                        an.setRenderable(starRenderable);

                                        CompletableFuture<Material> materialCompletableFuture =
                                                MaterialFactory.makeOpaqueWithColor(this, new Color(0, 0, 255));

                                        materialCompletableFuture.thenAccept(material -> {
                                            Renderable r2 = an.getRenderable().makeCopy();
                                            r2.setMaterial(material);
                                            an.setRenderable(r2);
                                        });
                                    }

                                }
                            }
                            Toast.makeText(this, "Successfully loaded the map! ☺", Toast.LENGTH_LONG)
                                    .show();
                        }

                        placeArrows = false;
                    }
                } break;
        }
  }

  private Node placeArrowNode(AnchorNode origin, float xOffset, float yOffset, Direction direction) {

      AnchorNode anchor = new AnchorNode();
      anchor.setParent(origin);

      // TODO: Determine actual floor level
      final float floorLevel = -1;

      // Stel de positie in op de beginpositie (0,0,0) plus de camera veranderingen
      anchor.setLocalPosition(new Vector3(xOffset, floorLevel, yOffset));

      Node arrow = new ArrowNode(direction); // Maak een Arrow Node aan
      arrow.setRenderable(arrowRenderable);  // Stel 3D model in
      arrow.setParent(anchor);               // Stel Node als child van anchor in om in plaats te houden

      return arrow;
  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
      String openGlVersionString = ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
              .getDeviceConfigurationInfo()
              .getGlEsVersion();
      if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
          Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
          Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                  .show();
          activity.finish();
          return false;
      }
      return true;
  }

    private void takePhoto(final View view) {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        fileName = "file" + date + ".jpeg";

        try{
            Image image = arFragment.getArSceneView().getArFrame().acquireCameraImage();
            byte[] byteImage = imageToByte(image);
            image.close();

            File dirpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            try {
                dirpath.mkdirs();

                File outFile = new File(dirpath, fileName);
                OutputStream os = new FileOutputStream(outFile);

                os.write(byteImage);
                os.close();
                Toast.makeText(arFragment.getActivity(), "Picture sucessfully taken", Toast.LENGTH_SHORT)
                        .show();
            } catch (Exception e) {
                Log.e(TAG, "error" + e);
            }
        } catch(Exception e) {
            Log.e(TAG, "error" + e);
        }
    }
    private static byte[] imageToByte(Image image){
        byte[] byteArray = null;
        byteArray = NV21toJPEG(YUV420toNV21(image),image.getWidth(),image.getHeight(),100);
        return byteArray;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        byte[] nv21;
        // Get the three planes.
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_FILE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(arFragment.getActivity(), "permission granted", Toast.LENGTH_SHORT).show();
                permissionGranted = true;
            }
        }
    }
}
