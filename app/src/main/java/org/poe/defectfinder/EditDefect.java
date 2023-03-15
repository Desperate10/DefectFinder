package org.poe.defectfinder;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.databinding.ActivityEditDefectBinding;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.other.BackgroundTask;
import org.poe.defectfinder.other.Image;
import org.poe.defectfinder.other.ImageAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import static org.poe.defectfinder.DefectActivity.PERMISSIONS_STORAGE;

public class EditDefect extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private ActivityEditDefectBinding binding;
    private ArrayList<Image> items = new ArrayList<Image>();
    private ArrayList<String> uri = new ArrayList<>();

    Uri imageUri;
    ArrayAdapter<String> adapter;
    ImageAdapter imageAdapter;
    String currentOpr;
    int pos;
    private int type = 0;
    private int oprOrSpan;
    List<Defects> defects = new ArrayList<>();
    List<String> oprs = new ArrayList<>();
    ArrayList<String> previousDefects = new ArrayList<>();
    private int objId;
    private String modificator, tabel, oldDefect, oldNote,filial;
    ObjectsDatabase db;
    DefectsDao defectsDao;
    ObjectsDao objectsDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditDefectBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            objId = bundle.getInt("objId");
            modificator = bundle.getString("mod");
            type = bundle.getInt("type");
            tabel = bundle.getString("tabel");
            filial = bundle.getString("filial");
        }

        db = App.getInstance().getDatabase();
        defectsDao = db.defectsDao();
        objectsDao = db.objectsDao();
        readData();
        binding.oprSpinner.setOnItemSelectedListener(this);
        binding.defectSpinner.setOnItemSelectedListener(this);

        imageAdapter = new ImageAdapter(this, items, uri);
        binding.photoLayout.setAdapter(imageAdapter);

        binding.photoLayout.setOnItemClickListener((parent, v, position, id) -> {
            if (position == 0) {
                selectPhoto();
            }
        });

        binding.continueBtn.setOnClickListener(this);
        binding.saveDefect.setOnClickListener(this);
        if(!modificator.equals(Default.CONFIRM_MODIFICATOR)) {
            binding.addDefect.setOnClickListener(this);
            binding.deleteDefect.setOnClickListener(this);
        } else {
            binding.addDefect.setVisibility(View.GONE);
            binding.deleteDefect.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, WatchCreatedLists.class);
        intent.putExtra("mod", modificator);
        intent.putExtra("tabel", tabel);
        startActivity(intent);
        finish();
    }

    private void readData() {
        new BackgroundTask(EditDefect.this) {

            @Override
            public long doInBackground() {
                oprs = defectsDao.getOprList(objId);
                defects = defectsDao.getDefectByObjectOprName(objId, currentOpr);
                return 0;
            }

            @Override
            public void onPostExecute(long id) {
                initOprSpinner(oprs);
                initDefect(defects.size());
            }
        }.execute();
    }

    private void loadDefectTxt() {
        try {
            ArrayList<String> strBuild = getDefects(type, oprOrSpan);
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, strBuild);
            // Определяем разметку для использования при выборе элемента
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Применяем адаптер к элементу spinner
            //binding.searchableSpinner.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initOprSpinner(List<String> oporas) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, oporas);
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        binding.oprSpinner.setAdapter(adapter);
        binding.oprSpinner.setSelection(adapter.getPosition(currentOpr));
    }

    private void addAddButton(Uri uri) {
        Image i = new Image();
        i.setURI(uri);
        items.add(i);
        imageAdapter.notifyDataSetChanged();
    }

    private ArrayList<String> getDefects(int resource, int oprOrSpan) throws IOException {
        InputStream inputStream = getResources().openRawResource(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ArrayList<String> strBuild = new ArrayList<>();
        try {
            String line = reader.readLine();
            String next = reader.readLine();
            for (boolean first = true, last = (line == null); !last; first = false, line = next) {
                last = ((next = reader.readLine()) == null);
                String[] words = line.split(";");
                if (resource != R.raw.tp && resource != R.raw.kl) {
                    if (oprOrSpan == 0 && words[0].equals("Опори")) {
                        strBuild.add(words[1]);
                    } else if (oprOrSpan == 1 && words[0].equals("Прогону")) {
                        strBuild.add(words[1]);
                    }
                    if (last) {
                        strBuild.remove(words[1]);
                        break;
                    }
                } else {
                    strBuild.add(line);
                    if (last) {
                        strBuild.remove(line);
                        break;
                    }
                }
            }
        } finally {
            reader.close();
            inputStream.close();
        }

        return strBuild;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.opr_spinner:
                currentOpr = binding.oprSpinner.getSelectedItem().toString();
                defects = defectsDao.getDefectByObjectOprName(objId, currentOpr);
                oprOrSpan = defects.get(0).getOprOrSpan();
                loadDefectTxt();
                int size = defects.size();
                initDefect(size);
                break;
            case R.id.defect_spinner:
                loadDefectTxt();
                pos = position;
                items.clear();
                uri.clear();
                String defectT = defects.get(position).getDefectName();
                //Log.d("defectT", defectT);
                oldDefect = defectT;
                Defects defect = defectsDao.getCurtainDefect(objId, binding.oprSpinner.getSelectedItem().toString(), defectT);
                oldNote = defect.getNote();
                binding.adds.setText(oldNote);
                List<String> photos = defect.getPhotos();
                //binding.searchableSpinner.setSelection(adapter.getPosition(defectT));
                if(modificator.equals(Default.CONFIRM_MODIFICATOR))
                //binding.searchableSpinner.setEnabled(false);
                //Uri selectedImage = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.ic_add_photo);
               // addAddButton(selectedImage);
                for (int i = 0; i< photos.size(); i++) {
                    uri.add(photos.get(i));
                    addAddButton(Uri.parse(photos.get(i)));
                }
                break;
        }
    }

    private void saveData() {
        //String defectTxt = binding.searchableSpinner.getSelectedItem().toString();
        /*if(defectTxt.contains("$") && uri.isEmpty()) {
            Toast.makeText(this, "Что бы сохранить этот дефект нужно добавить фото", Toast.LENGTH_SHORT).show();
            return;
        }*/

        new BackgroundTask(EditDefect.this) {
            @Override
            public long doInBackground() {
                Date date = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String strDate = dateFormat.format(date);
                String note = binding.adds.getText().toString();

                objectsDao.updateDate(objId, strDate);
                if(modificator.equals(Default.ADD_MODIFICATOR) || modificator.equals(Default.EDIT_MODIFICATOR) ) {
                    objectsDao.updateModificator(objId, Default.EDIT_MODIFICATOR);
                }
                defectsDao.deleteCurtainDefect(objId, binding.oprSpinner.getSelectedItem().toString(), oldDefect);
                //defectsDao.insert(new Defects(objId, binding.oprSpinner.getSelectedItem().toString(), oprOrSpan, defectTxt, uri, note, Default.EDIT_MODIFICATOR));
                return 0;
            }

            @Override
            public void onPostExecute(long id) {
                Toast.makeText(EditDefect.this, getString(R.string.defect_saved), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void deleteDefect() {
        new BackgroundTask(EditDefect.this) {

            @Override
            public long doInBackground() {
                for (int i = 0; i < uri.size(); i++) {
                    String photo = uri.get(i);
                    Uri uri = Uri.parse(photo);
                    try {
                        getContentResolver().delete(uri, null, null);
                    } catch (SecurityException e) {}
                }
                defectsDao.deleteCurtainDefect(objId, binding.oprSpinner.getSelectedItem().toString(), oldDefect);
                return 0;
            }

            @Override
            public void onPostExecute(long id) {
                Toast.makeText(EditDefect.this, getString(R.string.defect_deleted), Toast.LENGTH_SHORT).show();
            }
        }.execute();


    }

    private void initDefect(int defectsSize) {
        ArrayList<String> strBuild = new ArrayList<>();
        for (int i = 1; i <= defectsSize; i++) {
            strBuild.add("Дефект " + i);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, strBuild);
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        binding.defectSpinner.setAdapter(adapter);
    }

    private void selectPhoto() {
        final CharSequence[] options = { getString(R.string.do_photo), getString(R.string.from_galery), getString(R.string.cancel) };
        AlertDialog.Builder builder = new AlertDialog.Builder(EditDefect.this);
        builder.setTitle(getString(R.string.add_photo));
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals(getString(R.string.do_photo)))
            {
                String filename = filial;
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    File tempImage = File.createTempFile(filename, ".jpg", storageDirectory);
                    imageUri = FileProvider.getUriForFile(EditDefect.this, "org.poe.defectfinder.fileprovider", tempImage);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                    else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
                        ClipData clip=
                                ClipData.newUri(getContentResolver(), "A photo", imageUri);

                        intent.setClipData(clip);
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                    else {
                        List<ResolveInfo> resInfoList=
                                getPackageManager()
                                        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

                        for (ResolveInfo resolveInfo : resInfoList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            grantUriPermission(packageName, imageUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        }
                    }

                    try {
                        startActivityForResult(intent, 1);
                    }
                    catch (ActivityNotFoundException e) {
                        Toast.makeText(this, R.string.msg_no_camera, Toast.LENGTH_LONG).show();
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (options[item].equals(getString(R.string.from_galery)))
            {
                Intent intent = new   Intent(Intent.ACTION_OPEN_DOCUMENT,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setFlags(intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, 2);
            }
            else if (options[item].equals(getString(R.string.cancel))) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {

                Image i = new Image();
                i.setURI(imageUri);
                items.add(i);
                uri.add(imageUri.toString());
                imageAdapter.notifyDataSetChanged();

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(imageUri);

                this.sendBroadcast(mediaScanIntent);
            } else if (requestCode == 2) {
                int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    ActivityCompat.requestPermissions(
                            this,
                            PERMISSIONS_STORAGE,
                            1
                    );
                }
                Uri selectedImage;
                try {
                    String copiedPath = DefectActivity.getRealPathFromURI_API19(this, data.getData());
                    // Log.d("absolute", copiedPath);
                    File file = new File(copiedPath);
                    selectedImage = FileProvider.getUriForFile(EditDefect.this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
                   // Log.d("absolute", selectedImage.toString());
                } catch (Exception ex) {
                    selectedImage = data.getData();
                   // Log.d("absolute1", selectedImage.toString());
                }
              //  Log.d("absolute2", selectedImage.toString());
                Image i = new Image();
                i.setURI(selectedImage);
                uri.add(selectedImage.toString());
                items.add(i);
                imageAdapter.notifyDataSetChanged();
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.continue_btn:
                intent = new Intent(EditDefect.this, WatchCreatedLists.class);
                intent.putExtra("tabel", tabel);
                intent.putExtra("mod", modificator);
                startActivity(intent);
                finish();
                break;
            case R.id.add_defect:
                new BackgroundTask(EditDefect.this) {

                    @Override
                    public long doInBackground() {
                        defects = defectsDao.getDefectByObjectOprName(objId, binding.oprSpinner.getSelectedItem().toString());
                        return 0;
                    }

                    @Override
                    public void onPostExecute(long id) {
                        Intent intent;
                        intent = new Intent(EditDefect.this, DefectActivity.class);
                        intent.putExtra("objId", objId);
                        intent.putExtra("mod", modificator);
                        intent.putExtra("opr", binding.oprSpinner.getSelectedItem().toString());
                        intent.putExtra("type", type);
                        intent.putExtra("oprOr", oprOrSpan);
                        intent.putExtra("tabel", tabel);
                        intent.putExtra("defect", true);
                        intent.putExtra("edit", true);
                        intent.putExtra("isAddOpr", true);

                        for (int i = 0; i < defects.size(); i++) {
                            previousDefects.add(defects.get(i).getDefectName());
                        }
                        intent.putExtra("previous_defect", previousDefects);
                        startActivity(intent);
                        finish();
                    }
                }.execute();

                break;
            case R.id.delete_defect:
                if (defects.size() > 1) {
                    deleteDefect();
                    readData();
                } else {
                    Toast.makeText(this, getString(R.string.cant_delete_last_defect), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.save_defect:
                boolean valid = true;
                /*if(binding.defectSpinner.getSelectedItem().toString().contains("$") && uri.isEmpty()) {
                    Toast.makeText(this, "Этот дефект нельзя оставлять без фото", Toast.LENGTH_SHORT).show();
                    valid = false;
                }*/
                for (int i = 0; i< defects.size(); i++) {
                    //if(defects.get(i).getDefectName().equals(binding.searchableSpinner.getSelectedItem().toString()) && defects.get(i).getPhotos().size() == items.size() -1 && (oldNote !=null &&oldNote.equals(binding.adds.getText().toString()))) {
                    //    Toast.makeText(this, getString(R.string.defect_exist_on_opr), Toast.LENGTH_SHORT).show();
                    //    valid = false;
                    //    break;
                    //}
                }
                if (valid) {
                    saveData();
                }
                break;
        }
    }
}