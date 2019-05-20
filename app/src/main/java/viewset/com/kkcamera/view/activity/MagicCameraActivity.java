package viewset.com.kkcamera.view.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.seu.magicfilter.MagicEngine;
import com.seu.magicfilter.filter.helper.MagicFilterType;
import com.seu.magicfilter.widget.MagicCameraView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;

public class MagicCameraActivity extends AppCompatActivity {

    @BindView(R.id.miv)
    MagicCameraView magicCameraView;

    private Unbinder ub;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magic_camera);
        ub = ButterKnife.bind(this);

        new MagicEngine.Builder().build(magicCameraView);
    }

    @Override
    protected void onResume() {
        magicCameraView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        magicCameraView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ub.unbind();
    }

    @OnClick(R.id.just)
    public void onViewClick(View view) {
        magicCameraView.setFilter(MagicFilterType.N1977);
    }
}
