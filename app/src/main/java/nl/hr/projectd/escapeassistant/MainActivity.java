/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.hr.projectd.escapeassistant;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
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

    private Boolean placeArrows = true;

    private ArFragment arFragment;
    private ModelRenderable arrowRenderable;
    private Button pythonButton;
    private Python py;

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
        pythonButton = findViewById(R.id.btn_python_test);
        pythonButton.setOnClickListener(view -> {
            //TODO roep de python activity aan
            File testDirectory = FileUtil.getStorageDir("test", this);
            py.getModule("pythonTest").callAttr("test", testDirectory.getPath());
            //String result = FileUtil.readFile(testDirectory.getPath() + "/test.txt", this);
            Log.d(TAG, "---------------------------------------");
            Log.d(TAG, "Python test");
            //Log.d(TAG, result);
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

        switch(cam.getTrackingState()) {
            case TRACKING:
                {
                    Pose camPose = cam.getDisplayOrientedPose(); // ?

                    if (placeArrows) {

                        ArrayList<Tile> mapTiles = null;

                        try {
                            // Load the map
                            mapTiles = Map.generate(this, "map.csv");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (mapTiles == null) {
                            Log.e(TAG, "onUpdate: Could not load map");
                        } else {
                            for (Tile t : mapTiles) {
                                if (t.symbol == MapSymbols.END) {
                                    // TODO: End tile, do something
                                }
                                else {
                                    // Place Arrow
                                    placeArrowNode(t.x, t.y, Direction.fromMapSymbol(t.symbol));
                                }
                            }
                            placeArrows = false;
                        }
                    }
                }
        }
  }

  private void placeArrowNode(int x, int y, Direction direction) {
      AnchorNode anchorNode = new AnchorNode();
      anchorNode.setParent(arFragment.getArSceneView().getScene());

      // TODO: Determine actual floor level (set to 1.5m for testing purposes)
      final float floorLevel = -1.5f;

      // Set position relative to camera for now (0,0,0) is the center point of the camera
      anchorNode.setWorldPosition(new Vector3(x, floorLevel, y));

      // Create arrow node
      Node arrow = new ArrowNode(direction);

      // Add renderable (arrow model)
      arrow.setRenderable(arrowRenderable);

      // Add arrow to the anchor to keep it in place
      arrow.setParent(anchorNode);
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
