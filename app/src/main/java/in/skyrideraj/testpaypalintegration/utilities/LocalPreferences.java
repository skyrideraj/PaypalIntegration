package in.skyrideraj.testpaypalintegration.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import in.skyrideraj.testpaypalintegration.R;

public class LocalPreferences {
    private static final String TAG = LocalPreferences.class.getSimpleName();

    private static String SHARED_PREFS_NAME = "sf_paypal_integration";

    //store device data in preferences
    public static void storeDeviceData(Context context, String device_data){
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(context.getResources().getString(R.string.device_data), device_data);
        editor.commit();
    }

    //fetch device data from preferences
    public static String getDeviceData(Context context){
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String device_data = preferences.getString(context.getResources().getString(R.string.device_data), "Unknown");
        return device_data;
    }


}
