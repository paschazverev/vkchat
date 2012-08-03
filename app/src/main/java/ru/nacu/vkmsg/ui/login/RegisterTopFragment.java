package ru.nacu.vkmsg.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 6/21/12 3:10 PM
 */
public final class RegisterTopFragment extends SherlockFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.actionbar, container);
        v.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        final TextView title = (TextView) v.findViewById(R.id.title);
        title.setText(R.string.registration);
        v.findViewById(R.id.img).setVisibility(View.INVISIBLE);
        return v;
    }
}
