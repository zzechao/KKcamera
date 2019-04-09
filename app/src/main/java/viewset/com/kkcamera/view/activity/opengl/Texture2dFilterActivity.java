package viewset.com.kkcamera.view.activity.opengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.imay.capturefilter.widget.ICAutoAdjustRecylerView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import viewset.com.kkcamera.R;
import viewset.com.kkcamera.view.activity.opengl.texture.ColorTexture2dFilterRender;
import viewset.com.kkcamera.view.activity.opengl.texture.FilterState;

/**
 * 添加滤镜的TextureFilter
 */
public class Texture2dFilterActivity extends AppCompatActivity {

    @BindView(R.id.gltexture2d)
    GLSurfaceView glTextureView;

    @BindView(R.id.recycler)
    ICAutoAdjustRecylerView mRecyclerView;

    private Unbinder unbinder;

    ColorTexture2dFilterRender mRenderer;

    private RecyclerView.Adapter mAdapter;

    private FilterState.Filter[] filters = {
            FilterState.Filter.NONE,
            FilterState.Filter.GRAY,
            FilterState.Filter.COOL,
            FilterState.Filter.WARM,
            FilterState.Filter.BLUR,
            FilterState.Filter.N1977,
            FilterState.Filter.BEAUTY};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture2d);
        ButterKnife.bind(this);

        unbinder = ButterKnife.bind(this);

        try {
            mRenderer = new ColorTexture2dFilterRender(this);
            glTextureView.setEGLContextClientVersion(2);
            glTextureView.setRenderer(mRenderer);
            glTextureView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

            Bitmap bitmap = BitmapFactory.decodeStream(getResources().getAssets().open("texture/timg.jpeg"));
            mRenderer.setBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initFitter();
    }

    private void initFitter() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayout.HORIZONTAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setPxPerMillsec(1.0f);
        mRecyclerView.setVisibility(View.VISIBLE);

        mAdapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(LayoutInflater.from(getBaseContext()).inflate(R.layout.capturefilter_item_filter, null)) {
                };
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                FilterState.Filter filter = filters[i];
                TextView textView = viewHolder.itemView.findViewById(R.id.tv_name);
                if (filter.isSelected()) {
                    textView.setSelected(true);
                } else {
                    textView.setSelected(false);
                }
                textView.setText(filter.getName());
                textView.setOnClickListener(new OnItemClickListener(i));
            }

            @Override
            public int getItemCount() {
                return filters.length;
            }


        };


        mRecyclerView.setAdapter(mAdapter);
        filters[0].setSelected(true);
        mAdapter.notifyItemChanged(0);
    }

    class OnItemClickListener implements View.OnClickListener {

        int position;

        public OnItemClickListener(int i) {
            position = i;
        }

        @Override
        public void onClick(View v) {
            for (FilterState.Filter filter : filters) {
                filter.setSelected(false);
            }
            filters[position].setSelected(true);
            mAdapter.notifyDataSetChanged();
            mRenderer.setFilter(filters[position]);
            glTextureView.requestRender();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glTextureView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glTextureView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
