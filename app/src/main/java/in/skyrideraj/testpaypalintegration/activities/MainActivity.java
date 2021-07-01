package in.skyrideraj.testpaypalintegration.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreePaymentResultListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.BraintreePaymentResult;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.meet.quicktoast.Quicktoast;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import in.skyrideraj.testpaypalintegration.R;
import in.skyrideraj.testpaypalintegration.utilities.LocalPreferences;
import in.skyrideraj.testpaypalintegration.utilities.StringUtilities;


public class MainActivity extends AppCompatActivity {

    //Log tag
    private static final String TAG = MainActivity.class.getSimpleName();

    //BrainTree fragment
    private BraintreeFragment mBraintreeFragment;

    //Custom Toast
    private Quicktoast toast;

    //Listeners
    PaymentMethodNonceCreatedListener paymentMethodNonceCreatedListener;
    ConfigurationListener configurationListener;
    BraintreeErrorListener braintreeErrorListener;
    BraintreePaymentResultListener braintreePaymentResultListener;
    BraintreeCancelListener braintreeCancelListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize custom Toast library
        toast = new Quicktoast(this);

        //UI Elements initialization
        EditText amountEt = findViewById(R.id.amountEt);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView progressText = findViewById(R.id.progressText);
        Button payPalButton = findViewById(R.id.payPalCheckoutButton);
        CheckBox simulateDeclineCheckBox = findViewById(R.id.simulateDeclineCheckBox);

        payPalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Amount input validation
                if(!StringUtilities.isNumeric(amountEt.getText().toString())){
                    toast.swarn("Invalid Billing Amount!");
                    return;
                }

                //Create a Paypal Sale Intent for authorization
                PayPalRequest request = new PayPalRequest(amountEt.getText().toString())
                        .currencyCode("USD")
                        .shippingAddressEditable(true)
                        .shippingAddressRequired(true)
                        .displayName("Test Paypal Merchant")
                        .intent(PayPalRequest.INTENT_SALE);
                //We are requesting one time payment
                PayPal.requestOneTimePayment(mBraintreeFragment, request);
            }
        });


        paymentMethodNonceCreatedListener = new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                //We got the authorized payment Nonce
                if (paymentMethodNonce instanceof PayPalAccountNonce) {
                    PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce)paymentMethodNonce;

                    // Access additional information
                    String email = payPalAccountNonce.getEmail();
                    String firstName = payPalAccountNonce.getFirstName();
                    String lastName = payPalAccountNonce.getLastName();
                    String phone = payPalAccountNonce.getPhone();
                    Log.i(TAG, "email - "+email);
                    Log.i(TAG, "firstName - "+firstName);
                    Log.i(TAG, "lastName - "+lastName);
                    Log.i(TAG, "phone - "+phone);

                    // See PostalAddress.java for details
                    PostalAddress billingAddress = payPalAccountNonce.getBillingAddress();
                    PostalAddress shippingAddress = payPalAccountNonce.getShippingAddress();
                    Log.i(TAG, "billingAddress - "+billingAddress);
                    Log.i(TAG, "shippingAddress - "+shippingAddress);
                }
                Log.i(TAG, "Payment Method Nonce - "+paymentMethodNonce.getNonce());

                //Start showing progress
                progressBar.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);

                //Formatting amount as decimal with 2 places
                String amount = String.format("%.2f", Double.parseDouble(amountEt.getText().toString()));

                //Simulate transaction decline error?
                boolean simulateDeclineTransaction = simulateDeclineCheckBox.isChecked();

                //Server POST method API call to submit the Paypal Transaction Sale https://developers.braintreepayments.com/reference/request/transaction/sale/php
                AndroidNetworking.post(getString(R.string.server_link))
                        //Post params
                        .addBodyParameter(getString(R.string.payment_method_nonce), simulateDeclineTransaction? "fake-paypal-one-time-nonce" :  paymentMethodNonce.getNonce())
                        .addBodyParameter(getString(R.string.device_data), LocalPreferences.getDeviceData(MainActivity.this))
                        .addBodyParameter(getString(R.string.amount), amount)
                        .addBodyParameter(getString(R.string.order_id), UUID.randomUUID().toString())
                        .setTag(getString(R.string.transaction_tag))
                        .setPriority(Priority.MEDIUM)
                        .build()
                        //get response as JSON
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                progressBar.setVisibility(View.INVISIBLE);
                                progressText.setVisibility(View.INVISIBLE);
                                try {
                                    Log.i(TAG,"Json response "+response.toString());
                                    Boolean status = response.getBoolean("success");

                                    //Successful transaction
                                    if(status){
                                        JSONObject transaction_json = response.getJSONObject("transaction");
                                        String display_message = "Transaction successful. \nTransaction ref. no. : "+transaction_json.getString("id") + "\nStatus - "+transaction_json.optString("status","");
                                        Log.i(TAG,display_message);
                                        showDialogBox(MainActivity.this,"Transaction Success",display_message,true);
                                    }

                                    //Unsuccessful transaction
                                    else{
                                        JSONObject transaction_json = response.getJSONObject("transaction");
                                        String display_message = "Error - "+response.getString("message")+"\n Transaction ID :"+transaction_json.optString("id","Unknown") + "\nReason - "+transaction_json.optString("status","");
                                        Log.i(TAG,display_message);
                                        showDialogBox(MainActivity.this,"Transaction Error",display_message,false);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    toast.swarn("There was an error confirming the transaction. Check internet connection or try again later.");
                                }
                            }
                            @Override
                            public void onError(ANError error) {
                                // handle error
                                toast.swarn("Server error while confirming transaction. Try again later.");
                            }
                        });
            }
        };

        configurationListener = new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                Log.i(TAG, "On Braintree config - "+configuration.toJson());

            }
        };

        braintreeErrorListener = new BraintreeErrorListener() {
            @Override
            public void onError(Exception error) {
                if (error instanceof ErrorWithResponse) {
                    ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
                    Log.e(TAG, "On Error - "+errorWithResponse.getMessage());
                    toast.lwarn("Error - "+errorWithResponse.getMessage());
                }
            }
        };

        braintreePaymentResultListener = new BraintreePaymentResultListener() {
            @Override
            public void onBraintreePaymentResult(BraintreePaymentResult result) {
                Log.d(TAG, "On Payment Result - "+result.toString());
            }
        };

        braintreeCancelListener = new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                Log.e(TAG, "On Cancelled - "+requestCode);
                toast.swarn("Payment process was cancelled!");
            }
        };


        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, getString(R.string.braintree_sandbox_apikey));
            //Brain Tree Fragment is ready to use!

            // Add all BrainTree SDK Listeners
            mBraintreeFragment.addListener(paymentMethodNonceCreatedListener);
            mBraintreeFragment.addListener(configurationListener);
            mBraintreeFragment.addListener(braintreeErrorListener);
            mBraintreeFragment.addListener(braintreePaymentResultListener);
            mBraintreeFragment.addListener(braintreeCancelListener);
            DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
                @Override
                public void onResponse(String deviceData) {
                    // store device data
                    LocalPreferences.storeDeviceData(MainActivity.this,deviceData);
                    Log.i(TAG, "Device Data collected - "+deviceData);
                }
            });

        } catch (InvalidArgumentException e) {
            // There was an issue with authorization
            e.printStackTrace();
            toast.swarn("Error in Authorization with BrainTree SDK!");
        }


    }

    //Show Transaction details dialog
    void showDialogBox(Context context,String title, String message, boolean success){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder
                .setTitle(title)
                .setMessage(message)

                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(success?(R.drawable.success_icon):(R.drawable.alert_icon));
        final AlertDialog dialog = builder.create();
        dialog.show();

        //We dont need Negative button
        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

    }


}