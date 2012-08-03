package ru.nacu.vkmsg.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import ru.android.common.LogTools;
import ru.android.common.UiTools;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.loader.AsyncTaskLoader;
import ru.nacu.vkmsg.*;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;
import ru.nacu.vkmsg.ui.progress.ProgressDialog;

import java.io.IOException;

import static ru.nacu.vkmsg.ui.profiles.ProfileFragment.*;

/**
 * @author quadro
 * @since 7/7/12 10:58 PM
 */
public final class SettingsFragment extends SherlockFragment implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<SettingsFragment.LoadResult> {

    public static final int SETTINGS_CODE = 5678;
    public static final int ACTIVITY_SELECT_IMAGE = 5680;

    private View btnUpload;
    private View btnSettings;
    private View btnLogout;
    private View btnClose;
    private View btnLogs;
    private TextView title;
    private ImageView imgBig;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.settings, null);

        v.findViewById(R.id.back).setVisibility(View.INVISIBLE);
        title = (TextView) v.findViewById(R.id.title);
        v.findViewById(R.id.img).setVisibility(View.INVISIBLE);
        imgBig = (ImageView) v.findViewById(R.id.big_img);

        btnUpload = v.findViewById(R.id.btn_upload);
        btnSettings = v.findViewById(R.id.btn_settings);
        btnLogout = v.findViewById(R.id.btn_exit);
        btnClose = v.findViewById(R.id.btn_close_app);
        btnLogs = v.findViewById(R.id.btn_logs);

        if (Init.isConnectAlways() && !Init.isUsePush()) {
            btnClose.setVisibility(View.VISIBLE);
        } else {
            btnClose.setVisibility(View.GONE);
        }

        btnUpload.setOnClickListener(this);
        btnSettings.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnLogs.setOnClickListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(Constants.LOADER_SETTINGS, null, this);
    }

    @Override
    public void onClick(View view) {
        if (view == btnUpload) {
            Intent i = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, ACTIVITY_SELECT_IMAGE);

        } else if (btnSettings == view) {
            startActivityForResult(new Intent(getActivity(), PreferencesActivity.class), SETTINGS_CODE);
        } else if (btnLogout == view) {
            getActivity().finish();
            Init.updateToken(0, null);
            Flags.clear();
            VKContentProvider.clearData();
            startActivity(new Intent(getActivity(), MainActivity.class));
        } else if (btnClose == view) {
            Init.stopPoll();
            getActivity().finish();
        } else if (btnLogs == view) {
            try {
                LogTools.sendLogs(getActivity(), R.string.select_mail_app, R.string.no_mail_app);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SETTINGS_CODE: {
                Init.init(false);
                break;
            }

            case ACTIVITY_SELECT_IMAGE: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = data.getData();
                    ProgressDialog.showProgressDialog(getActivity(), new UploadProfileImageTask(selectedImage.toString()));
                }
            }
        }
    }

    @Override
    public Loader<LoadResult> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<LoadResult>(getActivity(), VKContentProvider.CONTENT_URI_FRIEND) {
            @Override
            public LoadResult loadInBackground() {
                final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_PROFILE, PROJECTION,
                        Queries.SELECTION_ID, new String[]{Long.toString(Init.getUserId())}, null);

                try {
                    if (c.moveToNext()) {
                        final long id = c.getLong(_ID);
                        final String fullName = id > 0 ?
                                c.getString(FIRST_NAME) + " " + c.getString(LAST_NAME) : c.getString(PHONE_NAME);
                        final String photo = c.getString(PHOTO);
                        final String photoBig = c.getString(PHOTO_BIG);
                        return new LoadResult(id, fullName, photo, photoBig);
                    } else {
                        return new LoadResult(0, "Профиль не найден", "", "");
                    }
                } finally {
                    c.close();
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onLoadFinished(Loader<LoadResult> loadResultLoader, final LoadResult data) {
        title.setText(data.name);
        new ModernAsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                float thumb = UiTools.dpToPix(200, VKMessenger.getCtx());
                return ThumbnailAsyncListHandler.load(
                        data.photoBig != null ? data.photoBig : data.photo, (int) thumb, false, R.drawable.big_thumb_mask);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    imgBig.setImageBitmap(bitmap);
                }
            }
        }.execute();

    }

    @Override
    public void onLoaderReset(Loader<LoadResult> loadResultLoader) {

    }

    public static class LoadResult {
        public final long id;
        public final String name;
        public final String photo;
        public final String photoBig;

        public LoadResult(long id, String name, String photo, String photoBig) {
            this.id = id;
            this.name = name;
            this.photo = photo;
            this.photoBig = photoBig;
        }
    }
}
