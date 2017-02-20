package com.example.tranh.pomodoro.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.tranh.pomodoro.R;
import com.example.tranh.pomodoro.database.DbContext;
import com.example.tranh.pomodoro.database.models.Task;
import com.example.tranh.pomodoro.fragments.TaskChangeListenner;
import com.example.tranh.pomodoro.fragments.TaskFragment;
import com.example.tranh.pomodoro.networks.services.GetAllTask;
import com.example.tranh.pomodoro.settings.SharedPrefs;
import com.example.tranh.pomodoro.utils.Util;

import java.util.List;

import butterknife.BindDrawable;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , TaskChangeListenner {
    @BindDrawable(R.drawable.ic_arrow_back_white_24px)
    Drawable drawable;
    private static final String TAG = TaskActivity.class.toString();
    TextView name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {

                    toggle.setDrawerIndicatorEnabled(false);
                    toggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24px);
                    toggle.setToolbarNavigationClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                            Util.hideSoftKeyboard(TaskActivity.this);
                        }
                    });
                } else {
                    toggle.setDrawerIndicatorEnabled(true);
                    toggle.setToolbarNavigationClickListener(null);
                }
            }
        });
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View header = navigationView.getHeaderView(0);
        name = (TextView) header.findViewById(R.id.tv_infoAccount);
        Bundle bundle = getIntent().getExtras();
        String user = bundle.getString("user", "nothing");
        name.setText(user.toUpperCase());
        onTaskChangeListenner(new TaskFragment(), false);
        Log.d(this.toString(), String.format("onCreate: %s", user));
        ButterKnife.bind(this);


    }
    //Todo: để ở đây hay để ở Login?
    public void getAllTask() {
        GetAllTask getAllTaskService = LoginActivity.retrofit.create(GetAllTask.class);
        String token= SharedPrefs.getInstance().getAccessToken();
        Call<List<Task>> getAllTask = getAllTaskService.getAllTask("JWT " + token);
        Log.e(TAG, String.format("getAllTask: %s", token));
        getAllTask.enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                List<Task> tasks = response.body();

                for (int i = 0; i < tasks.size(); i++) {
                    if (tasks.get(i).getName() != null) {
                        DbContext.instance.addTask(tasks.get(i));
                        Log.e(TAG, String.format("onResponse: %s", tasks.get(i).toString()));
                    }
                }
                // cái lày cho ló vẽ nại =))))
                findViewById(R.id.rv_task).invalidate();
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {

            }
        });

    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.task, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                gotoSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void replaceFragment(Fragment f, boolean addToBackTack) {
        if (addToBackTack) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fl_main, f)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fl_main, f)
                    .commit();
        }
    }

    public void gotoSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);

    }
    @Override
    public void onTaskChangeListenner(Fragment f, boolean addtoBackStack) {
        replaceFragment(f, addtoBackStack);
    }
}