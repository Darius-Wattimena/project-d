package nl.hr.projectd.escapeassistant.Services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.chaquo.python.Python;

import java.io.File;

import nl.hr.projectd.escapeassistant.Utils.FileUtil;

public class PythonService extends IntentService {

    private final String TAG = PythonService.class.getSimpleName();
    private Python py;

    public PythonService() {
        super("PythonService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        py = Python.getInstance();
        File saveDirectory = FileUtil.getStorageDir("test", this);
        File sdCard = Environment.getExternalStorageDirectory();
        File pictureDirectory = new File (sdCard.getAbsolutePath() + "/Pictures");
        py.getModule("opencv_binary")
                .callAttr("binary_save_test", //TODO replace the key with the correct method
                        saveDirectory.getPath(),
                        pictureDirectory.getPath());
        String result = FileUtil.readFile("output.bin", saveDirectory);
        Log.d(TAG, "---------------------------------------");
        Log.d(TAG, "Python test");
        Log.d(TAG, result);
        Log.d(TAG, "---------------------------------------");
    }
}
