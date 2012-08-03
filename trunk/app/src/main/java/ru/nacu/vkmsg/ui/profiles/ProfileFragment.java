package ru.nacu.vkmsg.ui.profiles;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockFragment;
import ru.android.common.UiTools;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.loader.AsyncTaskLoader;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.asynctasks.SyncContactsTask;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;
import ru.nacu.vkmsg.ui.contacts.ContactsFragment;
import ru.nacu.vkmsg.ui.contacts.DialogReadyListener;
import ru.nacu.vkmsg.ui.progress.ProgressDialog;

/**
 * @author quadro
 * @since 7/5/12 5:29 PM
 */
public final class ProfileFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<ProfileFragment.LoadResult>, View.OnClickListener, DialogReadyListener {

    public static final int FFRND = 1;
    public static final int FSENT = 2;
    public static final int FRECV = 3;
    public static final int FSUGG = 4;

    public static final String[] PROJECTION = {
            Tables.Columns._ID,
            Tables.Columns.FIRST_NAME,
            Tables.Columns.LAST_NAME,
            Tables.Columns.FRIEND,
            Tables.Columns.PHONE,
            Tables.Columns.PHOTO,
            Tables.Columns.PHOTO_BIG,
            Tables.Columns.PHONE_NAME
    };

    public static final int _ID = 0;
    public static final int FIRST_NAME = 1;
    public static final int LAST_NAME = 2;
    public static final int FRIEND = 3;
    public static final int PHONE = 4;
    public static final int PHOTO = 5;
    public static final int PHOTO_BIG = 6;
    public static final int PHONE_NAME = 7;

    private long profileId;
    private LoadResult data;

    private ImageView img;
    private View btnSendMessage;
    private View btnSendInvitation;
    private View invitation;
    private Button btnCall;
    private View btnAddToFriends;
    private View btnCancelInvitation;
    private View btnDeleteFriend;

    public void setProfileId(long profileId) {
        this.profileId = profileId;
        getLoaderManager().initLoader(Constants.LOADER_PROFILE, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.profile, null);

        img = (ImageView) v.findViewById(R.id.img);
        btnSendMessage = v.findViewById(R.id.btn_send);
        btnSendInvitation = v.findViewById(R.id.btn_send_invitation);
        invitation = v.findViewById(R.id.invitation);
        btnCall = (Button) v.findViewById(R.id.btn_call);
        btnAddToFriends = v.findViewById(R.id.btn_add_to_friends);
        btnDeleteFriend = v.findViewById(R.id.btn_delete_friend);
        btnCancelInvitation = v.findViewById(R.id.btn_cancel_invitation);

        btnSendMessage.setOnClickListener(this);
        btnSendInvitation.setOnClickListener(this);
        btnCall.setOnClickListener(this);
        btnAddToFriends.setOnClickListener(this);
        btnCancelInvitation.setOnClickListener(this);
        btnDeleteFriend.setOnClickListener(this);

        getActivity().registerForContextMenu(btnCall);

        return v;
    }

    @Override
    public Loader<LoadResult> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<LoadResult>(getActivity(), VKContentProvider.CONTENT_URI_FRIEND) {
            @Override
            public LoadResult loadInBackground() {
                final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_PROFILE, PROJECTION,
                        Queries.SELECTION_ID, new String[]{Long.toString(profileId)}, null);

                try {
                    if (c.moveToNext()) {
                        final long id = c.getLong(_ID);
                        final String fullName = id > 0 ?
                                c.getString(FIRST_NAME) + " " + c.getString(LAST_NAME) : c.getString(PHONE_NAME);
                        final int friend = c.getInt(FRIEND);
                        final String phone = c.getString(PHONE);
                        final String photo = c.getString(PHOTO);
                        final String photoBig = c.getString(PHOTO_BIG);
                        return new LoadResult(id, fullName, friend, phone, photo, photoBig);
                    } else {
                        throw new RuntimeException("Can't find profile");
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
        this.data = data;
        getHost().onProfileLoad(data);
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
                    img.setImageBitmap(bitmap);
                }
            }
        }.execute();

        btnSendMessage.setVisibility(data.id > 0 ? View.VISIBLE : View.GONE);
        btnSendInvitation.setVisibility(profileId < 0 ? View.VISIBLE : View.GONE);
        invitation.setVisibility(profileId < 0 ? View.VISIBLE : View.GONE);
        btnCall.setVisibility(data.phone != null ? View.VISIBLE : View.GONE);
        if (data.phone != null) {
            String[] phones = data.phone.split(",");
            if (phones.length == 1) {
                btnCall.setText(getString(R.string.call_to) + " " + SyncContactsTask.toMSISDN(phones[0].trim()));
            } else {
                btnCall.setText(R.string.call);
            }
        }

        btnAddToFriends.setVisibility(data.id > 0 && (data.friend == 4 || data.friend == 0 || data.friend == FRECV)
                ? View.VISIBLE : View.GONE);
        btnDeleteFriend.setVisibility(data.friend == 1 ? View.VISIBLE : View.GONE);
        btnCancelInvitation.setVisibility(data.friend == 2 || data.friend == 3 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<LoadResult> loadResultLoader) {

    }

    private boolean call = false;
    private boolean callMenu = false;
    private String[] phones = null;
    private String[] phonesMenu = null;

    public void onCreateContextMenu0(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (phones != null) {
            int i = 0;
            for (String phone : phones) {
                menu.add(0, i, i, SyncContactsTask.toMSISDN(phone.trim()));
                i++;
            }

            phonesMenu = phones;
            callMenu = call;
            phones = null;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (callMenu) {
            startActivity(
                    new Intent(Intent.ACTION_DIAL).setData(
                            Uri.parse("tel:" + SyncContactsTask.toMSISDN(phonesMenu[item.getItemId()].trim()))));
        } else {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse("sms:" + SyncContactsTask.toMSISDN(phonesMenu[item.getItemId()].trim())));
            sendIntent.putExtra("sms_body", getString(R.string.invitation_text));
            startActivity(sendIntent);
        }

        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == btnAddToFriends) {
            if (data.friend == 0) {
                ProgressDialog.showProgressDialog(getActivity(), new AddFriendTask(false, profileId));
            } else if (data.friend == FRECV) {
                ProgressDialog.showProgressDialog(getActivity(), new AddFriendTask(true, profileId));
            }
        } else if (view == btnCall) {
            final String[] phone = data.phone.split(",");
            if (phone.length == 1) {
                startActivity(
                        new Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:" + SyncContactsTask.toMSISDN(phone[0].trim()))));
            } else {
                call = true;
                phones = phone;
                getActivity().openContextMenu(btnCall);
            }
        } else if (view == btnCancelInvitation) {
            ProgressDialog.showProgressDialog(getActivity(), new RemoveFriendTask(profileId));
        } else if (view == btnDeleteFriend) {
            ProgressDialog.showProgressDialog(getActivity(), new RemoveFriendTask(profileId));
        } else if (view == btnSendInvitation) {
            final String[] phone = data.phone.split(",");
            if (phone.length == 1) {
                Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                sendIntent.setData(Uri.parse("sms:" + SyncContactsTask.toMSISDN(phone[0].trim())));
                sendIntent.putExtra("sms_body", getString(R.string.invitation_text));
                startActivity(sendIntent);
            } else {
                call = false;
                phones = phone;
                getActivity().openContextMenu(btnCall);
            }
        } else if (view == btnSendMessage) {
            ContactsFragment.writeMessage(profileId, this);
        }
    }

    @Override
    public void onReady(long dialogId, long userId, long chatId) {
        getHost().writeMessage(dialogId, userId, chatId);
    }

    public static class LoadResult {
        public final long id;
        public final String name;
        public final int friend;
        public final String phone;
        public final String photo;
        public final String photoBig;

        public LoadResult(long id, String name, int friend, String phone, String photo, String photoBig) {
            this.id = id;
            this.name = name;
            this.friend = friend;
            this.phone = phone;
            this.photo = photo;
            this.photoBig = photoBig;
        }
    }

    public Host getHost() {
        return (Host) getActivity();
    }

    public static interface Host {
        void onProfileLoad(LoadResult result);

        void writeMessage(long dialogId, long userId, long chatId);
    }
}
