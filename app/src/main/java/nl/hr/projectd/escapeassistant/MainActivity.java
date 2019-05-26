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
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.escapeassistant.R;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import nl.hr.projectd.escapeassistant.Utils.FileUtil;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  private int temp_nodes_to_place = 0; // ALLEEN OM PIJLEN TE TESTEN

    private Boolean placeArrows = true;
    private Boolean placedArrows = false;
    private int startX = 0, startY = 0;

  private ArrayList<Tile> arrowTiles = new ArrayList<>();

  private ArFragment arFragment;
  private ModelRenderable andyRenderable, arrowRenderable;
  private Button pythonButton;
  private Python py;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
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
    // When you build a Renderable, Sceneform loads its resources in the background while returning
    // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
    ModelRenderable.builder()
        .setSource(this, R.raw.andy)
        .build()
        .thenAccept(renderable -> andyRenderable = renderable)
        .exceptionally(
            throwable -> {
              Toast toast =
                  Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return null;
            });

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

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (andyRenderable == null) {
            return;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable andy and add it to the anchor.
          TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
          andy.setParent(anchorNode);
          andy.setRenderable(andyRenderable);
          andy.select();
        });
    pythonButton = findViewById(R.id.btn_python_test);
    pythonButton.setOnClickListener(view -> {
        //TODO roep de python activity aan
        File testDirectory = FileUtil.getStorageDir("test", this);
        py.getModule("pythonTest").callAttr("test", testDirectory.getPath());
        //String result = FileUtil.readFile(testDirectory.getPath() + "/test.txt", this);
        Log.d("EA", "---------------------------------------");
        Log.d("EA", "Python test");
        //Log.d("EA", result);
        Log.d("EA", "---------------------------------------");
    });

    arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
        arFragment.onUpdate(frameTime);
        onUpdate();
    });
  }

  public void loadMap() throws IOException {
      BufferedReader reader = null;

      reader = new BufferedReader(new InputStreamReader(getAssets().open("map.csv")));

      String line;
      String[] lineSplit;

      int y = 0;

      // For the users' start position
      int endX = 0, endY = 0;

      while ((line = reader.readLine()) != null) {
          lineSplit = line.split(",");

          for(int x=0; x<lineSplit.length; ++x) {
              switch(lineSplit[x]) {
                  case MapSymbols.START: {
                      startX = x;
                      startY = y;
                  } break;

                  case MapSymbols.NORTH:
                  case MapSymbols.EAST:
                  case MapSymbols.SOUTH:
                  case MapSymbols.WEST: {
                    arrowTiles.add(new Tile(x,y,lineSplit[x]));
                  } break;

                  case MapSymbols.END: {
                      endX = x;
                      endY = y;
                  } break;
              }
          }

          y+=1;
      }

      placeArrows = true;

      Log.d(TAG, "READY TO PLACE ARROWS");

      reader.close();
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
                  }

                  for (Tile t : mapTiles) {

                      if (t.symbol.equals(MapSymbols.END)) {
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
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
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