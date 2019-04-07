package viewset.com.kkcamera.view.activity.opengl.texture;

public class FilterState {
    public enum Filter {

        NONE("原色", 0, new float[]{0.0f, 0.0f, 0.0f}),
        GRAY("黑白", 1, new float[]{0.299f, 0.587f, 0.114f}),
        COOL("冷色调", 2, new float[]{0.0f, 0.0f, 0.1f}),
        WARM("暖色调", 2, new float[]{0.1f, 0.1f, 0.0f}),
        BLUR("模糊", 3, new float[]{0.006f, 0.004f, 0.002f}),
        MAGN("放大镜", 4, new float[]{0.0f, 0.0f, 0.4f}),
        I1977("1977", 2, new float[]{0.16666f, 0.5f, 0.83333f});

        private String name;
        private int vChangeType;
        private float[] data;
        private boolean selected = false;

        Filter(String name, int vChangeType, float[] data) {
            this.name = name;
            this.vChangeType = vChangeType;
            this.data = data;
        }

        public int getType() {
            return vChangeType;
        }

        public float[] data() {
            return data;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getName() {
            return name;
        }
    }
}
