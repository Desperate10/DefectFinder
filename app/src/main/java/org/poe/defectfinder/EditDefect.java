package org.poe.defectfinder;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.databinding.ActivityEditDefectBinding;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.entities.Objects;
import org.poe.defectfinder.other.BackgroundTask;
import org.poe.defectfinder.other.Image;
import org.poe.defectfinder.other.ImageAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EditDefect extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener, ObjectListDialogFragment.OnInputListener {
    private ActivityEditDefectBinding binding;
    private ArrayList<Image> items = new ArrayList<Image>();
    private ArrayList<String> uri = new ArrayList<>();

    Uri imageUri;
    ImageAdapter imageAdapter;
    String currentOpr;
    int pos;
    private int type = 0;
    private int oprOrSpan;
    List<Defects> defects = new ArrayList<>();
    List<String> oprs = new ArrayList<>();
    ArrayList<String> previousDefects = new ArrayList<>();
    private int objId;
    private String modificator, tabel, oldDefect, oldNote, filial;
    private boolean showDefectGroup = false;
    ObjectsDatabase db;
    DefectsDao defectsDao;
    ObjectsDao objectsDao;

    private File tempImage = null;
    ArrayList<String> strBuild = new ArrayList<>();

    private Uri latestTmpUri;
    private ActivityResultLauncher<Uri> takePhotoResult = registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
        if (isSuccess) {
            Image i = new Image();
            i.setURI(latestTmpUri);
            items.add(i);
            uri.add(latestTmpUri.toString());
            imageAdapter.notifyDataSetChanged();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(latestTmpUri);
            this.sendBroadcast(mediaScanIntent);
        }
    });

    private ActivityResultLauncher<PickVisualMediaRequest> pickMultiplePhoto = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(),
            result -> {
                for (int i = 0; i < result.size(); i++) {
                    Uri localUri = copyToLocalStorage(result.get(i));
                    if (localUri != null) {
                        Image image = new Image();
                        image.setURI(localUri);
                        uri.add(localUri.toString());
                        items.add(image);
                    }
                }
                imageAdapter.notifyDataSetChanged();
            });

    private void photoPicker() {
        pickMultiplePhoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

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

        String resourceName = getResources().getResourceEntryName(type);
        showDefectGroup = resourceName.equals("ps") || resourceName.equals("line35");

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
        setupThermalCheckboxes();
        if (!modificator.equals(Default.CONFIRM_MODIFICATOR)) {
            binding.addDefect.setOnClickListener(this);
            binding.deleteDefect.setOnClickListener(this);
        } else {
            binding.addDefect.setVisibility(View.GONE);
            binding.deleteDefect.setVisibility(View.GONE);
        }

        /*try {
            strBuild = getDefects(type, oprOrSpan);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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

    private boolean muteThermalListeners = false;
    private boolean[] thermalSnapshot = new boolean[4];

    private void snapshotThermal() {
        thermalSnapshot[0] = binding.cbThermalDone.isChecked();
        thermalSnapshot[1] = binding.cbThermalNotDone.isChecked();
        thermalSnapshot[2] = binding.cbDefectsFound.isChecked();
        thermalSnapshot[3] = binding.cbDefectsAbsent.isChecked();
    }

    private void restoreThermal() {
        muteThermalListeners = true;
        binding.cbThermalDone.setChecked(thermalSnapshot[0]);
        binding.cbThermalNotDone.setChecked(thermalSnapshot[1]);
        binding.cbDefectsFound.setChecked(thermalSnapshot[2]);
        binding.cbDefectsAbsent.setChecked(thermalSnapshot[3]);
        binding.defectsBox.setVisibility(thermalSnapshot[0] ? View.VISIBLE : View.GONE);
        muteThermalListeners = false;
    }

    private void persistThermalState() {
        objectsDao.updateThermalState(objId,
                binding.cbThermalDone.isChecked(),
                binding.cbThermalNotDone.isChecked(),
                binding.cbDefectsFound.isChecked(),
                binding.cbDefectsAbsent.isChecked());
    }

    private boolean listHasH31() {
        for (Defects d : defectsDao.getDefectsByObject(objId)) {
            if (d.getDefectName() != null && d.getDefectName().startsWith("Н31")) {
                return true;
            }
        }
        return false;
    }

    private boolean listHasNonH31() {
        for (Defects d : defectsDao.getDefectsByObject(objId)) {
            if (d.getDefectName() != null && !d.getDefectName().startsWith("Н31")) {
                return true;
            }
        }
        return false;
    }

    private void deleteAllH31() {
        for (Defects d : defectsDao.getDefectsByObject(objId)) {
            if (d.getDefectName() != null && d.getDefectName().startsWith("Н31")) {
                defectsDao.deleteCurtainDefect(objId, d.getOprName(), d.getDefectName());
            }
        }
    }

    private void onThermalChanged() {
        boolean newValid = binding.cbThermalDone.isChecked() && binding.cbDefectsFound.isChecked();
        boolean wasValid = thermalSnapshot[0] && thermalSnapshot[2];
        if (wasValid && !newValid && listHasH31()) {
            if (!listHasNonH31()) {
                Toast.makeText(this, getString(R.string.cant_delete_last_defect), Toast.LENGTH_LONG).show();
                restoreThermal();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.h31_remove_title))
                    .setMessage(getString(R.string.h31_remove_message))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes), (d, w) -> {
                        deleteAllH31();
                        snapshotThermal();
                        persistThermalState();
                        readData();
                    })
                    .setNegativeButton(getString(R.string.no), (d, w) -> restoreThermal())
                    .show();
        } else {
            snapshotThermal();
            persistThermalState();
        }
    }

    private void setupThermalCheckboxes() {
        if (type != R.raw.tp) {
            binding.thermalBox.setVisibility(View.GONE);
            return;
        }
        Objects current = objectsDao.getObjectById(objId);
        if (current != null) {
            binding.cbThermalDone.setChecked(current.isThermalDoneOk());
            binding.cbThermalNotDone.setChecked(current.isThermalNotDone());
            binding.cbDefectsFound.setChecked(current.isDefectsFound());
            binding.cbDefectsAbsent.setChecked(current.isDefectsAbsent());
            binding.defectsBox.setVisibility(current.isThermalDoneOk() ? View.VISIBLE : View.GONE);
        }
        snapshotThermal();
        binding.cbThermalDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (muteThermalListeners) return;
            muteThermalListeners = true;
            if (isChecked && binding.cbThermalNotDone.isChecked()) {
                binding.cbThermalNotDone.setChecked(false);
            }
            if (!isChecked) {
                binding.defectsBox.setVisibility(View.GONE);
                binding.cbDefectsFound.setChecked(false);
                binding.cbDefectsAbsent.setChecked(false);
            } else {
                binding.defectsBox.setVisibility(View.VISIBLE);
            }
            muteThermalListeners = false;
            onThermalChanged();
        });
        binding.cbThermalNotDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (muteThermalListeners) return;
            muteThermalListeners = true;
            if (isChecked && binding.cbThermalDone.isChecked()) {
                binding.cbThermalDone.setChecked(false);
                binding.defectsBox.setVisibility(View.GONE);
                binding.cbDefectsFound.setChecked(false);
                binding.cbDefectsAbsent.setChecked(false);
            }
            muteThermalListeners = false;
            onThermalChanged();
        });
        binding.cbDefectsFound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (muteThermalListeners) return;
            muteThermalListeners = true;
            if (isChecked && binding.cbDefectsAbsent.isChecked()) {
                binding.cbDefectsAbsent.setChecked(false);
            }
            muteThermalListeners = false;
            onThermalChanged();
        });
        binding.cbDefectsAbsent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (muteThermalListeners) return;
            muteThermalListeners = true;
            if (isChecked && binding.cbDefectsFound.isChecked()) {
                binding.cbDefectsFound.setChecked(false);
            }
            muteThermalListeners = false;
            onThermalChanged();
        });
    }

    private boolean otherH31Exists(String excludeDefectName) {
        for (Defects d : defectsDao.getDefectsByObject(objId)) {
            String name = d.getDefectName();
            if (name != null && name.startsWith("Н31") && !name.equals(excludeDefectName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isH31Allowed() {
        return binding.cbThermalDone.isChecked() && binding.cbDefectsFound.isChecked();
    }

    private ArrayList<String> filterH31(ArrayList<String> source) {
        if (source == null) return new ArrayList<>();
        if (isH31Allowed()) return source;
        ArrayList<String> out = new ArrayList<>();
        for (String s : source) {
            if (s != null && !s.startsWith("Н31")) out.add(s);
        }
        return out;
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
            strBuild = getDefects(type, oprOrSpan);
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
        boolean isNewObjectWithGroups = (resource == R.raw.ps || resource == R.raw.line35);
        try {
            String line = reader.readLine();
            String next = reader.readLine();
            for (boolean first = true, last = (line == null); !last; first = false, line = next) {
                last = ((next = reader.readLine()) == null);
                if (isNewObjectWithGroups) {
                    String[] parts = line.split(";");
                    if (parts.length >= 3) {
                        strBuild.add(parts[2].trim());
                    }
                    if (last && !strBuild.isEmpty()) {
                        strBuild.remove(strBuild.size() - 1);
                        break;
                    }
                } else {
                    String[] words = line.split(";");
                    if (resource != R.raw.tp && resource != R.raw.kl) {
                        if (oprOrSpan == 0 && words[0].equals("Опори")) {
                            strBuild.add(words[1]);
                        } else if (oprOrSpan == 1 && words[0].equals("Прогону")) {
                            strBuild.add(words[1]);
                        }
                        if (last) {
                            if (words.length >= 2) strBuild.remove(words[1]);
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
                binding.searchableSpinner.setText(defect.getDefectName());
                if (modificator.equals(Default.CONFIRM_MODIFICATOR))
                    binding.searchableSpinner.setEnabled(false);
                Uri selectedImage = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.ic_add_photo);
                addAddButton(selectedImage);
                for (int i = 0; i < photos.size(); i++) {
                    uri.add(photos.get(i));
                    addAddButton(Uri.parse(photos.get(i)));
                }
                break;
        }
    }

    private void saveData() {
        String defectTxt = binding.searchableSpinner.getText().toString();
        /*if(defectTxt.contains("$") && uri.isEmpty()) {
            Toast.makeText(this, "Что бы сохранить этот дефект нужно добавить фото", Toast.LENGTH_SHORT).show();
            return;
        }*/
        if (defectTxt.contains("Виберіть")) {
            Toast.makeText(this, getString(R.string.not_choose_defect), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean thermalDone = binding.cbThermalDone.isChecked();
        boolean thermalNotDone = binding.cbThermalNotDone.isChecked();
        boolean defectsFound = binding.cbDefectsFound.isChecked();
        boolean defectsAbsent = binding.cbDefectsAbsent.isChecked();
        new BackgroundTask(EditDefect.this) {
            @Override
            public long doInBackground() {
                Date date = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String strDate = dateFormat.format(date);
                String note = binding.adds.getText().toString().replaceAll("\n", ", ").replaceAll(";", ", ");

                objectsDao.updateDate(objId, strDate);
                objectsDao.updateThermalState(objId, thermalDone, thermalNotDone, defectsFound, defectsAbsent);
                if (modificator.equals(Default.ADD_MODIFICATOR) || modificator.equals(Default.EDIT_MODIFICATOR)) {
                    objectsDao.updateModificator(objId, Default.EDIT_MODIFICATOR);
                }
                defectsDao.deleteCurtainDefect(objId, binding.oprSpinner.getSelectedItem().toString(), oldDefect);
                defectsDao.insert(new Defects(objId, binding.oprSpinner.getSelectedItem().toString(), oprOrSpan, defectTxt, uri, note, Default.EDIT_MODIFICATOR));
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
                    } catch (SecurityException e) {
                    }
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

    private void takePhoto() {
        Uri uri = getTmpFileUri();
        latestTmpUri = uri;
        takePhotoResult.launch(uri);
    }

    private Uri getTmpFileUri() {
        String filename = filial;
        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            tempImage = File.createTempFile(filename, ".jpg", storageDirectory);
            tempImage.deleteOnExit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUri = FileProvider.getUriForFile(
                EditDefect.this,
                "org.poe.defectfinder.fileprovider",
                tempImage);
    }

    private Uri copyToLocalStorage(Uri sourceUri) {
        try {
            File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File destFile = File.createTempFile(filial, ".jpg", storageDirectory);
            InputStream in = getContentResolver().openInputStream(sourceUri);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            return FileProvider.getUriForFile(this, "org.poe.defectfinder.fileprovider", destFile);
        } catch (Exception e) {
            Log.e("EditDefect", "Failed to copy photo", e);
            return null;
        }
    }

    private void selectPhoto() {
        final CharSequence[] options = {getString(R.string.do_photo), getString(R.string.from_galery), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(EditDefect.this);
        builder.setTitle(getString(R.string.add_photo));
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals(getString(R.string.do_photo))) {
                takePhoto();
            } else if (options[item].equals(getString(R.string.from_galery))) {
                photoPicker();
            } else if (options[item].equals(getString(R.string.cancel))) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.searchableSpinner:
                ObjectListDialogFragment fragment = ObjectListDialogFragment.newInstance(filterH31(strBuild), "Виберіть дефект:");
                fragment.show(getSupportFragmentManager(), "DialogFragment");
                break;
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
                    if (type == R.raw.tp
                            && binding.cbThermalDone.isChecked()
                            && binding.cbDefectsFound.isChecked()
                            && oldDefect != null && oldDefect.startsWith("Н31")
                            && !otherH31Exists(oldDefect)) {
                        Toast.makeText(this, getString(R.string.h31_required_delete), Toast.LENGTH_LONG).show();
                        break;
                    }
                    deleteDefect();
                    readData();
                } else {
                    Toast.makeText(this, getString(R.string.cant_delete_last_defect), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.save_defect:
                boolean valid = true;
                String newDefectTxt = binding.searchableSpinner.getText().toString();
                if(newDefectTxt.contains("$") && showDefectGroup && uri.isEmpty()) {
                    Toast.makeText(this, "Цей дефект не можна залишати без фото", Toast.LENGTH_SHORT).show();
                    valid = false;
                }
                if (valid && newDefectTxt.startsWith("Н31")) {
                    if (uri.isEmpty()) {
                        Toast.makeText(this, getString(R.string.h31_photo_required), Toast.LENGTH_LONG).show();
                        valid = false;
                    } else if (binding.adds.getText().toString().trim().isEmpty()) {
                        Toast.makeText(this, getString(R.string.h31_note_required), Toast.LENGTH_LONG).show();
                        valid = false;
                    }
                }
                for (int i = 0; i < defects.size(); i++) {
                    if (defects.get(i).getDefectName().equals(newDefectTxt) && defects.get(i).getPhotos().size() == items.size() - 1 && (oldNote != null && oldNote.equals(binding.adds.getText().toString()))) {
                        Toast.makeText(this, getString(R.string.defect_exist_on_opr), Toast.LENGTH_SHORT).show();
                        valid = false;
                        break;
                    }
                }
                if (valid
                        && type == R.raw.tp
                        && binding.cbThermalDone.isChecked()
                        && binding.cbDefectsFound.isChecked()
                        && !newDefectTxt.startsWith("Н31")
                        && !otherH31Exists(oldDefect)) {
                    Toast.makeText(this, getString(R.string.h31_required), Toast.LENGTH_LONG).show();
                    valid = false;
                }
                if (valid) {
                    saveData();
                }
                break;
        }
    }

    @Override
    public void sendInput(String input, int requestCode) {
        binding.searchableSpinner.setText(input);
    }
}