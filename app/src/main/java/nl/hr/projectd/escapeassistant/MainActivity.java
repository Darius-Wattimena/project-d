package nl.hr.projectd.escapeassistant;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.escapeassistant.R;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import nl.hr.projectd.escapeassistant.Utils.FileUtil;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private Boolean placeArrows = false;

    private ArFragment arFragment;
    private ModelRenderable arrowRenderable;
    private Button pythonButton;
    private Python py;

    private Button btn_startNav;

    private ArrayList<Node> nav_arrow_nodes; // Om de geplaatste nodes in memory bij te houden

    public static final String MAP_FILENAME = "bigmap.bin";

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
            py = Python.getInstance();
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
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        btn_startNav = findViewById(R.id.btn_start_navigation);
        // Start navigation click
        btn_startNav.setOnClickListener(v -> {
            placeArrows = true;
            btn_startNav.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Starting navigation...", Toast.LENGTH_SHORT)
                    .show();

        });
        nav_arrow_nodes = new ArrayList<>();

        pythonButton = findViewById(R.id.btn_python_test);
        pythonButton.setOnClickListener(view -> {
            //TODO roep de python activity aan
            File saveDirectory = FileUtil.getStorageDir("test", this);
            File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            py.getModule("opencv_binary").callAttr("main", saveDirectory.getPath(), pictureDirectory.getPath());
            String result = FileUtil.readFile("plattegrond.csv", saveDirectory);
            Log.d(TAG, "---------------------------------------");
            Log.d(TAG, "Python test");
            Log.d(TAG, result);
            Log.d(TAG, "---------------------------------------");
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

        int i = 0;

        // Determine which arrows to display
        for (Node arrow : nav_arrow_nodes) {
            float dX = camPose.tx() - arrow.getWorldPosition().x;
            float dY = camPose.ty() - arrow.getWorldPosition().y;
            float dZ = camPose.tz() - arrow.getWorldPosition().z;

            double distance = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

            arrow.setEnabled(distance < 5 || i == 0);
            ++i;
        }

        switch(cam.getTrackingState()) {
            case TRACKING:
                {
                    if (placeArrows) {

                        ArrayList<Tile> mapTiles = null;

                        // De origin is de virtuele wereld positie van de camera op het moment van inladen
                        AnchorNode origin = new AnchorNode();
                        origin.setParent(arFragment.getArSceneView().getScene());
                        origin.setWorldPosition(new Vector3(camPose.tx(), camPose.ty(), camPose.tz()));
                        origin.setWorldRotation(new Quaternion(0, camPose.qy(), 0, camPose.qw()));

                        try {
                            // Load the map
                            mapTiles = Map.generate(this, MAP_FILENAME);
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
                                    // TODO: End tile, do something
                                }
                                else {
                                    // Plaats een pijl voor deze tile
                                    Node n = placeArrowNode(origin, t.x, t.y, Direction.fromMapSymbol(t.symbol));
                                    nav_arrow_nodes.add(n);
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
}
