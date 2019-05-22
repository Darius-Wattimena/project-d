package nl.hr.projectd.escapeassistant;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class ArrowNode extends Node {

    public ArrowNode(Direction direction) {
        // Resize
        this.setLocalScale(new Vector3(2.5f,2.5f,2.5f));

        // Rotate to point forward and towards given angle
        this.setLocalRotation(
                Quaternion.multiply(
                        Quaternion.axisAngle(new Vector3(1, 0f, 0), 90),
                        Quaternion.axisAngle(new Vector3(0,0,1), (180 + direction.getDegrees()) % 360))
        );


    }

}
