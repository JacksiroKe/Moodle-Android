package com.jacksiroke.moodle;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

import java.util.List;

import app.Constants;
import app.MyApplication;
import com.jacksiroke.moodle.fragments.MyCoursesFragment;
import com.jacksiroke.moodle.fragments.SearchCourseFragment;
import com.jacksiroke.moodle.fragments.SettingsFragment;
import com.jacksiroke.moodle.service.NotificationService;
import godau.fynn.librariesdirect.AboutLibrariesActivity;
import godau.fynn.librariesdirect.AboutLibrariesConfig;
import godau.fynn.librariesdirect.Library;
import godau.fynn.librariesdirect.License;
import helper.UserAccount;
import helper.UserUtils;
import io.realm.Realm;
import set.Course;

import static app.Constants.TOKEN;
import static com.jacksiroke.moodle.service.NotificationService.NOTIFICATION_CHANNEL_UPDATES;
import static com.jacksiroke.moodle.service.NotificationService.NOTIFICATION_CHANNEL_UPDATES_BUNDLE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1001;
    private static final String KEY_FRAGMENT = "fragment";
    private UserAccount mUserAccount;
    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_NoActionBar_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUserAccount = new UserAccount(this);
        Constants.TOKEN = mUserAccount.getToken();

        setTitle(mUserAccount.getSitename());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        };



        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Set the nav panel up
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        TextView username = headerView.findViewById(R.id.username);
        TextView fullName = headerView.findViewById(R.id.firstname);
        username.setText(mUserAccount.getUsername());
        fullName.setText(mUserAccount.getFirstName());

        // Set up fragments
        if (savedInstanceState == null) {
            pushView(MyCoursesFragment.newInstance(TOKEN), "My Courses", true);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.content_main);
            if (frag instanceof MyCoursesFragment){
                navigationView.setCheckedItem(R.id.my_courses);
            }
        });

        askPermission();
        createNotificationChannels(); // initialize channels before starting background service
        NotificationService.startService(this, false);
        resolveDeepLink();
        resolveModuleLinkShare();
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }

    private void resolveModuleLinkShare() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getData();
        if(uri != null && action != null && action.equals("android.intent.action.VIEW")) {
            Realm realm = Realm.getInstance(MyApplication.getRealmConfiguration());
            List<Course> courses = realm.copyFromRealm(realm.where(Course.class).findAll());
            int courseId = Integer.parseInt(uri.getQueryParameter("courseId"));

            boolean isEnrolled = false;
            for(Course course : courses) {
                if(course.getCourseId() == courseId) {
                    isEnrolled = true;
                    break;
                }
            }

            if(isEnrolled) {
                String fileUrl = uri.getScheme() + "://" + uri.getHost() + uri.getPath().replace("/fileShare", "") + "?forcedownload=1&token=" + mUserAccount.getToken();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
                startActivity(browserIntent);
            }
            else {
                Toast.makeText(this, "You need to be enrolled in " + uri.getQueryParameter("courseName") + " in order to view", Toast.LENGTH_LONG).show();
            }

            realm.close();

        }
    }

    // Create channels for devices running Oreo and above; Can be safely called even if channel exists
    void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the "Updates Bundle" channel, which is channel for summary notifications that bundles thr
            // individual notifications
            NotificationChannel service = new NotificationChannel(NOTIFICATION_CHANNEL_UPDATES_BUNDLE,
                    "New Content Bundle",
                    NotificationManager.IMPORTANCE_DEFAULT);
            service.setDescription("A default priority channel that bundles all the notifications");

            // Create the "Updates" channel which has the low importance level
            NotificationChannel updates = new NotificationChannel(NOTIFICATION_CHANNEL_UPDATES,
                    "New Content",
                    NotificationManager.IMPORTANCE_LOW);
            updates.setDescription("All low importance channel that relays all the updates.");

            NotificationManager nm = getSystemService(NotificationManager.class);
            // create both channels
            nm.createNotificationChannel(service);
            nm.createNotificationChannel(updates);
        }
    }

    private void resolveDeepLink() {
        String pathString = getIntent().getStringExtra("path");
        if (pathString != null) {
            if (pathString.contains("view.php")) {
                String[] ids = pathString.split("=");
                try {
                    int id = Integer.parseInt(ids[ids.length - 1]);
                    if (id == 1) {
                        //todo open site news
                    } else {
                        Intent intent = new Intent(this, CourseDetailActivity.class);
                        intent.putExtra("id", id);
                        startActivity(intent);
                        return;
                    }

                } catch (NumberFormatException e) {

                }
            }
        }
    }


    private void askPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                AlertDialog.Builder dialog;

                if (MyApplication.getInstance().isDarkModeEnabled()) {
                    dialog = new AlertDialog.Builder(this,R.style.Theme_AppCompat_Dialog_Alert);
                } else {
                    dialog = new AlertDialog.Builder(this,R.style.Theme_AppCompat_Light_Dialog_Alert);
                }

                dialog.setMessage("Need Write permissions to seamlessly Download Files...");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                    }
                });
                dialog.show();

            } else {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_STORAGE);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                askPermission();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void pushView(Fragment fragment, String tag, boolean rootFrag){
        if  (rootFrag){
            clearBackStack();
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_main, fragment, tag);
        if (!rootFrag){
            transaction.addToBackStack(null);
        }
        transaction.commit();
        this.fragment = fragment;
    }


    private AlertDialog askToLogout() {
        AlertDialog.Builder alertDialog;

        if (MyApplication.getInstance().isDarkModeEnabled()) {
            alertDialog = new AlertDialog.Builder(MainActivity.this,R.style.Theme_AppCompat_Dialog_Alert);
        } else {
            alertDialog = new AlertDialog.Builder(MainActivity.this,R.style.Theme_AppCompat_Light_Dialog_Alert);
        }

        alertDialog.setMessage(R.string.confirm_logout);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logout();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        return alertDialog.create();
    }

    public void logout() {
        UserUtils.logout(this);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();
        switch (id) {
            case R.id.my_courses:
                pushView(MyCoursesFragment.newInstance(TOKEN), getString(R.string.my_courses), true);
                break;
            case R.id.course_search:
                pushView(SearchCourseFragment.newInstance(TOKEN), getString(R.string.cousesearch), false);
                break;

            case R.id.settings:
                pushView(new SettingsFragment(), getString(R.string.settings), false);
                break;

            case R.id.about:
                /*AboutLibrariesConfig.setHeaderText(getString(R.string.about_header));
                AboutLibrariesConfig.setLibraries(new Library[]{
                        new Library("moodleDirect", License.MIT_LICENSE, null, "Isa f2k1de & Fynn Godau", true, "https://codeberg.org/fynngodau/moodleDirect"),
                        new Library("CMS-Android", License.MIT_LICENSE, null, "Crux", "https://github.com/crux-bphc/CMS-Android"),
                        new Library("Retrofit", License.APACHE_20_LICENSE, null, "Square, Inc.", "https://square.github.io/retrofit/"),
                        new Library("OkHttp", License.APACHE_20_LICENSE, null, "Square, Inc.", "https://square.github.io/okhttp/"),
                        new Library("Picasso", License.APACHE_20_LICENSE, null, "Square, Inc.", "https://square.github.io/picasso/"),
                        new Library("GlideToVectorYou", License.APACHE_20_LICENSE, null, "Corouteam", "https://github.com/corouteam/GlideToVectorYou"),
                        new Library("Butter Knife", License.APACHE_20_LICENSE, null, "Jake Wharton", "https://jakewharton.github.io/butterknife/"),
                        new Library("Realm", License.APACHE_20_LICENSE, null, "Realm", "https://realm.io/products/realm-database"),
                        new Library("librariesDirect", License.CC0_LICENSE, null, "Fynn Godau", "https://codeberg.org/fynngodau/librariesDirect"),
                });
                startActivity(new Intent(this, AboutLibrariesActivity.class));*/
                break;
            case R.id.logout:
                askToLogout().show();
                break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void clearBackStack() {
        for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); ++i) {
            getSupportFragmentManager().popBackStack();
        }
    }
}
