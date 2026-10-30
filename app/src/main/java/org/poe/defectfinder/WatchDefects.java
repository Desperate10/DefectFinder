package org.poe.defectfinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.poe.defectfinder.dao.DefectsDao;
import org.poe.defectfinder.dao.ObjectsDao;
import org.poe.defectfinder.entities.Defects;
import org.poe.defectfinder.entities.Objects;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.MyApi;
import org.poe.defectfinder.other.BackgroundTask;
import org.poe.defectfinder.other.CustomLoginDialog;
import org.poe.defectfinder.other.Defect;
import org.poe.defectfinder.other.DefectResponse;
import org.poe.defectfinder.other.Opora;
import org.poe.defectfinder.other.PaginationAdapter;
import org.poe.defectfinder.other.PaginationScrollListener;
import org.poe.defectfinder.other.PltxtAdapter;
import org.poe.defectfinder.other.Sheet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WatchDefects extends AppCompatActivity implements View.OnClickListener, PltxtAdapter.ItemClickListener {

    private PaginationAdapter paginationAdapter;
    private MyApi api;
    private ProgressBar progressBar;
    private static final int PAGE_START = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    //private int TOTAL_PAGES = ;
    private int currentPage = PAGE_START;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView recyclerView;
    private String tabel, filial, token, tplnr, opr, pltxtSaved;
    private int oprOrSpan;
    private ArrayList<Sheet> sheets = new ArrayList<>();
    private ArrayList<String> pltxtList = new ArrayList<>();
    private CheckBox lineCheckBox;
    private CheckBox tpCheckBox;
    private Boolean isLineChecked = true;
    private Boolean isTpChecked = true;
    private String tplnrT = "";
    private String pltxtT = "";
    private ArrayList<Sheet> selectedItems = new ArrayList<>();
    PltxtAdapter pltxtAdapter;
    Dialog findDialog;
    private boolean isSelectMode = false;
    private MenuItem defaultItem, addItem;
    ObjectsDatabase database;
    ObjectsDao objectsDao;
    DefectsDao defectsDao;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_defects);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            tabel = bundle.getString("tabel");
        }

        findViewById(R.id.move_up).setVisibility(View.GONE);

        sharedPreferences = getSharedPreferences("filial", MODE_PRIVATE);
        filial = sharedPreferences.getString("filial", "00");
        token = sharedPreferences.getString("token", "");
        pltxtSaved = sharedPreferences.getString("pltxt", "");
        tplnr = "";
        opr = "";

        database = App.getInstance().getDatabase();
        objectsDao = database.objectsDao();
        defectsDao = database.defectsDao();

        recyclerView = findViewById(R.id.sheet_view);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy >0) {
                    // Scroll Down
                    if (findViewById(R.id.move_up).getVisibility() == View.VISIBLE) {
                        findViewById(R.id.move_up).setVisibility(View.GONE);
                    }
                }
                else if (dy <0) {
                    // Scroll Up
                    if (!(findViewById(R.id.move_up).getVisibility() == View.VISIBLE)) {
                        findViewById(R.id.move_up).setVisibility(View.VISIBLE);
                    }
                } else {
                    findViewById(R.id.move_up).setVisibility(View.GONE);
                }
            }
        });
        progressBar = findViewById(R.id.progressbar);

        api = ClientApi.getInstance(this).getCachedApi();

        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        paginationAdapter = new PaginationAdapter(WatchDefects.this);

        paginationAdapter.setOnItemClickListener(new PaginationAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (isSelectMode) {
                    setCreateToolbar();
                    if (selectedItems.contains(sheets.get(position))) {
                        view.setBackgroundColor(Color.TRANSPARENT);
                        selectedItems.remove(sheets.get(position));
                    } else {
                        //Log.d("testim", sheets.get(position)+"");
                        view.setBackgroundResource(R.color.design_default_color_secondary_variant);
                        selectedItems.add(sheets.get(position));
                    }

                    if (selectedItems.size() == 0) {
                        isSelectMode = false;
                        setDefaultToolbar();
                    }
                } else {
                    setDefaultToolbar();
                    createMenuDialog(position);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                isSelectMode = true;
                setCreateToolbar();
                if (selectedItems.contains(sheets.get(position))) {
                    view.setBackgroundColor(Color.TRANSPARENT);
                    selectedItems.remove(sheets.get(position));
                } else {
                    view.setBackgroundResource(R.color.design_default_color_secondary_variant);
                    view.setElevation(6);
                    view.setPadding(4,4,4,4);
                    selectedItems.add(sheets.get(position));
                }
                if (selectedItems.size() == 0) {
                    isSelectMode = false;
                    setDefaultToolbar();
                }
            }
        });
        recyclerView.setLayoutManager(linearLayoutManager);
        paginationAdapter.setHasStableIds(true);
        recyclerView.setAdapter(paginationAdapter);

        /*recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                if(!isLastPage) {
                    isLoading = true;
                    currentPage += 1;
                    loadNextPage(tplnr, opr);
                }
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });*/
        if( pltxtSaved != null) {
            loadFirstPage(pltxtSaved);
        }

        showFindDialog();

    }

    private void createMenuDialog(int position) {
        final CharSequence[] options;// = { "Добавить к исправлению в существующий лист", "Добавить к исправлению в отдельный лист", "Отмена" };
        if (sheets.get(position).getStatus().equals(getString(R.string.not_fixed))) {
            //options = new CharSequence[]{ "Добавить к исправлению в существующий лист", "Добавить к исправлению в отдельный лист", "Отмена" };
            options = new CharSequence[]{ getString(R.string.create_csv), getString(R.string.add_photo), getString(R.string.cancel) };
        } else {
            options = new CharSequence[]{getString(R.string.go_fixing), getString(R.string.cancel)};
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(WatchDefects.this);
        builder.setTitle(getString(R.string.choose_object));

        String linename = sheets.get(position).getLinename();
        String tplnr = sheets.get(position).getTplnr();
        String opr = sheets.get(position).getOpr().get(0).getOpr();
        String defectTxt = sheets.get(position).getOpr().get(0).getDefect().get(0).getDefect();
        int oprOrSpan = sheets.get(position).getOpr().get(0).getOprOrSpan();
        int type = sheets.get(position).getType();
        boolean highVoltage = sheets.get(position).isHighVoltage();
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Добавить к исправлению в существующий лист")) {
                Objects object = objectsDao.getObject(linename, Default.CONFIRM_MODIFICATOR, highVoltage);
                int objId;
                try {
                    objId = object.getId();
                    defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, new ArrayList<>(), "", Default.CONFIRM_MODIFICATOR));
                } catch (NullPointerException e){
                    objId = (int) objectsDao.insert(new Objects(Default.CONFIRM_MODIFICATOR, tabel, "", linename, type, tplnr, getString(R.string.not_uploaded), highVoltage));
                    defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, new ArrayList<>(), "", Default.CONFIRM_MODIFICATOR));
                }
            } else if (options[item].equals("Добавить к исправлению в отдельный лист")) {
                int objId;
                objId = (int) objectsDao.insert(new Objects(Default.CONFIRM_MODIFICATOR, tabel, "", linename, type, tplnr, getString(R.string.not_uploaded), highVoltage));
                defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, new ArrayList<>(), "", Default.CONFIRM_MODIFICATOR));
            } else if (options[item].equals(getString(R.string.add_photo))) {
                int objId;
                objId = (int) objectsDao.insert(new Objects(Default.CONFIRM_MODIFICATOR, tabel, "", linename, type, tplnr, getString(R.string.not_uploaded), highVoltage));
                defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, new ArrayList<>(), "", Default.CONFIRM_MODIFICATOR));
                Intent intent = new Intent(WatchDefects.this, EditDefect.class);
                intent.putExtra("objId", objId);
                intent.putExtra("type", type);
                intent.putExtra("tabel", tabel);
                intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
                intent.putExtra("filial" , filial);
                startActivity(intent);
            } else if (options[item].equals(getString(R.string.create_csv))) {
                int objId;
                objId = (int) objectsDao.insert(new Objects(Default.CONFIRM_MODIFICATOR, tabel, "", linename, type, tplnr, getString(R.string.not_uploaded), highVoltage));
                defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, new ArrayList<>(), "", Default.CONFIRM_MODIFICATOR));
                Intent intent = new Intent(WatchDefects.this,WatchCreatedLists.class);
                intent.putExtra("tabel", tabel);
                intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
                intent.putExtra("pltxt", linename);
                intent.putExtra("defectTxt", defectTxt);
                startActivity(intent);
                finish();
            } else if (options[item].equals(getString(R.string.go_fixing))) {
                if(isSelectMode) {
                    startFixDefects();
                }
                Intent intent = new Intent(WatchDefects.this,WatchCreatedLists.class);
                intent.putExtra("tabel", tabel);
                intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
                startActivity(intent);
                finish();
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void setDefaultToolbar() {
        defaultItem.setVisible(true);
        addItem.setVisible(false);
    }

    private void setCreateToolbar() {
        defaultItem.setVisible(false);
        addItem.setVisible(true);
    }

    private void loadNextPage(String pltxtT) {

        api.getByPltxt(currentPage+"", filial.substring(0,2), pltxtT, "Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
            @Override
            public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) {
                onNextLoad(response);
            }

            @Override
            public void onFailure(Call<List<DefectResponse>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void loadOnlyPltxtNextPage(String tplnr, String pltxt) {

        api.getPltxt(currentPage+"", filial.substring(0,2), pltxt, tplnr, "Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
            @Override
            public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) {
                onNextLoadPltxt(response);
            }

            @Override
            public void onFailure(Call<List<DefectResponse>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void onNextLoadPltxt(Response<List<DefectResponse>> response) {
        isLoading = false;
        progressBar.setVisibility(View.GONE);

        List<DefectResponse> results = response.body();

            for (int i = 0; i < results.size(); i++) {
                DefectResponse result = results.get(i);
                pltxtList.add(result.getPltxt());

            }

        if (!isLastPage) {
            pltxtAdapter.addAll(pltxtList);
        }
    }

    private void loadFirstPage(String pltxt) {
        progressBar.setVisibility(View.VISIBLE);
        api.getByPltxt("0", filial.substring(0,2), pltxt,"Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
            @Override
            public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) {
               // Log.d("testim", "onResponse: ConfigurationListener::"+call.request().url());
                onFirstLoad(response);
                progressBar.setVisibility(View.GONE);
               // if (currentPage <= TOTAL_PAGES) paginationAdapter.addLoadingFooter();
                //else isLastPage = true;
            }

            @Override
            public void onFailure(Call<List<DefectResponse>> call, Throwable t) {
                Toast.makeText(WatchDefects.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }

        });
    }

    private void loadOnlyPltxtFirstPage(String tplnr, String pltxt) {
        progressBar.setVisibility(View.VISIBLE);
        api.getPltxt("0", filial.substring(0,2), pltxt, tplnr,"Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
            @Override
            public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) {
                //Log.d("testim", "onResponse: ConfigurationListener::"+call.request().url());
                onFirstLoadPltxt(response);
                progressBar.setVisibility(View.GONE);
                // if (currentPage <= TOTAL_PAGES) paginationAdapter.addLoadingFooter();
                //else isLastPage = true;
            }

            @Override
            public void onFailure(Call<List<DefectResponse>> call, Throwable t) {
                Toast.makeText(WatchDefects.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }

        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.find_button) {
            showFindDialog();
            return true;
        } else if (item.getItemId() == R.id.add_button) {
            startFixDefects();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startFixDefects() {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String strDate = dateFormat.format(date);

        for(int i = 0; i < selectedItems.size(); i++) {
            String linename = selectedItems.get(i).getLinename();
            String tplnr = selectedItems.get(i).getTplnr();
            String opr = selectedItems.get(i).getOpr().get(0).getOpr();
            String defectTxt = selectedItems.get(i).getOpr().get(0).getDefect().get(0).getDefect();
            int oprOrSpan = selectedItems.get(i).getOpr().get(0).getOprOrSpan();
            int type = selectedItems.get(i).getType();
            boolean highVoltage = selectedItems.get(i).isHighVoltage();

            Objects object = objectsDao.getObject(linename, Default.CONFIRM_MODIFICATOR, highVoltage);
            int objId;
            try {
                objId = object.getId();
                objectsDao.updateDate(objId, strDate);
               // Defects defect = defectsDao.getCurtainDefect(objId, opr, defectTxt);
                defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, new ArrayList<>(), "", Default.CONFIRM_MODIFICATOR));
            } catch (NullPointerException e){
                objId = (int) objectsDao.insert(new Objects(Default.CONFIRM_MODIFICATOR, tabel, "", linename, type, tplnr, "Не вигружено", highVoltage));
                objectsDao.updateDate(objId, strDate);
                defectsDao.insert(new Defects(objId, opr, oprOrSpan, defectTxt, new ArrayList<>(), "", Default.CONFIRM_MODIFICATOR));
            }
        }

        Intent intent = new Intent(WatchDefects.this, WatchCreatedLists.class);
        intent.putExtra("tabel", tabel);
        intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.move_up:
                linearLayoutManager.scrollToPositionWithOffset(0, 0);
                break;
            case R.id.fix_defects:
                if(isSelectMode) {
                    startFixDefects();
                }
                Intent intent = new Intent(WatchDefects.this, WatchCreatedLists.class);
                intent.putExtra("tabel", tabel);
                intent.putExtra("mod", Default.CONFIRM_MODIFICATOR);
                startActivity(intent);
                finish();
                break;
        }
    }

    private void showFindDialog() {
        findDialog = new Dialog(this);
        findDialog.setContentView(R.layout.layout_find_dialog);
        findDialog.setCancelable(false);
        Window window = findDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        //ArrayList<String> pltxtList = new ArrayList<>();
        pltxtList.add("Horse");
        pltxtList.add("Cow");
        pltxtList.add("Camel");
        pltxtList.add("Sheep");
        pltxtList.add("Goat");
        LinearLayout linearLayout = findDialog.findViewById(R.id.linear_layout_main);
        RecyclerView recyclerView1 = (RecyclerView) findDialog.findViewById(R.id.sheet_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView1.setLayoutManager(layoutManager);
        pltxtAdapter = new PltxtAdapter(WatchDefects.this);
        pltxtAdapter.setClickListener(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView1.getContext(),
                layoutManager.getOrientation());
        recyclerView1.addItemDecoration(dividerItemDecoration);
        recyclerView1.setAdapter(pltxtAdapter);





       /* lineCheckBox = dialog.findViewById(R.id.line);
        lineCheckBox.setChecked(isLineChecked);
        tpCheckBox = dialog.findViewById(R.id.tp);
        tpCheckBox.setChecked(isTpChecked);
        tpCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if(isChecked) {
                        opr.setVisibility(View.GONE);
                    } else {
                        opr.setVisibility(View.VISIBLE);
                    }
                }
        );*/
        EditText tplnr = findDialog.findViewById(R.id.tplnr);
        //tplnr.setText(tplnrT);
        EditText pltxt = findDialog.findViewById(R.id.pltxt);
        //pltxt.setText(pltxtT);


        Button find = findDialog.findViewById(R.id.find);
        find.setOnClickListener(v -> {

            if(tplnr.getText().toString().isEmpty() && pltxt.getText().toString().isEmpty()) {

            } else {
                wlp.gravity = Gravity.TOP;
                wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                window.setAttributes(wlp);
                pltxtAdapter.removeAll();
                pltxtList.clear();
                pltxtAdapter.notifyDataSetChanged();

                recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
                    @Override
                    protected void loadMoreItems() {
                        isLoading= true;
                        currentPage += 1;
                        loadOnlyPltxtNextPage(pltxt.getText().toString(), tplnr.getText().toString());
                    }

                    @Override
                    public boolean isLastPage() {
                        return true;
                    }

                    @Override
                    public boolean isLoading() {
                        return isLoading;
                    }
                });
                loadOnlyPltxtFirstPage(pltxt.getText().toString(), tplnr.getText().toString());
            }


            //Log.d("testim", "datestim");

            /*progressBar.setVisibility(View.VISIBLE);
            sheets.clear();
            paginationAdapter.removeAll();
            if((lineCheckBox.isChecked() && tpCheckBox.isChecked()) || (!lineCheckBox.isChecked() && !tpCheckBox.isChecked())) {
                isLineChecked = true;
                isTpChecked = true;
                recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
                    @Override
                    protected void loadMoreItems() {
                        isLoading = true;
                        currentPage += 1;
                        loadNextPage(tplnr.getText().toString(), opr.getText().toString());
                    }

                    @Override
                    public boolean isLastPage() {
                        return isLastPage;
                    }

                    @Override
                    public boolean isLoading() {
                        return isLoading;
                    }
                });
                loadFirstPage(tplnr.getText().toString(), opr.getText().toString());
                progressBar.setVisibility(View.GONE);
            } else if (lineCheckBox.isChecked() && !tpCheckBox.isChecked()) {
                isLineChecked = true;
                isTpChecked = false;
                recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
                    @Override
                    protected void loadMoreItems() {
                        if(!isLastPage) {
                        isLoading = true;
                        currentPage += 1;
                        api.getLineDefects(currentPage+"", filial.substring(0,2), tplnr.getText().toString(), opr.getText().toString(),"Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
                            @Override
                            public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) { Log.d("testim", "onResponse: ConfigurationListener::"+call.request().url());
                               onNextLoad(response);
                            }
                            @Override
                            public void onFailure(Call<List<DefectResponse>> call, Throwable t) {

                            }

                        });
                        }
                    }

                    @Override
                    public boolean isLastPage() {
                        return isLastPage;
                    }

                    @Override
                    public boolean isLoading() {
                        return isLoading;
                    }
                });
                api.getLineDefects("0", filial.substring(0,2), tplnr.getText().toString(), opr.getText().toString(), "Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
                    @Override
                    public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) {
                       // Log.d("testim", "onResponse: ConfigurationListener::"+call.request().url());
                        onFirstLoad(response);

                    }

                    @Override
                    public void onFailure(Call<List<DefectResponse>> call, Throwable t) {
                        Toast.makeText(WatchDefects.this, "Ошибка загрузки!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (!lineCheckBox.isChecked() && tpCheckBox.isChecked()) {
                isLineChecked = false;
                isTpChecked = true;
                //opr.setEnabled(false);
                recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
                    @Override
                    protected void loadMoreItems() {
                        if(!isLastPage) {
                            isLoading = true;
                            currentPage += 1;
                            api.getTpDefects(currentPage+"", filial.substring(0,2), tplnr.getText().toString(),"Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
                                @Override
                                public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) {
                                   // Log.d("testim", "onResponse: ConfigurationListener::"+call.request().url());
                                    onNextLoad(response);
                                }
                                @Override
                                public void onFailure(Call<List<DefectResponse>> call, Throwable t) {
                                    Toast.makeText(WatchDefects.this, "Ошибка загрузки!", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    }

                    @Override
                    public boolean isLastPage() {
                        return isLastPage;
                    }

                    @Override
                    public boolean isLoading() {
                        return isLoading;
                    }
                });
                api.getTpDefects("0", filial.substring(0,2), tplnr.getText().toString(), "Bearer "+token).enqueue(new Callback<List<DefectResponse>>() {
                    @Override
                    public void onResponse(Call<List<DefectResponse>> call, Response<List<DefectResponse>> response) {
                        Log.d("testim", "onResponse: ConfigurationListener::"+call.request().url());
                        onFirstLoad(response);
                    }

                    @Override
                    public void onFailure(Call<List<DefectResponse>> call, Throwable t) {
                        Toast.makeText(WatchDefects.this, "Ошибка загрузки!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            tplnrT = tplnr.getText().toString();
            oprT = opr.getText().toString();
            dialog.dismiss();*/
        });
        Button cancel = findDialog.findViewById(R.id.cancel_button);
        cancel.setOnClickListener(v -> {
            findDialog.dismiss();
        });
        findDialog.show();
    }

    private void onFirstLoadPltxt(Response<List<DefectResponse>> response) {

        List<DefectResponse> results = response.body();
        try {
            /*Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String strDate = dateFormat.format(date);*/
            for (int i = 0; i < results.size(); i++) {
                DefectResponse result = results.get(i);
                //Log.d("testim", result.getPltxt());
                pltxtList.add(result.getPltxt());
            }
        }catch (NullPointerException ex) {Toast.makeText(WatchDefects.this, "Пусто", Toast.LENGTH_SHORT).show();}
        progressBar.setVisibility(View.GONE);
        pltxtAdapter.addAll(pltxtList);
    }



    private void onFirstLoad(Response<List<DefectResponse>> response) {

        List<DefectResponse> results = response.body();
        try {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String strDate = dateFormat.format(date);
            for (int i = 0; i < results.size(); i++) {
                DefectResponse result = results.get(i);
                ArrayList<Opora> oporas = new ArrayList<>();
                List<Defect> defects = new ArrayList<>();
                int type;
                if (result.getPltxt().contains("0,4")) {
                    type = R.raw.line0_4;
                } else if (result.getTplnr().contains("-P")) {
                    type = R.raw.tp;
                } else if (result.getPltxt().contains("КЛ") || result.getPltxt().contains("КВ")) {
                    type = R.raw.kl;
                } else if (result.getPltxt().contains("35")) {
                    type = R.raw.line35;
                } else {
                    type = R.raw.line6_10;
                }
                boolean highVoltage = Default.isHighVoltage(type);

                String defectTxt = null;
                try {
                    defectTxt = getDefectTxt(type, result.getCodedefect());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                defects.add(new Defect(defectTxt, new ArrayList<>(), ""));
                if (result.getCodeplace().contains("-")) {
                    oprOrSpan = 1;
                } else oprOrSpan = 0;
                oporas.add(new Opora(result.getCodeplace(), oprOrSpan, defects));

                String status = getString(R.string.not_fixed);
                Objects object = objectsDao.getObject(result.getPltxt(), Default.CONFIRM_MODIFICATOR, highVoltage);
                int objId;
                try {
                    objId = object.getId();
                    objectsDao.updateDate(objId, strDate);
                    //Log.d("stabim", objId +" "+result.getCodeplace()+" "+defectTxt+" "+object.getStatus());
                    Defects defect = defectsDao.getCurtainDefect(objId, result.getCodeplace(), defectTxt);
                    if (defect != null) {
                        status = getString(R.string.in_process);
                    }
                } catch (NullPointerException e) {
                }
                Sheet sheet = new Sheet(Default.CONFIRM_MODIFICATOR, tabel, result.getPltxt(), result.getTplnr(), "", type,
                        oporas, strDate, status);
                sheet.setHighVoltage(highVoltage);
                sheets.add(sheet);
            }
        }catch (NullPointerException ex) {Toast.makeText(WatchDefects.this, "Пусто", Toast.LENGTH_SHORT).show();}
        progressBar.setVisibility(View.GONE);
        paginationAdapter.addAll(sheets);
    }

    private void onNextLoad(Response<List<DefectResponse>> response) {
        // paginationAdapter.removeLoadingFooter();
        isLoading = false;
        progressBar.setVisibility(View.GONE);

        List<DefectResponse> results = response.body();
        try {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String strDate = dateFormat.format(date);
            for (int i = 0; i < results.size(); i++) {
                DefectResponse result = results.get(i);
                ArrayList<Opora> oporas = new ArrayList<>();
                List<Defect> defects = new ArrayList<>();
                defects.add(new Defect(result.getCodedefect(), new ArrayList<>(), ""));
                int type;
                if (result.getPltxt().contains("0,4")) {
                    type = R.raw.line0_4;
                } else if(result.getTplnr().contains("-P")) {
                    type = R.raw.tp;
                } else if (result.getPltxt().contains("КЛ") || result.getPltxt().contains("КВ")){
                    type = R.raw.kl;
                } else if (result.getPltxt().contains("35")) {
                    type = R.raw.line35;
                } else {
                    type = R.raw.line6_10;
                }
                boolean highVoltage = Default.isHighVoltage(type);

                String defectTxt = null;
                try {
                    defectTxt = getDefectTxt(type, result.getCodedefect());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                defects.add(new Defect(defectTxt, new ArrayList<>(), ""));
                if(result.getCodeplace().contains("-")) {
                    oprOrSpan = 1;
                } else oprOrSpan = 0;
                oporas.add(new Opora(result.getCodeplace(), oprOrSpan, defects));
                String status = getString(R.string.not_fixed);
                Sheet sheet = new Sheet(Default.CONFIRM_MODIFICATOR, tabel, results.get(i).getPltxt(), results.get(i).getTplnr(), "", type,
                        oporas, strDate, status);
                sheet.setHighVoltage(highVoltage);
                sheets.add(sheet);

            }
        }catch (NullPointerException ex) {
            isLastPage = true;
            Toast.makeText(WatchDefects.this, getString(R.string.end_of_lists), Toast.LENGTH_SHORT).show();
        }
        if (!isLastPage) {
            paginationAdapter.addAll(sheets);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.find_defect_menu, menu);
        defaultItem = menu.findItem(R.id.find_button);
        addItem = menu.findItem(R.id.add_button);
        if(isSelectMode) {
            defaultItem.setVisible(false);
            addItem.setVisible(true);
        } else {
            defaultItem.setVisible(true);
            addItem.setVisible(false);
        }

        return true;
    }

    private String getDefectTxt(int resource, String defect) throws IOException {
        InputStream inputStream = getResources().openRawResource(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String defectTxt = "";
        try {
            String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    if (resource != R.raw.tp && resource != R.raw.kl) {
                        String[] words = line.split(";");
                        if (words[1].contains(defect)) {
                            defectTxt = words[1];
                        }
                    } else {
                        if (line.contains(defect)) {
                            defectTxt = line;
                        }
                    }
                }
            }  finally {
                reader.close();
                inputStream.close();
            }

        return defectTxt;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("tabel", tabel);
        startActivity(intent);
    }

    @Override
    public void onItemClick(View view, int position) {
        findDialog.dismiss();
        //Toast.makeText(this, "You clicked on row number " +pltxtAdapter.getItem(position)+" "+ position, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
            sheets.clear();
            paginationAdapter.removeAll();
                isLineChecked = true;
                isTpChecked = true;
                recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
                    @Override
                    protected void loadMoreItems() {
                        isLoading = true;
                        currentPage += 1;
                        loadNextPage(pltxtAdapter.getItem(position));
                    }

                    @Override
                    public boolean isLastPage() {
                        return true;
                    }

                    @Override
                    public boolean isLoading() {
                        return isLoading;
                    }
                });

                loadFirstPage(pltxtAdapter.getItem(position));
                    editor = sharedPreferences.edit();
                    editor.putString("pltxt", pltxtAdapter.getItem(position));
                    editor.apply();
                progressBar.setVisibility(View.GONE);

    }
}