package com.qdocs.smartschool.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.cardview.widget.CardView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.qdocs.smartschool.R;
import com.qdocs.smartschool.students.StudentAttendance;
import com.qdocs.smartschool.students.StudentDashboard;
import com.qdocs.smartschool.students.StudentHomework;
import com.qdocs.smartschool.students.StudentTasks;
import com.qdocs.smartschool.students.StudentTransportRoutes;
import com.qdocs.smartschool.utils.Constants;
import com.qdocs.smartschool.utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static android.widget.Toast.makeText;

public class StudentDashboardFragment extends Fragment {
    private static final String TAG = "StudentDashboardFragmen";
    private static final String CHANNEL_ID = "My Soft Smart School";
    private static int NOTIFICATION_DISTANCE = 200;
    private static boolean NOTIFICATION_FLAG = true;

    private FirebaseDatabase fbDatabase;
    private DatabaseReference dbRef;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Double cuLatitude;
    private Double cuLongitude;

    RelativeLayout transportLayout, attendanceLayout, homeworkLayout, pendingTaskLayout, transportTimeLayout;
    TextView transportTime, transportDistance, attendanceValue, homeworkValue, pendingTaskValue;
    CardView transportCard, attendanceCard, homeworkCard, pendingTaskCard;
    FrameLayout calenderFrame;
    ProgressBar locationPb;
    ImageView gMapBtn, locErrorIcon;
    ArrayList<String> moduleCodeList = new ArrayList<String>();
    ArrayList<String> moduleStatusList = new ArrayList<String>();
    public Map<String, String> headers = new HashMap<String, String>();
    public Map<String, String> params = new Hashtable<String, String>();
    JSONArray modulesJson;

    public StudentDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // loadData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        fbDatabase = FirebaseDatabase.getInstance();
        dbRef = fbDatabase.getReference("root/vehicles/");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        View mainView = inflater.inflate(R.layout.student_dashboard_fragment, container, false);

        transportLayout = mainView.findViewById(R.id.student_dashboard_fragment_transportView);
        attendanceLayout = mainView.findViewById(R.id.student_dashboard_fragment_attendanceView);
        homeworkLayout = mainView.findViewById(R.id.student_dashboard_fragment_homeworkView);
        pendingTaskLayout = mainView.findViewById(R.id.student_dashboard_fragment_pendingTaskView);

        transportCard = mainView.findViewById(R.id.student_dashboard_fragment_transportCard);
        attendanceCard = mainView.findViewById(R.id.student_dashboard_fragment_attendanceCard);
        homeworkCard = mainView.findViewById(R.id.student_dashboard_fragment_homeworkCard);
        pendingTaskCard = mainView.findViewById(R.id.student_dashboard_fragment_pendingTaskCard);

        locationPb = mainView.findViewById(R.id.location_time_loading_pb);
        gMapBtn = mainView.findViewById(R.id.gmap_iv);
        locErrorIcon = mainView.findViewById(R.id.location_error_iv);

        transportTimeLayout = mainView.findViewById(R.id.transport_time_view);
        transportDistance = mainView.findViewById(R.id.student_dashboard_fragment_transport_distance);
        transportTime = mainView.findViewById(R.id.student_dashboard_fragment_transport_time);
        attendanceValue = mainView.findViewById(R.id.student_dashboard_fragment_attendance_value);
        homeworkValue = mainView.findViewById(R.id.student_dashboard_fragment_homework_value);
        pendingTaskValue = mainView.findViewById(R.id.student_dashboard_fragment_pendingTask_value);

        calenderFrame = mainView.findViewById(R.id.dashboardViewPager);

        loadData();


        transportLayout.setOnClickListener(view -> {
            Intent asd = new Intent(getActivity().getApplicationContext(), StudentTransportRoutes.class);
            getActivity().startActivity(asd);
        });

        attendanceLayout.setOnClickListener(view -> {
            Intent asd = new Intent(getActivity().getApplicationContext(), StudentAttendance.class);
            getActivity().startActivity(asd);
        });

        homeworkLayout.setOnClickListener(view -> {
            Intent asd = new Intent(getActivity().getApplicationContext(), StudentHomework.class);
            getActivity().startActivity(asd);
        });

        pendingTaskLayout.setOnClickListener(view -> {
            Intent asd = new Intent(getActivity().getApplicationContext(), StudentTasks.class);
            getActivity().startActivity(asd);
        });
        Log.e("STATUS", "onCreateView");
        return mainView;
    }

    private void loadData() {
        decorate();
        loadFragment(new DashboardCalender());

        if (Utility.getSharedPreferences(getActivity(), Constants.loginType).equals("parent")) {
            if (Utility.isConnectingToInternet(getActivity())) {
                params.put("student_id", Utility.getSharedPreferences(getActivity().getApplicationContext(), Constants.studentId));
                params.put("date_from", getDateOfMonth(new Date(), "first"));
                params.put("date_to", getDateOfMonth(new Date(), "last"));
                params.put("role", Utility.getSharedPreferences(getActivity(), Constants.loginType));
                params.put("user_id", Utility.getSharedPreferences(getActivity(), Constants.userId));
                JSONObject obj = new JSONObject(params);
                Log.e("params~~~~~~~~", obj.toString());
                getDataFromApi(obj.toString());
            } else {
                makeText(getActivity(), R.string.noInternetMsg, Toast.LENGTH_SHORT).show();
            }

        } else {
            if (Utility.isConnectingToInternet(getActivity())) {
                params.put("student_id", Utility.getSharedPreferences(getActivity().getApplicationContext(), Constants.studentId));
                params.put("date_from", getDateOfMonth(new Date(), "first"));
                params.put("date_to", getDateOfMonth(new Date(), "last"));
                params.put("role", Utility.getSharedPreferences(getActivity(), Constants.loginType));
                JSONObject obj = new JSONObject(params);
                Log.e("params ", obj.toString());
                getDataFromApi(obj.toString());
            } else {
                makeText(getActivity(), R.string.noInternetMsg, Toast.LENGTH_SHORT).show();
            }

        }

        try {
            JSONArray modulesArray = new JSONArray(Utility.getSharedPreferences(getActivity().getApplicationContext(), Constants.modulesArray));

            if (modulesArray.length() != 0) {
                ArrayList<String> moduleCodeList = new ArrayList<String>();
                ArrayList<String> moduleStatusList = new ArrayList<String>();

                for (int i = 0; i < modulesArray.length(); i++) {
                    // TODO: Not yet implemented (from backend)
                    if (modulesArray.getJSONObject(i).getString("short_code").equals("student_transport")
                            && modulesArray.getJSONObject(i).getString("is_active").equals("0")) {
                        transportCard.setVisibility(View.GONE);
                    }
                    if (modulesArray.getJSONObject(i).getString("short_code").equals("student_attendance")
                            && modulesArray.getJSONObject(i).getString("is_active").equals("0")) {
                        attendanceCard.setVisibility(View.GONE);
                    }
                    if (modulesArray.getJSONObject(i).getString("short_code").equals("homework")
                            && modulesArray.getJSONObject(i).getString("is_active").equals("0")) {
                        homeworkCard.setVisibility(View.GONE);
                    }
                    if (modulesArray.getJSONObject(i).getString("short_code").equals("calendar_to_do_list")
                            && modulesArray.getJSONObject(i).getString("is_active").equals("0")) {
                        pendingTaskCard.setVisibility(View.GONE);
                        calenderFrame.setVisibility(View.GONE);
                    }
                }
            }
        } catch (JSONException e) {
            Log.d("Error", e.toString());
        }
    }


    private void getDataFromApi(String bodyParams) {

        Log.e("RESULT PARAMS", bodyParams);
        final ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Loading");
        pd.setCancelable(false);
        pd.show();

        final String requestBody = bodyParams;
        String url = Utility.getSharedPreferences(getActivity().getApplicationContext(), "apiUrl") + Constants.getDashboardUrl;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String result) {
                if (result != null) {
                    pd.dismiss();
                    try {
                        Log.e("Result", result);
                        JSONObject object = new JSONObject(result);
                        Log.i("TAG", "onResponse: " + object.toString());
                        //TODO success
                        String success = "1"; //object.getString("success");
                        if (success.equals("1")) {
                            if (object.getString("transport").equals("1")) {
                                String transportNo = object.getString("transport_no");
                                transportDistance.setText(transportNo);
                                loadTransportData(transportNo);
//                                transportTime.setText(object.getString("student_attendence_percentage") + "%");
                            } else {
                                transportCard.setVisibility(View.GONE);
                            }

                            if (object.getString("attendence_type").equals("0")) {
                                attendanceValue.setText(object.getString("student_attendence_percentage") + "%");
                            } else {
                                attendanceCard.setVisibility(View.GONE);
                            }

                            homeworkValue.setText(object.getString("student_homework_incomplete"));
                            pendingTaskValue.setText(object.getString("student_incomplete_task"));

                            Utility.setSharedPreference(getActivity().getApplicationContext(), Constants.classId, object.getString("class_id"));
                            Utility.setSharedPreference(getActivity().getApplicationContext(), Constants.sectionId, object.getString("section_id"));


                        } else {
                            Toast.makeText(getActivity().getApplicationContext(), object.getString("errorMsg"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    pd.dismiss();
                    Toast.makeText(getActivity().getApplicationContext(), R.string.noInternetMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                pd.dismiss();
                Log.e("Volley Error", volleyError.toString());
                Toast.makeText(getActivity(), R.string.apiErrorMsg, Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                headers.put("Client-Service", Constants.clientService);
                headers.put("Auth-Key", Constants.authKey);
                headers.put("Content-Type", Constants.contentType);
                headers.put("User-ID", Utility.getSharedPreferences(getActivity().getApplicationContext(), "userId"));
                headers.put("Authorization", Utility.getSharedPreferences(getActivity().getApplicationContext(), "accessToken"));
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());//Creating a Request Queue
        requestQueue.add(stringRequest);//Adding request to the queue
    }

    private void loadTransportData(String transportNo) {
        Log.d(TAG, "loadTransportData: Called");

        float[] result = new float[1];
        getUserCurrentLocation();
        if (isPermissionGranted()) {
            locationPb.setVisibility(View.GONE);
            gMapBtn.setVisibility(View.VISIBLE);

            dbRef.child(transportNo).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Double latitude = (Double) Double.valueOf(snapshot.child("latitude").getValue().toString());
                    Double longitude = (Double) Double.valueOf(snapshot.child("longitude").getValue().toString());

                    Log.i(TAG, "onDataChange: " + latitude + " | " + longitude);

                    Location.distanceBetween(cuLatitude, cuLongitude, latitude, longitude, result);

                    int distance = (int) result[0];
//                if (distance < 1000)
                    Log.d(TAG, "onDataChange: Distance " + result + " | " + result[0] + " | " + distance);

                    String distanceString = distance < 1000 ? distance + " meters away" : distance + " Kms away";
                    transportDistance.setText(distanceString);

                    if (distance <= NOTIFICATION_DISTANCE) {
                        Log.d(TAG, "onDataChange: distance <= notifDistanceInt");
                        showNotification(distanceString);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "onCancelled: Firebase Database - " + error.getMessage());
                }
            });
        } else
            makeText(requireContext(), "Location Permission is not Granted", Toast.LENGTH_SHORT).show();
    }

    private void showNotification(String distance) {
        Log.d(TAG, "showNotification: Called");

        if (NOTIFICATION_FLAG) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Transport Location", NotificationManager.IMPORTANCE_HIGH);

                NotificationManager notificationManager = requireActivity().getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                            .setSmallIcon(R.drawable.app_icon) //set icon for notification
                            .setContentTitle("Transport location") //set title of notification
                            .setContentText("The vehicle is " + distance)//this is notification message
                            .setAutoCancel(true) // makes auto cancel of notification
                            .setDefaults(Notification.PRIORITY_MAX)
                            .setPriority(NotificationCompat.PRIORITY_MAX); //set priority of notification


            Intent notificationIntent = new Intent(requireContext(), StudentTransportRoutes.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //notification message will get at NotificationView
//            notificationIntent.putExtra("message", "This is a notification message");

            PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            // Add as notification
            NotificationManager manager = (NotificationManager) requireActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(0, builder.build());
            NOTIFICATION_FLAG = false;
        }
    }

    private boolean isPermissionGranted() {

        Boolean permissionGranted = false;

//        if (Build.VERSION.PREVIEW_SDK_INT >= Build.VERSION_CODES.M) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true;
            getUserCurrentLocation();
            if (!isGPSEnabled()) {
                Log.i(TAG, "isPermissionGranted: GPS is ON");
            } else
                makeText(requireContext(), "Please turn ON GPS location", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
        }
//        }
        return permissionGranted;
    }

    @SuppressLint("MissingPermission")
    private void getUserCurrentLocation() {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
            Location location = task.getResult();
            if (location != null) {
                cuLatitude = location.getLatitude();
                cuLongitude = location.getLongitude();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 102) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: Granted");
            } else {
                Log.i(TAG, "onRequestPermissionsResult: Not Granted");
            }
        }
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void decorate() {
        transportLayout.setBackgroundColor(Color.parseColor(Utility.getSharedPreferences(getActivity().getApplicationContext(), Constants.secondaryColour)));
        attendanceLayout.setBackgroundColor(Color.parseColor(Utility.getSharedPreferences(getActivity().getApplicationContext(), Constants.secondaryColour)));
        homeworkLayout.setBackgroundColor(Color.parseColor(Utility.getSharedPreferences(getActivity().getApplicationContext(), Constants.secondaryColour)));
        pendingTaskLayout.setBackgroundColor(Color.parseColor(Utility.getSharedPreferences(getActivity().getApplicationContext(), Constants.secondaryColour)));
    }

    private void loadFragment(Fragment fragment) {
        // load fragment
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(calenderFrame.getId(), fragment);
//        transaction.addToBackStack(null);
        transaction.commit();
    }

    public static String getDateOfMonth(Date date, String index) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (index.equals("first")) {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        } else {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(cal.getTime());
    }
}
