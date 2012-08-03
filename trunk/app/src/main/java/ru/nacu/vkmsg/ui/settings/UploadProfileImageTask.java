package ru.nacu.vkmsg.ui.settings;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.net.ParseException;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONObject;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.progress.ProgressDialogTask;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author quadro
 * @since 7/7/12 11:55 PM
 */
public final class UploadProfileImageTask extends ProgressDialogTask implements Serializable {
    public static final String TAG = "UploadProfileImageTask";

    private final String image;

    private volatile boolean success = false;

    public UploadProfileImageTask(String image) {
        this.image = image;
    }

    @Override
    public void run(Activity ctx) {
        String server;
        try {
            server = VKMessenger.getApi().photosGetProfileUploadServer();
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return;
        }

        JSONObject resp;
        try {
            final String response = multipartRequest(server, Uri.parse(image), "photo");
            System.out.println(response);
            resp = new JSONObject(response);
            final String[] results =
                    VKMessenger.getApi().saveProfilePhoto(resp.getString("server"), resp.getString("photo"), resp.getString("hash"));

            final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>(1);

            o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                    .withSelection(Queries.SELECTION_ID, new String[]{Long.toString(Init.getUserId())})
                    .withValue(Tables.Columns.PHOTO, results[0])
                    .withValue(Tables.Columns.PHOTO_BIG, results[2])
                    .build());

            VKContentProvider.b(o);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return;
        }


        success = true;
    }

    @Override
    public void onPostExecute(Activity ctx) {
        if (!success) {
            Toast.makeText(ctx, R.string.upload_error, Toast.LENGTH_LONG).show();
        }
    }

    public static String getFileName(Uri contentUri) {
        Cursor cursor = VKMessenger.getCtx().getContentResolver().query(contentUri, null, null, null, null);
        try {
            int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            cursor.moveToFirst();
            return cursor.getString(idx);
        } catch (Exception e) {
            return null;
        } finally {
            cursor.close();
        }
    }

    public static String multipartRequest(String urlTo, String fileName, String filefield) throws ParseException, IOException {
        HttpURLConnection connection;
        DataOutputStream outputStream;
        InputStream inputStream;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";

        String result;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;


        try {
            InputStream in = new BufferedInputStream(new FileInputStream(fileName));


            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + fileName + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
            outputStream.writeBytes(lineEnd);

            bytesAvailable = in.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = in.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = in.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = in.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            inputStream = connection.getInputStream();
            result = convertStreamToString(inputStream);

            in.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return result;
        } catch (Exception e) {
            Log.e("MultipartRequest", "Multipart Form Upload Error");
            e.printStackTrace();
            return "error";
        }
    }

    public static String multipartRequest(String urlTo, Uri uri, String filefield) throws ParseException, IOException {
        HttpURLConnection connection;
        DataOutputStream outputStream;
        InputStream inputStream;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";

        String result;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;


        try {
            final String f = getFileName(uri);
            String fileName = "photo.png";
            if (f != null) {
                fileName = f;
            }

            InputStream in = new BufferedInputStream(VKMessenger.getCtx().getContentResolver().openInputStream(uri));


            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + fileName + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
            outputStream.writeBytes(lineEnd);

            bytesAvailable = in.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = in.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = in.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = in.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            inputStream = connection.getInputStream();
            result = convertStreamToString(inputStream);

            in.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return result;
        } catch (Exception e) {
            Log.e("MultipartRequest", "Multipart Form Upload Error");
            e.printStackTrace();
            return "error";
        }
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
