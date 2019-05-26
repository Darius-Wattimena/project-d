package nl.hr.projectd.escapeassistant.Utils;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

class ClosableUtil {
    public static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                Log.e("EA", "Unable to close resource");
            }
        }
    }
}
