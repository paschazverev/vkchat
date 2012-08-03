package ru.nacu.vkmsg.ui.chat.attachments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import ru.android.common.logs.Logs;
import ru.common.StreamTools;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.ui.progress.ProgressDialogTask;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author quadro
 * @since 7/9/12 2:55 PM
 */
public final class LoadPictureTask extends ProgressDialogTask implements Serializable {
    public static final String TAG = "LoadPictureTask";
    private final String image;

    private volatile boolean success;
    private volatile File f;

    public LoadPictureTask(String image) {
        this.image = image;
    }

    @Override
    public void run(Activity ctx) {
        InputStream in = null;
        OutputStream out = null;
        URL url;
        try {
            url = new URL(image);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String end = extractFile(url);
        f = new File(Constants.downloads, end);

        try {
            in = url.openStream();
            out = new FileOutputStream(f);
            StreamTools.copy(in, out);
        } catch (IOException e) {
            Logs.d(TAG, e.getMessage(), e);
            return;
        } finally {
            StreamTools.close(in);
            StreamTools.close(out);
        }

        success = true;
    }

    public static String extractFile(URL url) {
        String p = url.getPath();
        final int idx = p.lastIndexOf("/");
        if(idx != -1) {
            return p.substring(idx + 1);
        }

        return p;
    }

    @Override
    public void onPostExecute(Activity ctx) {
        if (!success) {
            Toast.makeText(ctx, R.string.error_loading_attachment, Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + f.getAbsolutePath()), "image/*");
            ctx.startActivity(intent);
        }
    }
}
