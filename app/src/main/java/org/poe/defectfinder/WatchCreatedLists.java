package org.poe.defectfinder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.databinding.ActivityWatchCreatedListsBinding;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.entities.Objects;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.MyApi;
import org.poe.defectfinder.network.UploadRequestBody;
import org.poe.defectfinder.network.UploadResponse;
import org.poe.defectfinder.other.BackgroundTask;
import org.poe.defectfinder.other.CustomLoginDialog;
import org.poe.defectfinder.other.Defect;
import org.poe.defectfinder.other.Opora;
import org.poe.defectfinder.other.Sheet;
import org.poe.defectfinder.other.SheetAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.Configuration;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Multipart;


public class WatchCreatedLists extends AppCompatActivity implements UploadRequestBody.UploadCallback {

    static String notificationChannelID = "1";

    ActivityWatchCreatedListsBinding binding;
    private SheetAdapter listAdapter;
    private ArrayList<Sheet> sheetsList = new ArrayList<>();
    private String modificator, token;
    //private NotificationManager manager;
    //private static NotificationCompat.Builder notification;
    private String filial, tabel, pltxt, defectTxt;
    private static boolean error;
   // private CIFSContext auth = null;
    private MyApi api;
    ObjectsDatabase db;
    ObjectsDao objectsDao;
    DefectsDao defectsDao;
    private ArrayList<String> files = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWatchCreatedListsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        api = ClientApi.getInstance(this).getApi();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.sheetView.setLayoutManager(layoutManager);
        listAdapter = new SheetAdapter(sheetsList, this);
        listAdapter.setOnEntryClickListener((view1, position) -> {
            createMenuDialog(position);
        });
        binding.sheetView.setAdapter(listAdapter);

        SharedPreferences sharedPreferences = getSharedPreferences("filial", MODE_PRIVATE);
        filial = sharedPreferences.getString("filial", "00");
        token = sharedPreferences.getString("token", "");

        db = App.getInstance().getDatabase();
        objectsDao = db.objectsDao();
        defectsDao = db.defectsDao();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            tabel = bundle.getString("tabel");
            modificator = bundle.getString("mod");
            pltxt = bundle.getString("pltxt");
            defectTxt = bundle.getString("defectTxt");
        }


        updateList();
        //Log.d("csv", token);

        api.checkTokenValidity(token).enqueue(new Callback<org.poe.defectfinder.network.Response>() {
            @Override
            public void onResponse(Call<org.poe.defectfinder.network.Response> call, Response<org.poe.defectfinder.network.Response> response) {
                if(response.code() == 200) {
                   // Log.d("tokenyk", response.body().getError()+ " " +response.body().getMessage());
                    if (response.body().getError().equals("true")) {
                        CustomLoginDialog customLoginDialog = new CustomLoginDialog(WatchCreatedLists.this);
                        customLoginDialog.setCancelable(false);
                        customLoginDialog.show(getSupportFragmentManager(), "login_dialog");
                    }
                }
            }

            @Override
            public void onFailure(Call<org.poe.defectfinder.network.Response> call, Throwable t) {

            }
        });
    }

    private void updateList() {
        new BackgroundTask(WatchCreatedLists.this) {

            @Override
            public long doInBackground() {
                readSaved();
                return 0;
            }

            @Override
            public void onPostExecute(long id) {
                listAdapter.notifyDataSetChanged();
                try{
                    Objects oject = objectsDao.getObject(pltxt, modificator);
                   // Log.d("testim", oject.getName());
                    for (int i = 0; i < sheetsList.size(); i++) {
                        if(sheetsList.get(i).getLinename().equals(pltxt) && sheetsList.get(i).getOpr().get(0).getDefect().get(0).getDefect().equals(defectTxt) && sheetsList.get(i).getModificator().equals(getString(R.string.not_fixed))) {
                            loadImages(i);
                        }
                    }
                } catch (NullPointerException e) {}
            }
        }.execute();
    }

    private void readSaved() {
        List<Objects> objects;
        if (!modificator.equals(Default.CONFIRM_MODIFICATOR)) {
            objects = objectsDao.getAllAddEdit();
        } else {
            objects = objectsDao.getAllConfirm();
        }
        for(int i = 0; i < objects.size(); i++) {
            List<Defects> defects = defectsDao.getDefectsByObject(objects.get(i).getId());
            Map<String, List<Defects>> subs = new HashMap<>();
            for(Defects defect : defects) {
                List<Defects> temp = subs.get(defect.getOprName());
                if (temp ==null) {
                    temp = new ArrayList<>();
                    subs.put(defect.getOprName(), temp);
                }
                temp.add(defect);
            }
            ArrayList<Opora> oporas = new ArrayList<>();
            for (List<Defects> value : subs.values()) {
                List<Defect> defects1 = new ArrayList<>();
                for (int j = 0; j < value.size(); j++) {
                    defects1.add(new Defect(value.get(j).getDefectName(), value.get(j).getPhotos(), value.get(j).getNote()));
                }
                oporas.add(new Opora(value.get(0).getOprName(), value.get(0).getOprOrSpan(), defects1));
            }
            sheetsList.add(new Sheet(objects.get(i).getId(), objects.get(i).getModificator(), objects.get(i).getTabel(), objects.get(i).getName(), objects.get(i).getTplnr(), objects.get(i).getObhod(), objects.get(i).getType(), oporas, objects.get(i).getDate(), objects.get(i).getStatus()));
        }

    }

    /*public String getRealPathFromUri(Uri contentUri) {
        Cursor cursor = null;
        int column_index = 1;
       // final int currentDefect = defectNumber+1;
       // final int currentPhoto = photoNumber+1;
        String finalCursorString = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getContentResolver().query(contentUri, proj, null, null, null);
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
        } catch (Exception e) {
           // new Handler(Looper.getMainLooper()).post(() -> {
               // Toast toast = Toast.makeText(getApplicationContext(), "Отсутствие доступа к снимку под номером "+currentPhoto+" в дефекте "+ currentDefect, Toast.LENGTH_SHORT);
                //toast.show();
           //     error = true;
             *//*   notification.setProgress(0, 0, false);
                notification.setOngoing(false);
                notification.setAutoCancel(true);
                notification.setContentTitle("Ошибка загрузки фото...");
                notification.setContentText("Ошибка загрузки фото под номером "+currentPhoto+" в дефекте "+ currentDefect+". Замените или удалите фото из фрагмента");*//*
            //});
        }finally {
            if(column_index != 1) {
                finalCursorString = cursor.getString(column_index);
            } else {
                finalCursorString = "";
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return finalCursorString;
    }*/

    /*private void uploadCSV( StringBuilder stringBuilder, String tplnr, String tabel, String modificator) {
        String url = "smb://10.107.25.10/data/defects/"+filial+"/csv/"+tplnr+"_"+tabel +"_"+modificator+".csv";
        try {
            SmbFile smb = new SmbFile(url, auth);
            SmbFileOutputStream out = new SmbFileOutputStream(smb);

            out.write(String.valueOf(stringBuilder).getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
            //Log.d("attempt", "finished");
            try {
                // Sleep for 1 second
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            notification.setProgress(0, 0, false);
            notification.setContentTitle("Загрузка файлов завершена").setContentText("CSV-файл выгружено");
            notification.setOngoing(false);
            notification.setAutoCancel(true);
            notification.setPriority(NotificationCompat.PRIORITY_LOW);
            manager.notify(1, notification.build());
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText(WatchCreatedLists.this, "CSV-файл выгружено", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        } catch (Exception e) {
            error = true;
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText(WatchCreatedLists.this, "Ошибка загрузки csv-файла", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
            notification.setProgress(0, 0, false);
            notification.setOngoing(false);
            notification.setAutoCancel(true);
            notification.setContentTitle("Ошибка...");
            notification.setContentText("Ошибка загрузки csv-файла");
            manager.notify(1, notification.build());
            e.printStackTrace();
        }
    }*/

    /*private void uploadPhotos(String url, File file) {
        try {
            SmbFile smb = new SmbFile(url, auth);
            SmbFileOutputStream out = new SmbFileOutputStream(smb);
            FileInputStream fis = new FileInputStream(file);

            long filesize = file.length();

            byte[] b = new byte[8192 * 10];
            int n, tot = 0;
            // Log.d("asdf", "initiating : total=" + tot);
            while ((n = fis.read(b)) > 0) {
                out.write(b, 0, n);
                tot += n;
                Long fileS = filesize;
                int down = tot;
                int max  = fileS.intValue();

                notification.setProgress(max, down, false);
                notification.setContentText("Загрузка фото...");
                manager.notify(1, notification.build());
                //  Log.d("asdf", "writing : total=" + tot);
            }
            fis.close();
            out.flush();
            out.close();
            notification.setContentText("Загрузка фото завершена");
            manager.notify(1, notification.build());
        } catch (Exception e) {
            error = true;
            try {
                // Sleep for 1 second
                Thread.sleep(1000);
            } catch (InterruptedException er) {
                er.printStackTrace();
            }
            notification.setProgress(0, 0, false);
            notification.setOngoing(false);
            notification.setAutoCancel(true);
            notification.setContentTitle("Ошибка...");
            notification.setContentText("Ошибка загрузки фото");
            manager.notify(1, notification.build());
            e.printStackTrace();
        }
    }*/

    private void createMenuDialog(int position) {
        final CharSequence[] options;// = { "Создать csv-файл", "Добавить опору", "Редактировать лист", "Удалить лист", "Отмена" };
        if (sheetsList.get(position).getStatus().equals(getString(R.string.fixed))) {
            options = new CharSequence[]{getString(R.string.delete_list), getString(R.string.cancel)};
        } else if(sheetsList.get(position).getOpr().get(0).getDefect().get(0).getDefect().contains("F0 ")){
            options = new CharSequence[]{getString(R.string.create_csv), getString(R.string.delete_list), getString(R.string.cancel)};
        } else if(sheetsList.get(position).getModificator().equals(Default.CONFIRM_MODIFICATOR)){
            options = new CharSequence[]{getString(R.string.create_csv), getString(R.string.add_photo), getString(R.string.delete_list), getString(R.string.cancel)};
        } else {
            options = new CharSequence[]{getString(R.string.create_csv), getString(R.string.add_opr), getString(R.string.edit_list), getString(R.string.delete_list), getString(R.string.cancel)};
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(WatchCreatedLists.this);
        builder.setTitle(R.string.choose_option);
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals(getString(R.string.create_csv))) {
               // ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
               // Network[] networks = connectivityManager.getAllNetworks();
               // boolean vpnInUse = false;
               // for (int i = 0; i < networks.length; i++) {
               //     NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(networks[i]);
               //     if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
               //         vpnInUse = true;
               //     }
              //  }
               // if (vpnInUse) {
                if(sheetsList.get(position).getModificator().equals(Default.CONFIRM_MODIFICATOR)) {
                    List<Opora> oporas = sheetsList.get(position).getOpr();
                    for (int i = 0; i < oporas.size(); i++) {
                        List<Defect> defects = oporas.get(i).getDefect();
                        for (int j = 0; j < defects.size(); j++) {
                            String defectstxt = defects.get(j).getDefect();
                            /*if (defectstxt.contains("$") && defects.get(j).getPhoto().size() == 0) {
                                Toast.makeText(this, "Добавьте фото к дефектам со знаком $", Toast.LENGTH_SHORT).show();
                                return;
                            }*/
                        }
                    }
                }
                    loadImages(position);
               // } else {
                 //   Toast.makeText(this, "Подключитесь к VPN", Toast.LENGTH_SHORT).show();
                //}
            } else if (options[item].equals(getString(R.string.add_opr))) {
                if (sheetsList.get(position).getType() != R.raw.tp && sheetsList.get(position).getType() != R.raw.kl) {
                    Intent intent = new Intent(WatchCreatedLists.this, AddOprToLine.class);
                    intent.putExtra("linename", sheetsList.get(position).getLinename());
                    intent.putExtra("tabel", sheetsList.get(position).getTabel());
                    intent.putExtra("type", sheetsList.get(position).getType());
                    intent.putExtra("objId", sheetsList.get(position).getId());
                    intent.putExtra("mod", modificator);
                    startActivity(intent);
                } else {
                    dialog.dismiss();
                    Toast.makeText(WatchCreatedLists.this, getString(R.string.cant_add_opr), Toast.LENGTH_SHORT).show();
                }
            } else if (options[item].equals(getString(R.string.delete_list))) {
                DialogInterface.OnClickListener dialogClickListener = (dialog1, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            List<Opora> oporas = sheetsList.get(position).getOpr();
                            for (int i = 0; i < oporas.size(); i++) {
                                List<Defect> defects = oporas.get(i).getDefect();
                                for (int j = 0; j < defects.size(); j++) {
                                    List<String> photos = defects.get(j).getPhoto();
                                    for (int k = 0; k < photos.size(); k++) {
                                        String photo = photos.get(k);
                                        Uri uri = Uri.parse(photo);
                                        try {
                                            getContentResolver().delete(uri, null, null);
                                        } catch (SecurityException | IllegalArgumentException e) {}
                                    }
                                }
                            }
                            deleteList(position);
                            Toast.makeText(this, getString(R.string.list_succ_deleted), Toast.LENGTH_LONG).show();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            dialog1.dismiss();
                            break;
                    }
                };

                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setMessage(getString(R.string.r_sure_question)).setPositiveButton(getString(R.string.yes), dialogClickListener)
                        .setNegativeButton(getString(R.string.no), dialogClickListener).show();
            } else if (options[item].equals(getString(R.string.edit_list)) || options[item].equals(getString(R.string.add_photo))) {
                Intent intent = new Intent(WatchCreatedLists.this, EditDefect.class);
                intent.putExtra("objId", sheetsList.get(position).getId());
                intent.putExtra("mod", sheetsList.get(position).getModificator());
                intent.putExtra("type", sheetsList.get(position).getType());
                intent.putExtra("tabel", sheetsList.get(position).getTabel());
                intent.putExtra("filial" , filial);

                startActivity(intent);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void writeCsv(String fileName, String content) {
        File path = getFilesDir();
        try {
            File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File tempImage = File.createTempFile(fileName, ".csv", storageDirectory);
           // File file = new File(path, fileName);
            FileOutputStream writer = new FileOutputStream(tempImage);
            writer.write(content.getBytes());
            writer.close();
            Uri csvUri = FileProvider.getUriForFile(WatchCreatedLists.this, "org.poe.defectfinder.fileprovider", tempImage);
            files.add(0, csvUri.toString());
           // Uri contentUri = FileProvider.getUriForFile(this, "org.poe.defectfinder.fileprovider", file);
           // files.add(Uri.parse(file.getAbsolutePath()));
            //Log.d("csv", csvUri.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri, UploadRequestBody body) {
        File file = new File(fileUri.getPath());
       // Log.d("absolute", file.getName());
        if (file.getName().contains(".csv")) {
            return MultipartBody.Part.createFormData(partName, file.getName(), body);
        } else if(file.getName().contains(".jpg") || file.getName().contains(".jpeg")) {
            return MultipartBody.Part.createFormData(partName, filial + file.getName(), body);
        } else {
            return MultipartBody.Part.createFormData(partName, filial + file.getName() + ".jpg", body);
        }
    }

    private void uploadFiles(List<String> uriStrings, int position) throws IOException {
        ArrayList<MultipartBody.Part> list = new ArrayList<>();
       // ContentResolver contentResolver = getContentResolver();
       // int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;


        for (int i = 0; i< uriStrings.size(); i++) {

//            contentResolver.takePersistableUriPermission(Uri.parse(uriStrings.get(i)), takeFlags);
         //   Log.d("absolutepath", uriStrings.get(i));
            ParcelFileDescriptor pfsf = getContentResolver().openFileDescriptor(Uri.parse(uriStrings.get(i)), "r", null);

            if (pfsf != null) {
                FileInputStream fileInputStream = new FileInputStream(pfsf.getFileDescriptor());
                String name;
                Cursor returnCursor = getContentResolver().query(Uri.parse(uriStrings.get(i)), null, null, null, null);
                if (returnCursor != null) {
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    returnCursor.moveToFirst();
                    name = returnCursor.getString(nameIndex);
                    returnCursor.close();

                } else {
                    name = Uri.parse(uriStrings.get(i)).getPath();
                }

                File file = new File(getCacheDir(), name);

                FileOutputStream outputStream = new FileOutputStream(file);
                int bytesCopied;
                byte[] buffer = new byte[4096];
                while ((bytesCopied = fileInputStream.read(buffer)) != -1)
                {
                    outputStream.write(buffer, 0, bytesCopied);
                }
                UploadRequestBody body = new UploadRequestBody(file, "*", this);
                list.add(prepareFilePart("files", Uri.parse(uriStrings.get(i)), body));
                returnCursor.close();
            }
        }
        binding.progressBar.setProgress(0);
        api.uploadDefects(list,
                RequestBody.create(MediaType.parse("multipart/form-data"), "json"), "Bearer "+token
        ).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
               // Log.d("csv", token +" "+response.body().);
                if (response.code() == 200) {
                    Snackbar.make(findViewById(R.id.coord_layout), response.body().getMessage(), Snackbar.LENGTH_LONG).show();
                    binding.progressBar.setProgress(100);
                    if (response.body().getMessage().contains(list.size()+"")) {
                        changeStatus(position);
                    }
                } else if (response.code() == 403){
                    Snackbar.make(findViewById(R.id.coord_layout), getString(R.string.auth_error), Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
              //  Log.d("testim", t.getMessage());
               // error = true;
                Snackbar.make(findViewById(R.id.coord_layout), t.getMessage(), Snackbar.LENGTH_LONG).show();
                binding.progressBar.setProgress(0);
            }
        });
    }

    @Override
    public void onProgressUpdate(int percentage) {
        binding.progressBar.setProgress(percentage);
    }

    private void deleteList(int position) {
        new BackgroundTask(WatchCreatedLists.this) {
            @Override
            public long doInBackground() {
                objectsDao.delete(sheetsList.get(position).getId());
                defectsDao.deleteDefectsByObject(sheetsList.get(position).getId());
                return 0;
            }

            @Override
            public void onPostExecute(long id) {

                sheetsList.remove(position);
                listAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

    private void loadImages(int position) {
        Toast.makeText(this, "Загрузка об'єкта " + sheetsList.get(position).getLinename()+ " почалась", Toast.LENGTH_LONG).show();
        //NOtification
        /*manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(notificationChannelID, "Upload Channel", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }

        notification = new NotificationCompat.Builder(this, notificationChannelID)
                .setSmallIcon(R.drawable.ic_add)
                .setContentTitle("Загрузка...")
                .setContentText("Загрузка файлов...")
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        manager.notify(1, notification.build());*/

       /* new Thread(new Runnable() {
            @Override
            public void run() {*/
                error = false;
                Sheet record = sheetsList.get(position);
                List<Opora> oporas = sheetsList.get(position).getOpr();
                StringBuilder stringBuilder = new StringBuilder();
             //   BaseContext baseCxt = null;
             //   Properties jcifsProperties = new Properties();
             //   jcifsProperties.setProperty("jcifs.smb.client.enableSMB2", "true");
             //   jcifsProperties.setProperty("jcifs.smb.client.dfs.disabled", "true");
             //   Configuration config = null;
             //   try {
             //       config = new PropertyConfiguration(jcifsProperties);
             //   } catch (CIFSException e) {
             //       e.printStackTrace();
             //   }
             //   baseCxt = new BaseContext(config);
             //   auth = baseCxt.withCredentials(new NtlmPasswordAuthenticator("", "defectfinder", "123Qwerty"));
                files = new ArrayList<>();
                for (int i = 0; i < oporas.size(); i++) {
                    List<Defect> defects = oporas.get(i).getDefect();
                    for (int j = 0; j < defects.size(); j++) {
                        List<String> images = defects.get(j).getPhoto();
                        //формируем строку для записи в csv
                        //stringBuilder.append(record.getObhod()+";"+record.getTabel()+";"+record.getTplnr()+";"+defects.get(j).getDefect().split(" ")[0]+";"+oporas.get(i).getOpr()+";");
                        // links = new ArrayList<>();
                        for (int k = 0; k < images.size(); k++) {
                            String photo = images.get(k);
                           // Log.d("csv", photo);
                            files.add(photo);
                            //arraylist.add(photo)
                            //после stringbuilder сохранить в csv и взять стринговое uri и потом метод на отправку
                            //и добавить полосу загрузки и snackbar для оповещения вместо полосы загрузки?
                            Uri uri = Uri.parse(photo);
                            //File file1;
                            String url= "fake";
                            if (uri.getLastPathSegment().contains(".")) {
                              //  file1 = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + uri.getLastPathSegment());
                                //поменять урлы
                                url = "smb://10.107.25.10/data/defects/"+filial+"/foto/" +filial+uri.getLastPathSegment();
                                //files.add(filial+"/"+uri.getLastPathSegment()+".jpg");
                            } else {
                             //   file1 = new File(getRealPathFromUri(WatchCreatedLists.this, uri, j, k));
                                url = "smb://10.107.25.10/data/defects/"+filial+"/foto/" + filial+uri.getLastPathSegment() + ".jpg";
                               // files.add(filial+"/"+uri.getLastPathSegment());
                            }
                            if( error) return;
                            //links.add(url);
                            if(!modificator.equals(Default.CONFIRM_MODIFICATOR)) {
                                stringBuilder.append(record.getObhod() + ";" + record.getTabel() + ";" + record.getTplnr() + ";" + defects.get(j).getDefect().split(" ")[0] + ";" + oporas.get(i).getOpr() + ";" + defects.get(j).getNote() + ";[" + url + "];" + record.getTime() + ";[]\n");
                            } else {
                           //     if(url.equals("fake")) {
                                    stringBuilder.append(";" + record.getTabel() + ";" + record.getTplnr() + ";" + defects.get(j).getDefect().split(" ")[0] + ";" + oporas.get(i).getOpr() + ";" + defects.get(j).getNote() + ";[];" + record.getTime() + ";[" + url + "];" + "\n");
                            //    } else {
                            //        stringBuilder.append(";" + record.getTabel() + ";" + record.getTplnr() + ";" + defects.get(j).getDefect().split(" ")[0] + ";" + oporas.get(i).getOpr() + ";" + defects.get(j).getNote() + ";[];" + record.getTime() + ";[fake];" + "\n");
                           //     }
                            }
                          //  uploadPhotos(url, file1);
                          //  if (k == images.size()-1) {
                           //     checkLastPhoto(url);
                            //}
                        }
                        if (images.size() == 0) {
                            if(!modificator.equals(Default.CONFIRM_MODIFICATOR)) {
                                stringBuilder.append(record.getObhod()+";"+record.getTabel()+";"+record.getTplnr()+";"+defects.get(j).getDefect().split(" ")[0]+";"+oporas.get(i).getOpr()+";"+defects.get(j).getNote()+";[];"+record.getTime()+";[]\n");
                            } else {
                                stringBuilder.append(";" +record.getTabel()+";"+record.getTplnr()+";"+defects.get(j).getDefect().split(" ")[0]+";"+oporas.get(i).getOpr()+";"+defects.get(j).getNote()+";[];"+record.getTime()+";[fake]\n");
                            }
                        }
                        //Дописать картинки в конец каждого дефекта и переходим к записи следующего
                        //stringBuilder.append(links+";"+record.getTime()+"\n");
                        /*if (j == defects.size() - 1) {
                            notification.setContentTitle("Загрузка файлов завершена");
                            notification.setOngoing(false);
                            notification.setProgress(0, 0, false);
                            notification.setPriority(NotificationCompat.PRIORITY_LOW);
                            manager.notify(1, notification.build());
                        }*/
                    }

                }
                writeCsv(record.getTplnr()+"_"+record.getTabel()+"_"+record.getModificator(), stringBuilder.toString());
                //if (!error) {
                 //  uploadCSV(stringBuilder, record.getTplnr(), record.getTabel(), record.getModificator());
                //}
                if (!error) {
                 //   changeStatus(position);
                }
        try {
            uploadFiles(files ,position);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     //   }).start();
   // }

    /*private void checkLastPhoto(String url) {
        try {
            SmbFile smb = new SmbFile(url, auth);
            if(smb.exists()) {
                error = false;
            }
        } catch (Exception e) {
            runOnUiThread(() ->Toast.makeText(this, "Проблема с выгрузкой фото", Toast.LENGTH_SHORT).show());
            error = true;
        }
    }*/

    private void changeStatus(int position) {
        objectsDao.updateStatus(sheetsList.get(position).getId());
        sheetsList.clear();
        readSaved();
        runOnUiThread(() -> {
            listAdapter = new SheetAdapter(sheetsList, this);
            listAdapter.setOnEntryClickListener((view1, position1) -> {
                createMenuDialog(position1);
            });
            binding.sheetView.setAdapter(listAdapter);
            listAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(WatchCreatedLists.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent intent;
        if(tabel == null) {
            intent = new Intent(WatchCreatedLists.this, MainActivity.class);
        } else if(modificator.equals(Default.CONFIRM_MODIFICATOR) && !tabel.isEmpty()){
            intent = new Intent(WatchCreatedLists.this, WatchDefects.class);
            intent.putExtra("tabel", tabel);
            //intent.putExtra("mod", modificator);
        } else {
            intent = new Intent(WatchCreatedLists.this, CreateListActivity.class);
            intent.putExtra("tabel", tabel);
            intent.putExtra("mod", modificator);
        }
        startActivity(intent);
        finish();
    }

}