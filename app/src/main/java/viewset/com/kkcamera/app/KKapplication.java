package viewset.com.kkcamera.app;

import android.app.Application;
import android.content.Context;

public class KKapplication extends Application {

    private static KKapplication instance;

    public static KKapplication get(Context context){
        return (KKapplication)context.getApplicationContext();
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }
}
