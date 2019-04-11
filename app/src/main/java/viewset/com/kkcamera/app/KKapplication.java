package viewset.com.kkcamera.app;

import android.app.Application;
import android.content.Context;

/**
 * https://blog.csdn.net/qqchenjian318/article/details/78396428
 */
public class KKapplication extends Application {

    private static KKapplication instance;

    public static KKapplication get(Context context) {
        return (KKapplication) context.getApplicationContext();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Constants.init(this);
    }
}
