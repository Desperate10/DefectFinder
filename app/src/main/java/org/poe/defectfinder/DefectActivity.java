package org.poe.defectfinder;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.databinding.ActivityDefectBinding;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.other.Image;
import org.poe.defectfinder.other.ImageAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class DefectActivity extends AppCompatActivity implements View.OnClickListener {

    public static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
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

        if ( ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    100);
        }

        ArrayList<String> strBuild = null;
        try {
            strBuild = getDefects(type, oprOrSpan);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, strBuild);
            // Определяем разметку для использования при выборе элемента
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Применяем адаптер к элементу spinner
            //binding.searchableSpinner.setAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        for (int i = 1; i < items.size(); i++) {
            try {
                getContentResolver().delete(items.get(i).getURI(), null, null);
            } catch (SecurityException e) {
                Toast.makeText( this, "Фото зроблені не з додатку потрібно видаляти з галереї власноруч", Toast.LENGTH_SHORT).show();
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
            case R.id.continue_btn:
                ObjectsDatabase database = App.getInstance().getDatabase();
                ObjectsDao objectsDao = database.objectsDao();
                DefectsDao defectsDao = database.defectsDao();

                //defectTxt = binding.searchableSpinner.getSelectedItem().toString();
                Defects ifExists = defectsDao.getCurtainDefect(objId, opr, defectTxt);
                if(ifExists != null) {
                    Toast.makeText(this, getString(R.string.opr_exists), Toast.LENGTH_SHORT).show();
                    break;
                }
                List<Defects> list = defectsDao.getDefectByObjectOprName(objId, opr);
                if (defectTxt.contains("F0 ") && list.size()>0) {
                    Toast.makeText(this, getString(R.string.defect_exist_on_opr), Toast.LENGTH_SHORT).show();
                    break;
                }
                /*if (defectTxt.contains("$") && uri.size() == 0) {
                    Toast.makeText(this, "Додайте фото до опису даного дефекту", Toast.LENGTH_SHORT).show();
                    break;
                }*/
                noteTxt = binding.adds.getText().toString().replaceAll("\n", ", ").replaceAll(";", ", ");
                previousDefects.add(defectTxt);
                //Defect defect = new Defect(defectTxt, uri, noteTxt);

                Date date = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String strDate = dateFormat.format(date);

                objectsDao.updateDate(objId, strDate);
                defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, uri, noteTxt, modificator));

                if (defectTxt.contains("F0 ")) {
                    Intent intent = new Intent(DefectActivity.this, WatchCreatedLists.class);
                    intent.putExtra("mod", modificator);
                    intent.putExtra("tabel", tabel);
                    startActivity(intent);
                    finish();
                }
                else if (!repeat) {
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

    private void selectPhoto() {
        final CharSequence[] options = { getString(R.string.do_photo), getString(R.string.from_galery), getString(R.string.cancel) };
        AlertDialog.Builder builder = new AlertDialog.Builder(DefectActivity.this);
        builder.setTitle(getString(R.string.add_photo));
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals(getString(R.string.do_photo)))
            {
                String filename = filial;
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    tempImage = File.createTempFile(filename, ".jpg", storageDirectory);
                    imageUri = FileProvider.getUriForFile(DefectActivity.this, "org.poe.defectfinder.fileprovider", tempImage);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.photo_permission_granted), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Uri imageUri = FileProvider.getUriForFile(this, "org.poe.defectfinder.fileprovider",tempImage);
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
                    String copiedPath = getRealPathFromURI_API19(this, data.getData());
                    // Log.d("absolute", copiedPath);
                    File file = new File(copiedPath);
                    selectedImage = FileProvider.getUriForFile(DefectActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
                  //  Log.d("absolute", selectedImage.toString());
                } catch (Exception ex) {
                    selectedImage = data.getData();
                    //Log.d("absolute1", selectedImage.toString());
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

    public static String getRealPathFromURI_API19(Context context, Uri uri) {
        String filePath = "";

        // ExternalStorageProvider
        String docId = DocumentsContract.getDocumentId(uri);
        String[] split = docId.split(":");
        String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
            return Environment.getExternalStorageDirectory() + "/" + split[1];
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                //getExternalMediaDirs() added in API 21
                File[] external = context.getExternalMediaDirs();
                if (external.length > 1) {
                    filePath = external[1].getAbsolutePath();
                    filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1];
                }
            } else {
                filePath = "/storage/" + type + "/" + split[1];
            }
            return filePath;
        }
    }

    private ArrayList<String> getDefects(int resource, int oprOrSpan) throws IOException {
        InputStream inputStream = getResources().openRawResource(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ArrayList<String> strBuild = new ArrayList<>();
        try {
            String line = reader.readLine();
            if (isDefectAdd || isAddOpr) {
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
                strBuild.removeAll(previousDefects);
            } else {
                while ((line = reader.readLine()) != null) {
                    if (resource != R.raw.tp && resource != R.raw.kl) {
                        String[] words = line.split(";");
                        if (oprOrSpan == 0 && words[0].equals("Опори")) {
                            strBuild.add(words[1]);
                        } else if (oprOrSpan == 1 && words[0].equals("Прогону")) {
                            strBuild.add(words[1]);
                        }
                    } else {
                        strBuild.add(line);
                    }
                }
            }
        } finally {
            reader.close();
            inputStream.close();
        }

        return strBuild;
    }

}