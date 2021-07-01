package in.skyrideraj.testpaypalintegration;

import android.app.Application;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;


public class TestPaypalApplicationClass  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //Initialize library for making API calls
        AndroidNetworking.initialize(getApplicationContext());
        AndroidNetworking.enableLogging();
    }
}
