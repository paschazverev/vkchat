package ru.nacu.vkmsg.ui.chat.menu;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import ru.android.common.UiTools;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.ui.chat.ChatFragment;

/**
 * @author quadro
 * @since 7/10/12 10:45 AM
 */
public final class AttachMenu extends LinearLayout implements View.OnClickListener {

    private Button btnPhoto;
    private Button btnSelectPhoto;
    private Button btnLocation;

    private final ChatFragment f;

    public AttachMenu(Context context, ChatFragment f) {
        super(context);
        this.f = f;
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.attach_menu);
        btnPhoto = new Button(getContext());
        btnSelectPhoto = new Button(getContext());
        btnLocation = new Button(getContext());

        btnLocation.setBackgroundResource(R.drawable.attach_bg);
        btnSelectPhoto.setBackgroundResource(R.drawable.attach_bg);
        btnPhoto.setBackgroundResource(R.drawable.attach_bg);

        btnLocation.setCompoundDrawablesWithIntrinsicBounds(R.drawable.attach_location, 0, 0, 0);
        btnLocation.setText(R.string.attach_location);
        btnPhoto.setCompoundDrawablesWithIntrinsicBounds(R.drawable.attach_photo, 0, 0, 0);
        btnPhoto.setText(R.string.make_photo);
        btnSelectPhoto.setCompoundDrawablesWithIntrinsicBounds(R.drawable.attach_gallery, 0, 0, 0);
        btnSelectPhoto.setText(R.string.choose_photo);

        int dp10 = (int) UiTools.dpToPix(10, getContext());
        btnLocation.setPadding(dp10, 0, 0, 0);
        btnPhoto.setPadding(dp10, 0, 0, 0);
        btnSelectPhoto.setPadding(dp10, 0, 0, 0);

        btnLocation.setTextColor(Color.WHITE);
        btnLocation.setTypeface(Typeface.DEFAULT_BOLD);
        btnPhoto.setTextColor(Color.WHITE);
        btnPhoto.setTypeface(Typeface.DEFAULT_BOLD);
        btnSelectPhoto.setTextColor(Color.WHITE);
        btnSelectPhoto.setTypeface(Typeface.DEFAULT_BOLD);

        addView(btnPhoto, new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(btnSelectPhoto, new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(btnLocation, new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnPhoto.setOnClickListener(this);
        btnSelectPhoto.setOnClickListener(this);
        btnLocation.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == btnPhoto) {
            f.addPhoto();
            f.hideMenu();
        } else if (view == btnLocation) {
            f.switchLocation();
            f.hideMenu();
        } else if (view == btnSelectPhoto) {
            f.addGalleryPhoto();
            f.hideMenu();
        }
    }
}
