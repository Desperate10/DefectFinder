package org.poe.defectfinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.databinding.ActivityAddOprToLineBinding;
import org.poe.defectfinder.other.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class AddOprToLine extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener{

    ActivityAddOprToLineBinding binding;
    //LocationManager mLocationManager;
    private static final int REQUEST_LOCATION = 1;
    private static final long LOCATION_REFRESH_TIME = 5000;
    private static final float LOCATION_REFRESH_DISTANCE = 50;

    private String lineName, modificator, tabel;
    private int type, objId;

    AssetManager assetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddOprToLineBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        ActivityCompat.requestPermissions( this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            objId = bundle.getInt("objId");
            modificator = bundle.getString("mod");
            lineName = bundle.getString("linename");
            type = bundle.getInt("type");
            tabel = bundle.getString("tabel");
        }

        assetManager = getAssets();

        binding.continueBtn.setOnClickListener(this);
        binding.locationBtn.setOnClickListener(this);
        binding.oprOrSpan.setOnItemSelectedListener(this);
    }

    /*@Override
    protected void onStop() {
        super.onStop();
        try{
            mLocationManager.removeUpdates(mLocationListener);
        } catch (NullPointerException e) {}
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            if (location != null) {
                ArrayList<LatLng> list = getLocationList(lineName);
                ArrayList<String> result = new ArrayList<>();
                //Log.d("zashlo", "zaslo");
                for (int i = 0; i < list.size(); i++) {
                    // Log.d("location", list.get(i).getLocation().toString());
                    if (location.distanceTo(list.get(i).getLocation()) < 100) {
                        //result.add(list.get(i).getInvent() + " " + list.get(i).getLinename() + "; Опора: " + list.get(i).getOpr());
                        result.add(list.get(i).getOpr());
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(AddOprToLine.this);
                builder.setTitle(getString(R.string.choose_near_object));
                builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
                builder.setItems(result.toArray(new String[0]), (dialog, which) -> {
                    String opr = result.get(which);
                    binding.inputOpr.setText(opr);
                });
                builder.show();
                binding.llProgressBar.getRoot().setVisibility(View.GONE);

                mLocationManager.removeUpdates(this);
            } else {
                Toast.makeText(AddOprToLine.this, getString(R.string.cant_find_geo), Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @Override
        public void onProviderDisabled(@NonNull String provider) { }
    };*/

    public ArrayList<LatLng> getLocationList(String lineName) {
        ArrayList<LatLng> latLngs = new ArrayList<>();

        try {
            //InputStream inputStream = getResources().openRawResource(R.raw.linename0_4);
            InputStream inputStream = assetManager.open("lines0_4.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

            String lines;
            while ((lines = in.readLine()) != null) {
                String[] words = lines.split(";");
                if (lineName.equals(words[2])) {
                    String checkCoords = words[3].replace("/", "\\");
                    JsonReader reader = new JsonReader(new StringReader(checkCoords));
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();
                    Mast[] points = gson.fromJson(reader, Mast[].class);
                    ArrayList<Mast> list = new ArrayList<>(Arrays.asList(points));
                    for (int i = 0; i < list.size(); i++) {
                        // Log.d("coords", list.get(i).getLat()+" "+ list.get(i).getLng());
                        Location location = new Location("");
                        location.setLatitude(list.get(i).getLat());
                        location.setLongitude(list.get(i).getLng());
                        latLngs.add(new LatLng(words[0], words[2], list.get(i).getOpr(), location));
                    }
                    reader.close();
                }
            }
            in.close();
            inputStream.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return latLngs;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.location_btn:
                Toast.makeText(this, R.string.func_not_working, Toast.LENGTH_LONG).show();
                /*
                binding.oprOrSpan.setSelection(0);

                mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                if (ActivityCompat.checkSelfPermission(
                        AddOprToLine.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        AddOprToLine.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                break;*/
            case R.id.continue_btn:
                if (validate()) {
                    ObjectsDatabase database = App.getInstance().getDatabase();
                    ObjectsDao objectsDao = database.objectsDao();
                    // Only flip ADD/EDIT rows. Flipping CONFIRM rows here was the bug
                    // that emptied the obhod ("Плановий") field at CSV export time:
                    // CONFIRM rows are inserted with obhod="" intentionally, but a
                    // post-flip non-CONFIRM CSV branch reads obhod and emits "".
                    if (Default.ADD_MODIFICATOR.equals(modificator)
                            || Default.EDIT_MODIFICATOR.equals(modificator)) {
                        objectsDao.updateModificator(objId, Default.EDIT_MODIFICATOR);
                    }
                    Intent intent = new Intent(AddOprToLine.this, DefectActivity.class);
                    intent.putExtra("objId", objId);
                    intent.putExtra("isAddOpr", true);
                    intent.putExtra("mod", modificator);
                    intent.putExtra("type" , type);
                    intent.putExtra("oprOr", binding.oprOrSpan.getSelectedItemPosition());
                    intent.putExtra("opr", binding.inputOpr.getText().toString());
                    intent.putExtra("tabel", tabel);
                    startActivity(intent);
                    finish();
                    //startActivity(new Intent(CreateListActivity.this, DefectActivity.class), bundle);
                }
                break;
        }
    }

    /*private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.turn_on_gps)).setCancelable(false)
                .setPositiveButton(getString(R.string.yes), (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.cancel());
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }*/

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.opr_or_span:
                if(parent.getItemAtPosition(position).toString().equals(getString(R.string.opr))) {
                    binding.inputOpr.setHint(getString(R.string.opr_name));
                } else {
                    binding.inputOpr.setHint(R.string.enter_span);
                }
                break;
        }
    }

    private boolean validate() {
        boolean valid = true;
        ObjectsDatabase db = App.getInstance().getDatabase();
        DefectsDao defectsDao = db.defectsDao();
        List<String> oprs = defectsDao.getOprList(objId);
        for(int i = 0 ; i < oprs.size(); i++) {
            if (oprs.get(i).equals(binding.inputOpr.getText().toString().trim())) {
                Toast.makeText(this, getString(R.string.opr_already_created), Toast.LENGTH_SHORT).show();
                valid = false;
                break;
            }
        }
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
        return valid;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}