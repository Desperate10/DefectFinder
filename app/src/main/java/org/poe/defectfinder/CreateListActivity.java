package org.poe.defectfinder;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.databinding.ActivityCreateListBinding;
import org.poe.defectfinder.entities.Objects;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.MyApi;
import org.poe.defectfinder.network.ObjectsResponse;
import org.poe.defectfinder.other.BackgroundTask;
import org.poe.defectfinder.other.DefectResponse;
import org.poe.defectfinder.other.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateListActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener, ObjectListDialogFragment.OnInputListener {

    private static final int REQUEST_LOCATION = 1;
    private static final long LOCATION_REFRESH_TIME = 5000;
    private static final float LOCATION_REFRESH_DISTANCE = 50;
    private static final String TP_OBJECT = "/tp.txt";
    private static final String LINE0_4_OBJECT = "-0";
    private static final String LINE6_10_OBJECT = "/lines6_10.txt";
    private static final String KL_OBJECT = "/kl.txt";

    ActivityCreateListBinding binding;
    private int resource;
    private String objectType;

    LocationManager mLocationManager;

    private String linename;
    private ArrayAdapter<String> line_adapter = null;
    private ArrayList<String> objects = null;
    private String modificator;
    private String tabel, filial, token;
    private boolean forceRefresh = false;
    private HashMap<String, String> lineToTplnr = null;

    private MyApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateListBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        ActivityCompat.requestPermissions( this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            modificator = bundle.getString("mod");
            tabel = bundle.getString("tabel");
            if (modificator.equals(Default.CONFIRM_MODIFICATOR)) {
                binding.activityName.setText(getString(R.string.creating_fixed_list));
                binding.header.setVisibility(View.GONE);
                binding.planSpinner.setVisibility(View.GONE);
            }
        }

        api = ClientApi.getInstance(this).getCachedApi();

        SharedPreferences sharedPreferences = getSharedPreferences("filial", MODE_PRIVATE);
        filial = sharedPreferences.getString("filial", "00");
        token = sharedPreferences.getString("token", "");

        binding.planSpinner.setOnItemSelectedListener(this);
        binding.lineOrTpSpinner.setOnItemSelectedListener(this);
        /*binding.listSpinner.setTitle(getString(R.string.choose_object));
        binding.listSpinner.setPositiveButton(getString(R.string.close_tab));
        binding.listSpinner.setOnItemSelectedListener(this);*/
        binding.oprOrSpan.setOnItemSelectedListener(this);
        binding.listSpinner.setEnabled(false);
        binding.listSpinner.setOnClickListener(this);
        binding.locationBtn.setOnClickListener(this);
        binding.continueBtn.setOnClickListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        /*try{
            mLocationManager.removeUpdates(mLocationListener);
        } catch (NullPointerException e) {}*/
    }

    private void initListSpinner(int position) {
        binding.listSpinner.setEnabled(true);
        // binding.inputOpr.setEnabled(true);
        // binding.oprOrSpan.setEnabled(true);
        binding.inputOpr.setVisibility(View.VISIBLE);
        binding.oprOrSpan.setVisibility(View.VISIBLE);
        objects = new ArrayList<>();

        switch (position) {
            case 0:
                resource = R.raw.line0_4;
                objectType = LINE0_4_OBJECT;
                break;
            case 1:
                resource = R.raw.line6_10;
                objectType = LINE6_10_OBJECT;
                break;
            case 2:
                binding.listSpinner.setEnabled(true);
                binding.inputOpr.setVisibility(View.INVISIBLE);
                binding.oprOrSpan.setVisibility(View.INVISIBLE);
                resource = R.raw.tp;
                objectType = TP_OBJECT;
                break;
            case 3:
                binding.listSpinner.setEnabled(true);
                binding.inputOpr.setVisibility(View.INVISIBLE);
                binding.oprOrSpan.setVisibility(View.INVISIBLE);
                resource = R.raw.kl;
                objectType = KL_OBJECT;
                break;
        }

        if (line_adapter != null)
            line_adapter.notifyDataSetChanged();

        getObjectsApiCall(objectType);
    }

    private void getObjectsApiCall(String objectType) {
        lineToTplnr = new HashMap<>();
        binding.llProgressBar.getRoot().setVisibility(View.VISIBLE);
        //switch (objectType) {
          //  case LINE0_4_OBJECT:
                api.getLowLineObjects(filial.substring(0,2), objectType, "Bearer "+token, forceRefresh? "no-cache" : null).enqueue(new Callback<List<ObjectsResponse>>() {
                    @Override
                    public void onResponse(Call<List<ObjectsResponse>> call, Response<List<ObjectsResponse>> response) {
                        List<ObjectsResponse> results = response.body();
                        for (int i = 0; i<results.size(); i++) {
                            objects.add(results.get(i).getInvnr()+" "+results.get(i).getPltxt());
                            lineToTplnr.put(results.get(i).getPltxt(), results.get(i).getTplnr());
                        }
                        setLineAdapter(objects);
                        binding.llProgressBar.getRoot().setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(Call<List<ObjectsResponse>> call, Throwable t) {}
                });
              //  break;
          //  case LINE6_10_OBJECT:
                api.getLineObjects(filial.substring(0,2), objectType, "Bearer "+token, forceRefresh? "no-cache" : null).enqueue(new Callback<List<ObjectsResponse>>() {
                    @Override
                    public void onResponse(Call<List<ObjectsResponse>> call, Response<List<ObjectsResponse>> response) {
                        List<ObjectsResponse> results = response.body();
                        for (int i = 0; i<results.size(); i++) {
                            objects.add(results.get(i).getInvnr()+" "+results.get(i).getPltxt());
                            lineToTplnr.put(results.get(i).getPltxt(), results.get(i).getTplnr());
                        }
                        setLineAdapter(objects);
                        binding.llProgressBar.getRoot().setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(Call<List<ObjectsResponse>> call, Throwable t) {}
                });
            //    break;
           // case TP_OBJECT:
                api.getTpObjects(filial.substring(0,2), "Bearer "+token, forceRefresh? "no-cache" : null).enqueue(new Callback<List<ObjectsResponse>>() {
                    @Override
                    public void onResponse(Call<List<ObjectsResponse>> call, Response<List<ObjectsResponse>> response) {
                        List<ObjectsResponse> results = response.body();
                        for (int i = 0; i<results.size(); i++) {
                            objects.add(results.get(i).getInvnr()+" "+results.get(i).getPltxt());
                            lineToTplnr.put(results.get(i).getPltxt(), results.get(i).getTplnr());
                        }
                        setLineAdapter(objects);
                        binding.llProgressBar.getRoot().setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(Call<List<ObjectsResponse>> call, Throwable t) {}
                });
            //    break;
            //case KL_OBJECT:
                api.getKlObjects(filial.substring(0,2), "Bearer "+token, forceRefresh? "no-cache" : null).enqueue(new Callback<List<ObjectsResponse>>() {
                    @Override
                    public void onResponse(Call<List<ObjectsResponse>> call, Response<List<ObjectsResponse>> response) {
                        List<ObjectsResponse> results = response.body();
                        for (int i = 0; i<results.size(); i++) {
                            objects.add(results.get(i).getInvnr()+" "+results.get(i).getPltxt());
                            lineToTplnr.put(results.get(i).getPltxt(), results.get(i).getTplnr());
                        }
                        setLineAdapter(objects);
                        binding.llProgressBar.getRoot().setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(Call<List<ObjectsResponse>> call, Throwable t) {}
                });
              //  break;
        //}
        forceRefresh = false;
    }

    private void setLineAdapter(ArrayList<String> obj) {
        line_adapter = new ArrayAdapter<>(CreateListActivity.this, android.R.layout.simple_list_item_1, obj);
        // Specify the layout to use when the list of choices appears
        line_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the adapter to th spinner
        //binding.listSpinner.setAdapter(line_adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh_button) {
            forceRefresh = true;
            getObjectsApiCall(objectType);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*public ArrayList<String> readFromTxt(String objectType) {
        ArrayList<String> text = new ArrayList<>();

        try {
            //InputStream inputStream = getResources().openRawResource(typeResource);
            InputStream inputStream = getAssets().open(filial+objectType);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

            String lines;
            while ((lines = in.readLine()) != null) {
                String[] words = lines.split(";");
                text.add(words[0] + " " + words[2]);
            }

            in.close();
            inputStream.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return text;
    }

    public ArrayList<LatLng> getLocationList(String objectType) {
        ArrayList<LatLng> latLngs = new ArrayList<>();

        try {
            //InputStream inputStream = getResources().openRawResource(resource);
            InputStream inputStream = getAssets().open(filial+objectType);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

            String lines;
            while ((lines = in.readLine()) != null) {
                String[] words = lines.split(";");
                if(objectType.equals(TP_OBJECT)) {
                    Location location = new Location("");
                    location.setLatitude(Double.valueOf(words[3]));
                    location.setLongitude(Double.valueOf(words[4]));
                    latLngs.add(new LatLng(words[0], words[2], location));
                } else if (objectType.equals(KL_OBJECT)){

                } else {
                    try {
                        String checkCoords = words[3].replace("/", "\\");
                        JsonReader reader = new JsonReader(new StringReader(checkCoords));
                        GsonBuilder gsonBuilder = new GsonBuilder();
                        Gson gson = gsonBuilder.create();
                        Mast[] points = gson.fromJson(reader, Mast[].class);
                        ArrayList<Mast> list = new ArrayList<>(Arrays.asList(points));
                        for (int i = 0; i < list.size(); i++) {
                            // Log.d("coords", list.get(i).getLat() + " " + list.get(i).getLng());
                            Location location = new Location("");
                            location.setLatitude(list.get(i).getLat());
                            location.setLongitude(list.get(i).getLng());
                            latLngs.add(new LatLng(words[0], words[2], list.get(i).getOpr(), location));
                        }
                        reader.close();
                    } catch (ArrayIndexOutOfBoundsException | JsonSyntaxException ex) {}
                }
            }
            in.close();
            inputStream.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return latLngs;
    }*/

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.line_or_tp_spinner:
                initListSpinner(position);
                break;
            case R.id.opr_or_span:
                if(parent.getItemAtPosition(position).toString().equals("Опора")) {
                    binding.inputOpr.setHint(getString(R.string.enter_opr_name));
                } else {
                    binding.inputOpr.setHint(getString(R.string.enter_span));
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.list_spinner:
                ObjectListDialogFragment fragment = ObjectListDialogFragment.newInstance(objects, "Виберіть об'єкт:");
                fragment.show(getSupportFragmentManager(), "DialogFragment");
                break;
            case R.id.location_btn:
                Toast.makeText(this, R.string.func_not_working, Toast.LENGTH_LONG).show();
                /*if(!objectType.equals(KL_OBJECT)) {
                    mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                    if (ActivityCompat.checkSelfPermission(
                            CreateListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            CreateListActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                    } else {
                        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            OnGPS();
                        } else {
                            binding.llProgressBar.getRoot().setVisibility(View.VISIBLE);
                            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                                    LOCATION_REFRESH_DISTANCE, mLocationListener);
                        }
                    }
                } else {
                    Toast.makeText(this, "Пока что для кабельных функционал отсутствует", Toast.LENGTH_SHORT).show();
                }*/
                break;
            case R.id.continue_btn:
                if (validate()) {
                    linename= binding.listSpinner.getText().toString().split(" ", 2)[1];
                    String plan;
                    if ( modificator.equals(Default.CONFIRM_MODIFICATOR)) plan = "";
                    else plan = binding.planSpinner.getSelectedItem().toString();
                    String tplnr = lineToTplnr.get(linename);//getTplnr(objectType, linename);
                    new BackgroundTask(CreateListActivity.this) {

                        @Override
                        public long doInBackground() {
                            ObjectsDatabase db = App.getInstance().getDatabase();
                            ObjectsDao objectsDao = db.objectsDao();
                            Objects object = objectsDao.getObject(linename, modificator);
                            long objId;
                            try {
                                objId = object.getId();
                            } catch (NullPointerException e){
                                objId = objectsDao.insert(new Objects(modificator, tabel, plan, linename, resource, tplnr, getString(R.string.not_uploaded)));
                            }
                            return objId;
                        }

                        @Override
                        public void onPostExecute(long objId) {
                            Intent intent = new Intent(CreateListActivity.this, DefectActivity.class);
                            int objIdI = (int) objId;
                            intent.putExtra("objId", objIdI);
                            intent.putExtra("mod", modificator);
                            intent.putExtra("tabel", tabel);
                            intent.putExtra("type" , resource);
                            intent.putExtra("oprOr", binding.oprOrSpan.getSelectedItemPosition());
                            intent.putExtra("opr", binding.inputOpr.getText().toString());
                            startActivity(intent);
                            finish();
                        }
                    }.execute();



                }
                break;
        }
    }

    /*private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(CreateListActivity.this);
        builder.setMessage("Включить GPS?").setCancelable(false)
                .setPositiveButton("Да", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Нет", (dialog, which) -> dialog.cancel());
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }*/

    /*private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            if (location != null) {
                ArrayList<LatLng> list = getLocationList(objectType);
                ArrayList<String> result = new ArrayList<>();
                if (objectType.equals(TP_OBJECT)) {
                    for (int i = 0; i < list.size(); i++) {
                        if (location.distanceTo(list.get(i).getLocation()) < 200) {
                            //Log.d("item", i + " " + list.get(i).getLocation().getLatitude() + " " + list.get(i).getLocation().getLongitude());
                            result.add(list.get(i).getInvent() + " " + list.get(i).getLinename());
                        }
                    }
                } else if(objectType.equals(KL_OBJECT)) {
                    Toast.makeText(CreateListActivity.this, "Пока эта функция не работает для КЛ и КВ", Toast.LENGTH_SHORT).show();
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        if (location.distanceTo(list.get(i).getLocation()) < 100) {
                            result.add(list.get(i).getInvent() + " " + list.get(i).getLinename() + "; Опора: " + list.get(i).getOpr());
                        }
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(CreateListActivity.this);
                builder.setTitle("Выберите объект по близости:");
                builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
                builder.setItems(result.toArray(new String[0]), (dialog, which) -> {
                    if (objectType.equals(TP_OBJECT)) {
                        binding.listSpinner.setSelection(line_adapter.getPosition(result.get(which)));
                        String parts[] = result.get(which).split(" ", 2);
                        linename = parts[1];
                    } else {
                        binding.listSpinner.setSelection(line_adapter.getPosition(result.get(which).split(";", 2)[0]));
                        String parts[] = result.get(which).split(" ");
                        linename = parts[1];
                        String opr[] = result.get(which).split("Опора: ");
                        binding.inputOpr.setText(opr[1]);
                        // Log.d("logtest", result.get(which).split(";", 2)[0] + " " + opr[1]);
                    }
                });
                builder.show();
                binding.llProgressBar.getRoot().setVisibility(View.GONE);

                mLocationManager.removeUpdates(this);
            } else {
                Toast.makeText(CreateListActivity.this, "Невозможно определить местоположение", Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {

        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {

        }
    };*/

    /*private String getTplnr(String objectType, String linename) {
        //InputStream inputStream = getResources().openRawResource(typeResource);
        InputStream inputStream = null;
        BufferedReader reader = null;
        String tplnr = null;
        try {
            inputStream= getAssets().open(filial+objectType);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String [] words = line.split(";");
                if (words[2].equals(linename)) {
                    tplnr = words[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return tplnr;
    }*/

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("tabel", tabel);
        startActivity(intent);
    }

    private boolean validate() {
        boolean valid = true;
        if (binding.listSpinner.getText().toString().contains("Виберіть")) {
            Toast.makeText(this, "Виберіть об'єкт", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        else if (binding.lineOrTpSpinner.getSelectedItem().toString().contains(getString(R.string.line))) {
            if (binding.oprOrSpan.getSelectedItem().toString().equals("Опора") && !(binding.inputOpr.getText().length() > 0)) {
                binding.inputOpr.setError(getString(R.string.enter_opr_name));
                valid = false;
            } else if(binding.oprOrSpan.getSelectedItem().toString().equals("Опора") && binding.inputOpr.getText().toString().contains("-") || binding.inputOpr.getText().toString().contains(",") || binding.inputOpr.getText().toString().contains(";")){
                binding.inputOpr.setError(getString(R.string.validate_hint1));
                valid = false;
            } else if (binding.oprOrSpan.getSelectedItem().toString().equals("Проліт") && !binding.inputOpr.getText().toString().contains("-")) {
                binding.inputOpr.setError(getString(R.string.validate_hint2));
                valid = false;
            } else if (binding.oprOrSpan.getSelectedItem().toString().equals("Проліт") && binding.inputOpr.getText().toString().contains("-") && binding.inputOpr.getText().toString().contains(",") || binding.inputOpr.getText().toString().contains("_") || binding.inputOpr.getText().toString().contains(":") || binding.inputOpr.getText().toString().contains(";")) {
                binding.inputOpr.setError(getString(R.string.validate_hint3));
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public void sendInput(String input) {
        binding.listSpinner.setText(input);
    }
}