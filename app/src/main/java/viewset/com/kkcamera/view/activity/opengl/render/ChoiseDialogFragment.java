package viewset.com.kkcamera.view.activity.opengl.render;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ChoiseDialogFragment extends DialogFragment {



    public static ChoiseDialogFragment newInstance() {
        Bundle args = new Bundle();
        ChoiseDialogFragment fragment = new ChoiseDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
