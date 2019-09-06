package com.ambekar.healthtracker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {

    public static final String TAG = "MainActivity_History";
    public static boolean authInProgress = false;
    public static final int REQUEST_OAUTH = 100;
    public static final int REQUEST_PERMISSIONS = 200;
    GoogleApiClient fitApiClient;

    RecyclerView recyclerView;
    TextView tv_daily_steps;
    AdapterStepTable mAdapter;
    private static List<Step_Item> data = new ArrayList<>();
    AlertDialog popup_filters;

    private static String filter_by = "Date";
    private static String order_by = "DESC";
    private String daily_steps = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.rv_table);
        mAdapter = new AdapterStepTable(data);
        tv_daily_steps = findViewById(R.id.tv_daily_steps);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        findViewById(R.id.btn_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show_filter_popup();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(fitApiClient == null){
            check_permission();
            buildClient();
        }
        else if(fitApiClient.isConnected()) {
            request_data();
        }
    }

    private void request_data(){
        new RetriveSteps().execute();
        //new RetrieveStepsCurrentDate().execute();
    }

    //build client for the fit history access
    private void buildClient() {
        fitApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(this)
                .useDefaultAccount()
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        Log.i(TAG, "Connection failed. Cause: " + result.toString());
                        if (!result.hasResolution()) {
                            // Show the localized error dialog
                            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), MainActivity.this, 0).show();
                            return;
                        }
                        // The failure has a resolution. Resolve it.
                        if (!authInProgress) {
                            try {
                                Log.i(TAG, "Attempting to resolve failed connection");
                                authInProgress = true;
                                result.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Exception while starting resolution activity", e);
                            }
                        }
                    }
                })
                .build();

        fitApiClient.connect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OAUTH) {
            Log.e(TAG, "Auth request processed ");
            authInProgress = false;
            if (resultCode == Activity.RESULT_OK) {
                Log.e(TAG, "result ok" );
                Toast.makeText(getApplicationContext(),getString(R.string.auth_success), Toast.LENGTH_SHORT).show();
                if (!fitApiClient.isConnecting() && !fitApiClient.isConnected()) {
                    fitApiClient.connect();
                }
            }
            else {
                Toast.makeText(getApplicationContext(),getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                show_auth_failure_popup();
                Log.e(TAG,"Auth failure!");
            }
        }
    }

    //check if app has the history access permissions, if not then request them
    void check_permission(){
        String [] permissions = {Manifest.permission.ACTIVITY_RECOGNITION};
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,permissions , REQUEST_PERMISSIONS);
            Log.e(TAG, "Permission not granted");
        }
        else {
            Log.e(TAG, "Permission granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "r code "+requestCode);
        if(requestCode == REQUEST_PERMISSIONS){
            if(grantResults.length > 0){
                Log.e(TAG, "Permission is granted");
            }
            else {
                Log.e(TAG, "Permission is denied");
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(TAG,"APP connected to fit");
        request_data();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG,"APP connection suspended to fit");

    }


    //create request to retrieve step history for specific weeks
    public static DataReadRequest getDataRequestForWeeks(int weeks){
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1 * weeks);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = DateFormat.getDateInstance();
        Log.e(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.e(TAG, "Range End: " + dateFormat.format(endTime));

        final DataSource ds = new DataSource.Builder()
                .setAppPackageName("com.google.android.gms")
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(ds,DataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        return readRequest;
    }

    private void update_daily_counter(){
        tv_daily_steps.setText(getString(R.string.daily_counter, daily_steps));
    }


    class RetrsieveStepsCurrentDate extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            DailyTotalResult result = Fitness.HistoryApi.readDailyTotal(fitApiClient, DataType.TYPE_STEP_COUNT_DELTA).await(30, TimeUnit.SECONDS);
            if (result.getStatus().isSuccess()) {
                Log.e(TAG, "Daily success!");
                DataSet dataset = result.getTotal();
                for (DataPoint dp : dataset.getDataPoints()) {
                    for (Field field : dp.getDataType().getFields()) {
                        if (field.getName().equals("steps")) {
                            daily_steps = dp.getValue(field).toString();
                            Log.e(TAG, "Steps " + daily_steps);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            update_daily_counter();
        }
    }

    class RetriveSteps extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //clear the previous data
            data = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            DataReadRequest readRequest = getDataRequestForWeeks(2);
            DataReadResult dataReadResult = Fitness.HistoryApi.readData(fitApiClient, readRequest).await(30, TimeUnit.SECONDS);
            if (dataReadResult.getBuckets().size() > 0) {
                Log.e("History", "Number of buckets: " + dataReadResult.getBuckets().size());
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {

                        updateDataList(dataSet);
                    }
                }
            }

            //get current date steps
            DailyTotalResult result = Fitness.HistoryApi.readDailyTotal(fitApiClient, DataType.TYPE_STEP_COUNT_DELTA).await(30, TimeUnit.SECONDS);
            if (result.getStatus().isSuccess()) {
                Log.e(TAG, "Daily success!");
                DataSet dataset = result.getTotal();
                updateDataList(dataset);
                for (DataPoint dp : dataset.getDataPoints()) {
                    for (Field field : dp.getDataType().getFields()) {
                        if (field.getName().equals("steps")) {
                            daily_steps = dp.getValue(field).toString();
                            Log.e(TAG, "Steps " + daily_steps);
                        }
                    }
                }
            }

             return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //filter and update the recyclerview list and render the view
            filter_list();
            update_daily_counter();
        }
    }


    //Update the data List object with the steps values retrieved from the Fit History API
    private static void updateDataList(DataSet dataSet) {

        DateFormat dateFormat = DateFormat.getDateInstance();
        if(dataSet.getDataPoints().size() > 0){
            int nSteps = dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
            long ts =dataSet.getDataPoints().get(0).getStartTime(TimeUnit.MILLISECONDS);
            String date = dateFormat.format(ts);
            Step_Item new_item = new Step_Item(date,nSteps+"",ts);

            if(data.contains(new_item)){
                int item_index = data.indexOf(new_item);
                Step_Item temp_item = data.get(item_index);
                int steps = nSteps + Integer.parseInt(temp_item.getSteps());
                new_item = new Step_Item(date,steps+"",ts);
                data.remove(item_index);

            }

            data.add(new_item);
        }
    }

    //method to filter the list data with defined comparators
    void filter_list(){
        if(filter_by.equals("Date") && order_by.equals("ASC")){
            Collections.sort(data,compare_date);
        }
        else if(filter_by.equals("Date") && order_by.equals("DESC")){
            Collections.sort(data,compare_date_reversed);
        }
        else if(filter_by.equals("Steps") && order_by.equals("ASC")){
            Collections.sort(data,compare_steps);
        }
        else {
            Collections.sort(data,compare_steps_reversed);
        }
        //update adapter and view
        mAdapter.updateData(data);

    }

    //show filter history value popup
    private void show_filter_popup() {

        final View popup_view = getLayoutInflater().inflate(R.layout.filter_popup, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popup_view);

        final RadioGroup rg_filter = popup_view.findViewById(R.id.rg_filter);
        final RadioGroup rg_order = popup_view.findViewById(R.id.rg_order);

        RadioButton rb_date = popup_view.findViewById(R.id.rb_date);
        RadioButton rb_steps = popup_view.findViewById(R.id.rb_steps);
        RadioButton rb_asc = popup_view.findViewById(R.id.rb_asc);
        RadioButton rb_desc = popup_view.findViewById(R.id.rb_desc);

        //set radio_button view with previous values

        if(filter_by.equals("Date")){
            rb_date.setChecked(true);
        }
        else {
            rb_steps.setChecked(true);
        }

        if(order_by.equals("ASC")){
            rb_asc.setChecked(true);
        }
        else {
            rb_desc.setChecked(true);
        }

        popup_view.findViewById(R.id.btn_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(popup_filters !=null){

                    //get user input and set local filter variables
                    if (R.id.rb_date == rg_filter.getCheckedRadioButtonId()){
                        filter_by = "Date";
                    }
                    else {
                        filter_by = "Steps";
                    }

                    if(R.id.rb_asc == rg_order.getCheckedRadioButtonId()){
                        order_by = "ASC";
                    }
                    else {
                        order_by = "DESC";
                    }

                    filter_list();
                    popup_filters.dismiss();
                }
            }
        });

        popup_filters= builder.create();
        popup_filters.show();

    }

    //show popup for auth failure
    private void show_auth_failure_popup(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(getString(R.string.auth_failure_message));
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                buildClient();
            }
        });

        alertDialogBuilder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });

        alertDialogBuilder.create().show();
    }



    //comparators for filtering the history values
    Comparator<Step_Item> compare_date = new Comparator<Step_Item>() {
        @Override
        public int compare(Step_Item step_item, Step_Item t1) {
            long ts1 = step_item.getTimestamp();
            long ts2 = t1.getTimestamp();
            return (int) (ts1 - ts2);
        }
    };

    Comparator<Step_Item> compare_date_reversed = new Comparator<Step_Item>() {
        @Override
        public int compare(Step_Item t1, Step_Item step_item) {
            long ts1 = step_item.getTimestamp();
            long ts2 = t1.getTimestamp();
            return (int) (ts1 - ts2);
        }
    };


    Comparator<Step_Item> compare_steps = new Comparator<Step_Item>() {
        @Override
        public int compare(Step_Item step_item, Step_Item t1) {
            int steps1 = Integer.parseInt(step_item.getSteps());
            int steps2 = Integer.parseInt(t1.getSteps());
            return (steps1 - steps2);
        }
    };

    Comparator<Step_Item> compare_steps_reversed = new Comparator<Step_Item>() {
        @Override
        public int compare(Step_Item t1, Step_Item step_item) {
            int steps1 = Integer.parseInt(step_item.getSteps());
            int steps2 = Integer.parseInt(t1.getSteps());
            return (steps1 - steps2);
        }
    };




}
