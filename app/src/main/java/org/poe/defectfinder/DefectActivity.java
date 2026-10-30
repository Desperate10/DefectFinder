package org.poe.defectfinder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.databinding.ActivityDefectBinding;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.entities.Objects;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DefectActivity extends AppCompatActivity implements View.OnClickListener, ObjectListDialogFragment.OnInputListener {

    ActivityDefectBinding binding;
    private ArrayList<Image> items = new ArrayList<Image>();
    private ArrayList<String> uri = new ArrayList<>();
    ImageAdapter imageAdapter;
    Uri imageUri;
    String defectTxt, noteTxt;
    ArrayList<String> previousDefects;
    boolean isDefectAdd = false;
    private int type, oprOrSpan;
    private String opr, modificator, tabel, filial;
    private int objId;
    private boolean isAddOpr, edit;
    private boolean repeat = false;
    private File tempImage = null;
    private ArrayList<String> groupDefects = new ArrayList<>();
    private ArrayList<String> subgroupDefects = new ArrayList<>();
    private ArrayList<String> defects = new ArrayList<>();
    private boolean showDefectGroup = false;
    private boolean showDefectSubgroup = false;
    private String selectedGroup = null;
    private String selectedSubgroup = null;
    private String selectedDefect = null;

    private Uri latestTmpUri;
    private List<Uri> selectedPhotos = new ArrayList();
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
                selectedPhotos = result;
                for (int i = 0; i < selectedPhotos.size(); i++) {
                    Uri localUri = copyToLocalStorage(selectedPhotos.get(i));
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
        binding = ActivityDefectBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        previousDefects = new ArrayList<>();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            isDefectAdd = bundle.getBoolean("defect");
            if (isDefectAdd) {
                previousDefects = bundle.getStringArrayList("previous_defect");
            }
            objId = bundle.getInt("objId");
            edit = bundle.getBoolean("edit");
            modificator = bundle.getString("mod");
            isAddOpr = bundle.getBoolean("isAddOpr");
            tabel = bundle.getString("tabel");
            type = bundle.getInt("type");
            opr = bundle.getString("opr");
            oprOrSpan = bundle.getInt("oprOr");
        }

        // binding.activityName.setText(getResources().getString(R.string.add_object_to_line) +" к "+ lineName);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    100);
        }

        String resourceName = getResources().getResourceEntryName(type);
        showDefectGroup = resourceName.equals("ps") || resourceName.equals("line35");

        binding.defectGroup.setVisibility(showDefectGroup ? View.VISIBLE : View.GONE);
        binding.defectSubgroup.setVisibility(View.GONE);

        if (showDefectGroup) {
            setupGroupSpinner();
            setupSubgroupSpinner();
        }

        setupSearchableSpinner();
        setupThermalCheckboxes();

        /*ArrayList<String> strBuild = null;
        try {
            strBuild = getDefects(type, oprOrSpan);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, strBuild);
            // Определяем разметку для использования при выборе элемента
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Применяем адаптер к элементу spinner
            //binding.searchableSpinner.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        imageAdapter = new ImageAdapter(this, items, uri);
        binding.photoLayout.setAdapter(imageAdapter);
        addAddButton();

        binding.photoLayout.setOnItemClickListener((parent, v, position, id) -> {
            if (position == 0) {
                selectPhoto();
            }
        });

        //binding.searchableSpinner.setTitle(getString(R.string.find_defect));
        //binding.searchableSpinner.setPositiveButton(getString(R.string.close_tab));
        binding.continueBtn.setOnClickListener(this);

        SharedPreferences sharedPreferences = getSharedPreferences("filial", MODE_PRIVATE);
        filial = sharedPreferences.getString("filial", "00");
    }

    private void setupGroupSpinner() {
        binding.defectGroup.setOnClickListener(this);

        try {
            groupDefects = getGroupDefects(type);
        } catch (IOException e) {
            Log.e("DefectActivity", "Error loading defect groups", e);
            Toast.makeText(this, "Помилка завантаження груп дефектів", Toast.LENGTH_SHORT).show();
            binding.defectGroup.setVisibility(View.GONE);
        }
    }

    private void setupSubgroupSpinner() {
        binding.defectSubgroup.setOnClickListener(this);
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
        App.getInstance().getDatabase().objectsDao().updateThermalState(objId,
                binding.cbThermalDone.isChecked(),
                binding.cbThermalNotDone.isChecked(),
                binding.cbDefectsFound.isChecked(),
                binding.cbDefectsAbsent.isChecked());
    }

    private boolean listHasH31() {
        for (Defects d : App.getInstance().getDatabase().defectsDao().getDefectsByObject(objId)) {
            if (d.getDefectName() != null && d.getDefectName().startsWith("Н31")) {
                return true;
            }
        }
        return false;
    }

    private boolean listHasNonH31() {
        for (Defects d : App.getInstance().getDatabase().defectsDao().getDefectsByObject(objId)) {
            if (d.getDefectName() != null && !d.getDefectName().startsWith("Н31")) {
                return true;
            }
        }
        return false;
    }

    private void deleteAllH31() {
        DefectsDao dao = App.getInstance().getDatabase().defectsDao();
        for (Defects d : dao.getDefectsByObject(objId)) {
            if (d.getDefectName() != null && d.getDefectName().startsWith("Н31")) {
                dao.deleteCurtainDefect(objId, d.getOprName(), d.getDefectName());
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
                        if (previousDefects != null) {
                            previousDefects.removeIf(s -> s != null && s.startsWith("Н31"));
                        }
                        snapshotThermal();
                        persistThermalState();
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
        Objects current = App.getInstance().getDatabase().objectsDao().getObjectById(objId);
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

    private void setupSearchableSpinner() {
        binding.searchableSpinner.setOnClickListener(this);

        // Якщо це нові об'єкти з групами і група потрібна - не завантажуємо дефекти
        // Вони завантажаться після вибору групи
        if (showDefectGroup) {
            defects = null;
            return;
        }

        // Завантажуємо дефекти в залежності від типу об'єкта
        try {
            if (type == R.raw.tp || type == R.raw.kl) {
                // Об'єкти без груп
                defects = getDefects(type, -1, null, null);
            } else if (type == R.raw.ps || type == R.raw.line35) {
                // Нові об'єкти - всі дефекти (без фільтрації по групі)
                defects = getDefects(type, null);
            } else {
                // Старі об'єкти з Опори/Прогони
                //int oprOrSpan = getIntent().getIntExtra("OPR_OR_SPAN", 0);
                defects = getDefects(type, oprOrSpan);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Помилка завантаження дефектів", Toast.LENGTH_SHORT).show();
            defects = new ArrayList<>();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        for (int i = 1; i < items.size(); i++) {
            try {
                getContentResolver().delete(items.get(i).getURI(), null, null);
            } catch (SecurityException e) {
                Toast.makeText(this, "Фото зроблені не з додатку потрібно видаляти з галереї власноруч", Toast.LENGTH_SHORT).show();
            }
        }
        backToCreateListActivity();
    }

    private void addAddButton() {
        Uri selectedImage = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.ic_add_photo);
        Image i = new Image();
        i.setURI(selectedImage);
        items.add(i);
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.defect_group:
                // Перевіряємо чи завантажені групи
                if (groupDefects == null || groupDefects.isEmpty()) {
                    Toast.makeText(this, "Немає доступних груп дефектів", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Відкриваємо діалог вибору групи
                ObjectListDialogFragment fragment = ObjectListDialogFragment.newInstance(
                        groupDefects,
                        "Виберіть групу дефектів:",
                        ObjectListDialogFragment.REQUEST_CODE_GROUP
                );
                fragment.show(getSupportFragmentManager(), "GroupDialogFragment");
                break;
            case R.id.defect_subgroup:
                if (selectedGroup == null) {
                    Toast.makeText(this, "Спочатку виберіть групу дефектів", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (subgroupDefects == null || subgroupDefects.isEmpty()) {
                    Toast.makeText(this, "Немає доступних підгруп", Toast.LENGTH_SHORT).show();
                    return;
                }
                ObjectListDialogFragment subgroupFragment = ObjectListDialogFragment.newInstance(
                        subgroupDefects,
                        "Виберіть підгрупу дефектів:",
                        ObjectListDialogFragment.REQUEST_CODE_SUBGROUP
                );
                subgroupFragment.show(getSupportFragmentManager(), "SubgroupDialogFragment");
                break;
            case R.id.searchableSpinner:
                // Перевіряємо чи вибрана група (якщо потрібно)
                if (showDefectGroup && selectedGroup == null) {
                    Toast.makeText(this, "Спочатку виберіть групу дефектів", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (showDefectSubgroup && selectedSubgroup == null) {
                    Toast.makeText(this, "Спочатку виберіть підгрупу дефектів", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Перевіряємо чи є дефекти для вибору
                if (defects == null || defects.isEmpty()) {
                    Toast.makeText(this, "Немає доступних дефектів", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Відкриваємо діалог вибору дефекту (Н31 доступний тільки якщо тепловізор зроблено та виявлено дефекти)
                ObjectListDialogFragment fragment1 = ObjectListDialogFragment.newInstance(
                        filterH31(defects),
                        "Виберіть дефект:",
                        ObjectListDialogFragment.REQUEST_CODE_DEFECT
                );
                fragment1.show(getSupportFragmentManager(), "DefectDialogFragment");
                break;
            case R.id.continue_btn:
                defectTxt = binding.searchableSpinner.getText().toString();
                if (defectTxt.contains("Виберіть дефект")) {
                    Toast.makeText(this, getString(R.string.not_choose_defect), Toast.LENGTH_SHORT).show();
                    break;
                }
                ObjectsDatabase database = App.getInstance().getDatabase();
                ObjectsDao objectsDao = database.objectsDao();
                DefectsDao defectsDao = database.defectsDao();


                Defects ifExists = defectsDao.getCurtainDefect(objId, opr, defectTxt);
                if (ifExists != null) {
                    Toast.makeText(this, getString(R.string.opr_exists), Toast.LENGTH_SHORT).show();
                    break;
                }
                List<Defects> list = defectsDao.getDefectByObjectOprName(objId, opr);
                if (defectTxt.contains("F0 ") && list.size() > 0) {
                    Toast.makeText(this, getString(R.string.defect_exist_on_opr), Toast.LENGTH_SHORT).show();
                    break;
                }
                if (defectTxt.contains("$") && showDefectGroup && uri.size() == 0) {
                    Toast.makeText(this, "Додайте фото до опису даного дефекту", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (defectTxt.startsWith("Н31")) {
                    if (uri.size() == 0) {
                        Toast.makeText(this, getString(R.string.h31_photo_required), Toast.LENGTH_LONG).show();
                        break;
                    }
                    if (binding.adds.getText().toString().trim().isEmpty()) {
                        Toast.makeText(this, getString(R.string.h31_note_required), Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                if (type == R.raw.tp
                        && binding.cbThermalDone.isChecked()
                        && binding.cbDefectsFound.isChecked()
                        && !defectTxt.startsWith("Н31")) {
                    boolean hasH31 = false;
                    for (Defects d : defectsDao.getDefectsByObject(objId)) {
                        if (d.getDefectName() != null && d.getDefectName().startsWith("Н31")) {
                            hasH31 = true;
                            break;
                        }
                    }
                    if (!hasH31) {
                        Toast.makeText(this, getString(R.string.h31_required), Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                noteTxt = binding.adds.getText().toString().replaceAll("\n", ", ").replaceAll(";", ", ");
                previousDefects.add(defectTxt);
                //Defect defect = new Defect(defectTxt, uri, noteTxt);

                Date date = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String strDate = dateFormat.format(date);

                objectsDao.updateDate(objId, strDate);
                objectsDao.updateThermalState(objId,
                        binding.cbThermalDone.isChecked(),
                        binding.cbThermalNotDone.isChecked(),
                        binding.cbDefectsFound.isChecked(),
                        binding.cbDefectsAbsent.isChecked());
                defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, uri, noteTxt, modificator));

                if (defectTxt.contains("F0 ")) {
                    Intent intent = new Intent(DefectActivity.this, WatchCreatedLists.class);
                    intent.putExtra("mod", modificator);
                    intent.putExtra("tabel", tabel);
                    startActivity(intent);
                    finish();
                } else if (!repeat) {
                    Intent intent = new Intent(DefectActivity.this, LastActivity.class);
                    intent.putExtra("objId", objId);
                    intent.putExtra("mod", modificator);
                    intent.putExtra("tabel", tabel);
                    intent.putExtra("defect", previousDefects);
                    intent.putExtra("opr", opr);
                    intent.putExtra("type", type);
                    intent.putExtra("oprOr", oprOrSpan);
                    startActivity(intent);
                    finish();
                } else {
                    backToCreateListActivity();
                }
                break;
        }
    }

    private void backToCreateListActivity() {
        if (!edit) {
            ObjectsDatabase database = App.getInstance().getDatabase();
            ObjectsDao objectsDao = database.objectsDao();
            DefectsDao defectsDao = database.defectsDao();
            List<Defects> list = defectsDao.getDefectsByObject(objId);
            if (list.size() == 0)
                objectsDao.delete(objId);
            Intent intent = new Intent(DefectActivity.this, CreateListActivity.class);
            intent.putExtra("mod", modificator);
            intent.putExtra("tabel", tabel);
            startActivity(intent);
        } else {
            Intent intent = new Intent(DefectActivity.this, EditDefect.class);
            intent.putExtra("objId", objId);
            intent.putExtra("mod", modificator);
            intent.putExtra("tabel", tabel);
            intent.putExtra("type", type);
            intent.putExtra("filial", filial);
            startActivity(intent);
        }
        finish();

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
                DefectActivity.this,
                "org.poe.defectfinder.fileprovider",
                tempImage);
    }

    private void selectPhoto() {
        final CharSequence[] options = {getString(R.string.do_photo), getString(R.string.from_galery), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(DefectActivity.this);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.photo_permission_granted), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Потрібно надати дозвіл на камеру в налаштуваннях вручну", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Uri imageUri = FileProvider.getUriForFile(this, "org.poe.defectfinder.fileprovider", tempImage);
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
            Log.e("DefectActivity", "Failed to copy photo", e);
            return null;
        }
    }

    public static String getRealPathFromURI_API19(Context context, Uri uri) {
        String filePath = "";

        // ExternalStorageProvider
        String docId = DocumentsContract.getDocumentId(uri);
        String[] split = docId.split(":");
        String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
            return Environment.getExternalStorageDirectory() + "/" + split[1];
        } else {
            //getExternalMediaDirs() added in API 21
            File[] external = context.getExternalMediaDirs();
            if (external.length > 1) {
                filePath = external[1].getAbsolutePath();
                filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1];
            }
            return filePath;
        }
    }

    private ArrayList<String> getGroupDefects(int resource) throws IOException {
        ArrayList<String> groups = new ArrayList<>();
        InputStream is = getResources().openRawResource(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        // Читаємо файл і витягуємо унікальні назви груп (перший рівень)
        Set<String> uniqueGroups = new LinkedHashSet<>();
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                String[] parts = line.split(";");
                if (parts.length >= 1) {
                    uniqueGroups.add(parts[0].trim());
                }
            }
        }

        groups.addAll(uniqueGroups);
        reader.close();
        return groups;
    }

    private ArrayList<String> getSubgroupDefects(int resource, String groupName) throws IOException {
        ArrayList<String> subgroups = new ArrayList<>();
        InputStream is = getResources().openRawResource(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        Set<String> uniqueSubgroups = new LinkedHashSet<>();
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                String[] parts = line.split(";");
                if (parts.length >= 3 && parts[0].trim().equals(groupName)) {
                    String subgroup = parts[1].trim();
                    if (!subgroup.isEmpty()) {
                        uniqueSubgroups.add(subgroup);
                    }
                }
            }
        }

        subgroups.addAll(uniqueSubgroups);
        reader.close();
        return subgroups;
    }

    private boolean groupHasSubgroups(int resource, String groupName) throws IOException {
        InputStream is = getResources().openRawResource(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                String[] parts = line.split(";");
                if (parts.length >= 3 && parts[0].trim().equals(groupName)) {
                    if (!parts[1].trim().isEmpty()) {
                        reader.close();
                        return true;
                    }
                }
            }
        }

        reader.close();
        return false;
    }

    /**
     * Універсальний метод для отримання дефектів
     *
     * @param resource - ресурс файлу
     * @param oprOrSpan - для старих об'єктів: 0 = Опори, 1 = Прогони, -1 = не використовується
     * @param groupName - назва групи (equipment) або null
     * @param subgroupName - назва підгрупи або null
     * @return ArrayList з дефектами
     */
    private ArrayList<String> getDefects(int resource, int oprOrSpan, String groupName, String subgroupName) throws IOException {
        InputStream inputStream = getResources().openRawResource(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ArrayList<String> strBuild = new ArrayList<>();

        try {
            String line;

            boolean isNewObjectWithGroups = (resource == R.raw.ps || resource == R.raw.line35);

            if (isDefectAdd || isAddOpr) {
                line = reader.readLine();
                String next = reader.readLine();

                for (boolean first = true, last = (line == null); !last; first = false, line = next) {
                    last = ((next = reader.readLine()) == null);

                    if (line == null || line.trim().isEmpty()) {
                        continue;
                    }

                    if (isNewObjectWithGroups) {
                        String[] parts = line.split(";");
                        if (parts.length >= 3) {
                            String currentGroup = parts[0].trim();
                            String currentSubgroup = parts[1].trim();
                            String defect = parts[2].trim();

                            boolean groupMatch = (groupName == null || currentGroup.equals(groupName));
                            boolean subgroupMatch = (subgroupName == null || currentSubgroup.equals(subgroupName));

                            if (groupMatch && subgroupMatch) {
                                strBuild.add(defect);
                            }
                        }
                    } else if (resource == R.raw.tp || resource == R.raw.kl) {
                        strBuild.add(line);
                    } else {
                        String[] words = line.split(";");
                        if (words.length >= 2) {
                            if (oprOrSpan == 0 && words[0].equals("Опори")) {
                                strBuild.add(words[1]);
                            } else if (oprOrSpan == 1 && words[0].equals("Прогону")) {
                                strBuild.add(words[1]);
                            }
                        }
                    }

                    if (last) {
                        // Видаляємо "Відсутні дефекти" якщо він потрапив у список
                        if (!strBuild.isEmpty()) {
                            String lastItem = strBuild.get(strBuild.size() - 1);
                            if (lastItem.contains("Відсутні дефекти") || lastItem.startsWith("NN0") || lastItem.startsWith("R0") || lastItem.startsWith("F0")) {
                                strBuild.remove(strBuild.size() - 1);
                            }
                        }
                        break;
                    }
                }

                strBuild.removeAll(previousDefects);

            } else {
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    if (isNewObjectWithGroups) {
                        String[] parts = line.split(";");
                        if (parts.length >= 3) {
                            String currentGroup = parts[0].trim();
                            String currentSubgroup = parts[1].trim();
                            String defect = parts[2].trim();

                            boolean groupMatch = (groupName == null || currentGroup.equals(groupName));
                            boolean subgroupMatch = (subgroupName == null || currentSubgroup.equals(subgroupName));

                            if (groupMatch && subgroupMatch) {
                                strBuild.add(defect);
                            }
                        }
                    } else if (resource == R.raw.tp || resource == R.raw.kl) {
                        strBuild.add(line);
                    } else {
                        String[] words = line.split(";");
                        if (words.length >= 2) {
                            if (oprOrSpan == 0 && words[0].equals("Опори")) {
                                strBuild.add(words[1]);
                            } else if (oprOrSpan == 1 && words[0].equals("Прогону")) {
                                strBuild.add(words[1]);
                            }
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

    // Для нових об'єктів з групами (ПС, ПЛ 35кВ)
    private ArrayList<String> getDefects(int resource, String groupName) throws IOException {
        return getDefects(resource, -1, groupName, null);
    }

    private ArrayList<String> getDefects(int resource, String groupName, String subgroupName) throws IOException {
        return getDefects(resource, -1, groupName, subgroupName);
    }

    // Для старих об'єктів з Опори/Прогони
    private ArrayList<String> getDefects(int resource, int oprOrSpan) throws IOException {
        return getDefects(resource, oprOrSpan, null, null);
    }

    public void sendInput(String input, int requestCode) {
        if (requestCode == ObjectListDialogFragment.REQUEST_CODE_GROUP) {
            selectedGroup = input;
            binding.defectGroup.setText(input);

            // Скидаємо підгрупу та дефект
            selectedSubgroup = null;
            selectedDefect = null;
            binding.defectSubgroup.setText("Виберіть підгрупу дефектів");
            binding.searchableSpinner.setText("Виберіть дефект");
            defects = null;

            // Перевіряємо чи є підгрупи для обраної групи
            try {
                boolean hasSubgroups = groupHasSubgroups(type, selectedGroup);
                if (hasSubgroups) {
                    showDefectSubgroup = true;
                    binding.defectSubgroup.setVisibility(View.VISIBLE);
                    subgroupDefects = getSubgroupDefects(type, selectedGroup);
                } else {
                    // Немає підгруп - пропускаємо підгрупу, завантажуємо дефекти
                    showDefectSubgroup = false;
                    binding.defectSubgroup.setVisibility(View.GONE);
                    selectedSubgroup = "";
                    defects = getDefects(type, selectedGroup, "");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Помилка завантаження підгруп", Toast.LENGTH_SHORT).show();
            }

        } else if (requestCode == ObjectListDialogFragment.REQUEST_CODE_SUBGROUP) {
            selectedSubgroup = input;
            binding.defectSubgroup.setText(input);

            // Скидаємо дефект
            selectedDefect = null;
            binding.searchableSpinner.setText("Виберіть дефект");

            // Завантажуємо дефекти для обраної групи + підгрупи
            try {
                defects = getDefects(type, selectedGroup, selectedSubgroup);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Помилка завантаження дефектів", Toast.LENGTH_SHORT).show();
                defects = new ArrayList<>();
            }

        } else if (requestCode == ObjectListDialogFragment.REQUEST_CODE_DEFECT) {
            selectedDefect = input;
            binding.searchableSpinner.setText(input);
        }
    }

}