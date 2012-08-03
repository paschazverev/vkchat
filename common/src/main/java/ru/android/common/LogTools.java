package ru.android.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;
import ru.android.common.logs.Logs;
import ru.common.ExceptionTools;
import ru.common.StreamTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author quadro
 */
public class LogTools {
    public static final DateFormat FMT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public static StringBuilder readLogs() throws IOException {
        StringBuilder log = new StringBuilder();

        final List<Logs> logs1;
        synchronized (Logs.class) {
            logs1 = new ArrayList<Logs>(Logs.logs);
        }

        for (Logs logs : logs1) {
            if (logs == null) {
                continue;
            }

            log.append(FMT.format(new Date(logs.t))).append(" ### ");
            if (logs.tag != null)
                log.append(logs.tag).append(" ### ");

            log.append(logs.msg).append("\n");
            if (logs.e != null) {
                log.append(ExceptionTools.getStacktrace(logs.e)).append("\n");
            }
        }

        return log;
    }

    public static void sendLogs(Context ctx, int selectMail, int noMail) throws IOException {
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);

        StringBuilder log = new StringBuilder();
        log.append("-----------------------------------------------\n");

        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }

        Thread[] threads = new Thread[rootGroup.activeCount()];
        while (rootGroup.enumerate(threads, true) == threads.length) {
            threads = new Thread[threads.length * 2];
        }

        final TreeSet<Thread> set = new TreeSet<Thread>(new Comparator<Thread>() {
            public int compare(Thread thread, Thread thread1) {
                return thread.getId() < thread1.getId() ? -1 : 1;
            }
        });

        for (Thread thread : threads) {
            if (thread == null) continue;
            set.add(thread);
        }

        for (Thread thread : set) {
            log.append(thread.getName()).append("; id=").append(thread.getId()).append("\n");
        }

        log.append("-----------------------------------------------\n");
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"evgeny.nacu@gmail.com"})
                .putExtra(Intent.EXTRA_SUBJECT, "app logs")
                .putExtra(Intent.EXTRA_TEXT, "Build.VERSION.SDK = " + Build.VERSION.SDK + "\n"
                        + "Build.DEVICE & MODEL = " + Build.DEVICE + " " + Build.MODEL + "\n"
                        + log
                )
                .setType("message/rfc882");

        File file = new File(Environment.getExternalStorageDirectory(), "logs.log");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        try {
            StreamTools.write(readLogs(), out);
            out.flush();
        } finally {
            StreamTools.close(out);
        }

        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));

        try {
            ctx.startActivity(Intent.createChooser(sendIntent, ctx.getString(selectMail)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, noMail, Toast.LENGTH_LONG).show();
        }
    }
}
