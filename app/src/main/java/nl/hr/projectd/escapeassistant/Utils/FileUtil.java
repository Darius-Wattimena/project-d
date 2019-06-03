package nl.hr.projectd.escapeassistant.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Scanner;

public class FileUtil {

    /**
     * @param fileName
     *      Name of the file you want to read.
     * @param context
     *      Context of the current activity
     * @return
     *      {@link String} of the value inside the file.
     */
    public static String readFile(String fileName, Context context) {
        String result;
        FileInputStream fis = null;
        InputStreamReader sr = null;
        BufferedReader bufferedReader = null;

        try {
            fis = context.openFileInput(fileName);

            sr = new InputStreamReader(fis, Charset.forName("UTF-8"));

            bufferedReader = new BufferedReader(sr);
            StringBuilder sb = new StringBuilder();

            while((result = bufferedReader.readLine()) != null) {
                sb.append(result);
            }
            result = sb.toString();
        } catch (IOException e) {
            result = null;
        } finally {
            try {
                ClosableUtil.close(sr);
                ClosableUtil.close(fis);

                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                Log.e("EA", e.getMessage());
            }
        }

        return result;
    }

    public static String readFile(String fileName, File directory) {
        String result;
        FileInputStream fis = null;
        InputStreamReader sr = null;
        BufferedReader bufferedReader = null;

        try {
            File file = new File(directory, fileName);
            //File file = File.createTempFile(fileName, fileSuffix, directory);
            bufferedReader = new BufferedReader(new FileReader(file));

            StringBuilder sb = new StringBuilder();

            while((result = bufferedReader.readLine()) != null) {
                sb.append(result);
            }
            result = sb.toString();
        } catch (IOException e) {
            result = null;
        } finally {
            try {
                ClosableUtil.close(sr);
                ClosableUtil.close(fis);

                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                Log.e("EA", e.getMessage());
            }
        }

        return result;
    }

    /**
     * @param fileName
     *      Name of the file you want to read.
     * @param vClass
     *      Current class
     * @return
     *      {@link String} of the value inside the file.
     */
    public static String readFileFromResource(String fileName, Class vClass) {
        StringBuilder sb = new StringBuilder("");
        ClassLoader cl = vClass.getClassLoader();
        File file = new File(cl.getResource(fileName).getFile());
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                sb.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * @param fileName
     *      Name of the file you want to read.
     * @return
     *      {@link String} of the value inside the file.
     */
    public static String readExternalFile(String fileName) {
        if (isExternalStorageReadable()) {
            File file = new File(Environment.getExternalStoragePublicDirectory("test"), fileName);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
                while ((line = in.readLine()) != null) stringBuilder.append(line);

            } catch (IOException e) {
                Log.e("EA", e.getMessage());
            }finally{
                ClosableUtil.close(in);
            }
            return stringBuilder.toString();
        } else {
            return null;
        }
    }


    /**
     * @param fileName
     *      Name of the file you want to write.
     * @param content
     *      The value you want to write to the file.
     * @param context
     *      Context of the current activity
     * @return
     *      Boolean value, true if success or false if failed.
     */
    public static boolean writeFile(String fileName, Integer content, Context context) {
        return writeFile(fileName, String.valueOf(content), context);
    }

    /**
     * @param fileName
     *      Name of the file you want to write.
     * @param content
     *      The value you want to write to the file.
     * @param context
     *      Context of the current activity
     * @return
     *      Boolean value, true if success or false if failed.
     */
    public static boolean writeFile(String fileName, String content, Context context) {
        boolean succes;
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes(Charset.forName("UTF-8")));

            succes = true;
        } catch (IOException e) {
            Log.e("EA", e.getMessage());
            succes = false;
        } finally {
            ClosableUtil.close(fos);
        }
        return succes;
    }

    /**
     * @param fileName
     *      Name of the file you want to write.
     * @param content
     *      The value you want to write to the file.
     * @return
     *      Boolean value, true if success or false if failed.
     */
    public static boolean writeExternalFile(String fileName, Integer content) {
        return writeExternalFile(fileName, String.valueOf(content));
    }

    /**
     * @param fileName
     *      Name of the file you want to write.
     * @param content
     *      The value you want to write to the file.
     * @return
     *      Boolean value, true if success or false if failed.
     */
    public static boolean writeExternalFile(String fileName, String content) {
        if (isExternalStorageWritable()) {
            FileOutputStream out = null;
            try {
                File dir = Environment.getExternalStoragePublicDirectory("test");
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e("EA", "Directory not created");
                    }
                }
                File file = new File(dir, fileName);
                out = new FileOutputStream(file);

                out.write(content.getBytes(Charset.forName("UTF-8")));

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }finally{
                ClosableUtil.close(out);
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static File getStorageDir(String fileName, Context context) {
        File dir = new File(context.getExternalFilesDir(null), fileName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("EA", "Error creating director");
            }
        }

        return dir;
    }
}
