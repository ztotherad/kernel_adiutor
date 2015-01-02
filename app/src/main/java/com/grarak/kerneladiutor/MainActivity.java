package com.grarak.kerneladiutor;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.grarak.kerneladiutor.elements.ListAdapter;
import com.grarak.kerneladiutor.elements.ScrimInsetsFrameLayout;
import com.grarak.kerneladiutor.fragments.AboutUsFragment;
import com.grarak.kerneladiutor.fragments.BuildpropFragment;
import com.grarak.kerneladiutor.fragments.CPUFragment;
import com.grarak.kerneladiutor.fragments.CPUVoltageFragment;
import com.grarak.kerneladiutor.fragments.FrequencyTableFragment;
import com.grarak.kerneladiutor.fragments.GPUFragment;
import com.grarak.kerneladiutor.fragments.IOFragment;
import com.grarak.kerneladiutor.fragments.KSMFragment;
import com.grarak.kerneladiutor.fragments.KernelInformationFragment;
import com.grarak.kerneladiutor.fragments.LMKFragment;
import com.grarak.kerneladiutor.fragments.MiscControlsFragment;
import com.grarak.kerneladiutor.fragments.ScreenFragment;
import com.grarak.kerneladiutor.fragments.VMFragment;
import com.grarak.kerneladiutor.utils.Constants;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.kernel.CPUVoltage;
import com.grarak.kerneladiutor.utils.kernel.GPU;
import com.grarak.kerneladiutor.utils.kernel.KSM;
import com.grarak.kerneladiutor.utils.kernel.LMK;
import com.grarak.kerneladiutor.utils.kernel.Screen;
import com.grarak.kerneladiutor.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 01.12.14.
 */
public class MainActivity extends ActionBarActivity implements Constants {

    private boolean hasRoot;
    private boolean hasBusybox;

    private ProgressBar progressBar;
    private Toolbar toolbar;

    private String mTitle;

    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ScrimInsetsFrameLayout mScrimInsetsFrameLayout;
    private ListView mDrawerList;

    private final List<ListAdapter.ListItem> mList = new ArrayList<>();

    private int cur_position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = new ProgressBar(this);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(progressBar, new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END));

        new Task().execute();
    }

    private void selectItem(int position) {
        Fragment fragment = mList.get(position).getFragment();

        if (fragment == null || cur_position == position) {
            mDrawerList.setItemChecked(cur_position, true);
            return;
        }

        mDrawerLayout.closeDrawer(mScrimInsetsFrameLayout);

        cur_position = position;

        Log.i(TAG, "Open postion " + position + ": " + mList.get(position).getTitle());
        getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commitAllowingStateLoss();

        setTitle(mList.get(position).getTitle());
        mDrawerList.setItemChecked(position, true);
    }

    private void setList() {
        mList.clear();
        mList.add(new ListAdapter.MainHeader());
        mList.add(new ListAdapter.Header(getString(R.string.information)));
        mList.add(new ListAdapter.Item(getString(R.string.kernel_information), new KernelInformationFragment()));
        mList.add(new ListAdapter.Item(getString(R.string.frequency_table), new FrequencyTableFragment()));
        mList.add(new ListAdapter.Header(getString(R.string.kernel)));
        mList.add(new ListAdapter.Item(getString(R.string.cpu), new CPUFragment()));
        if (CPUVoltage.hasCpuVoltage())
            mList.add(new ListAdapter.Item(getString(R.string.cpu_voltage), new CPUVoltageFragment()));
        if (GPU.hasGpuControl())
            mList.add(new ListAdapter.Item(getString(R.string.gpu), new GPUFragment()));
        if (Screen.hasScreen())
            mList.add(new ListAdapter.Item(getString(R.string.screen), new ScreenFragment()));
        mList.add(new ListAdapter.Item(getString(R.string.io_scheduler), new IOFragment()));
        if (KSM.hasKsm())
            mList.add(new ListAdapter.Item(getString(R.string.ksm), new KSMFragment()));
        if (LMK.hasMinFree())
            mList.add(new ListAdapter.Item(getString(R.string.low_memory_killer), new LMKFragment()));
        mList.add(new ListAdapter.Item(getString(R.string.virtual_machine), new VMFragment()));
        mList.add(new ListAdapter.Item(getString(R.string.misc_controls), new MiscControlsFragment()));
        mList.add(new ListAdapter.Header(getString(R.string.tools)));
        mList.add(new ListAdapter.Item(getString(R.string.build_prop_editor), new BuildpropFragment()));
        mList.add(new ListAdapter.Header(getString(R.string.other)));
        mList.add(new ListAdapter.Item(getString(R.string.about_us), new AboutUsFragment()));
    }

    private void setView() {
        mScrimInsetsFrameLayout = (ScrimInsetsFrameLayout) findViewById(R.id.scrimInsetsFrameLayout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.color_primary_dark));
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerList = (ListView) findViewById(R.id.drawer_list);
    }

    private void setInterface() {
        mScrimInsetsFrameLayout.setLayoutParams(getDrawerParams());
        mDrawerList.setAdapter(new ListAdapter.Adapter(this, mList));
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, mDrawerLayout, toolbar, 0, 0) {
            @Override
            public void onDrawerClosed(View drawerView) {
                getSupportActionBar().setTitle(mTitle);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(getString(R.string.app_name));
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
    }

    private class Task extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            setView();
        }

        @Override
        protected String doInBackground(Void... params) {

            // Check root access and busybox installation
            if (RootUtils.rooted()) hasRoot = RootUtils.rootAccess();
            if (hasRoot) hasBusybox = RootUtils.busyboxInstalled();

            if (hasRoot && hasBusybox) {
                String[] files = {String.format(CPU_MAX_FREQ, 0), String.format(CPU_MIN_FREQ, 0),
                        String.format(CPU_SCALING_GOVERNOR, 0), LMK_MINFREE};

                RootUtils.su = new RootUtils.SU();
                for (String file : files) RootUtils.runCommand("chmod 644 " + file);
                setList();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (!hasRoot || !hasBusybox) {
                Intent i = new Intent(MainActivity.this, TextActivity.class);
                Bundle args = new Bundle();
                args.putString(TextActivity.ARG_TEXT, !hasRoot ? getString(R.string.no_root)
                        : getString(R.string.no_busybox));
                Log.d(TAG, !hasRoot ? getString(R.string.no_root) : getString(R.string.no_busybox));
                i.putExtras(args);
                startActivity(i);

                cancel(true);
                finish();
                return;
            }

            setInterface();
            ((ViewGroup) progressBar.getParent()).removeView(progressBar);

            selectItem(2);
            mScrimInsetsFrameLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title.toString();
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScrimInsetsFrameLayout.setLayoutParams(getDrawerParams());
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (!mDrawerLayout.isDrawerOpen(mScrimInsetsFrameLayout)) super.onBackPressed();
        else mDrawerLayout.closeDrawer(mScrimInsetsFrameLayout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (RootUtils.su != null) RootUtils.su.close();
        RootUtils.su = null;
    }

    private DrawerLayout.LayoutParams getDrawerParams() {
        boolean tablet = Utils.isTablet(this);

        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) mScrimInsetsFrameLayout.getLayoutParams();
        int width = getResources().getDisplayMetrics().widthPixels;

        TypedArray ta = obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int actionBarSize = ta.getDimensionPixelSize(0, 100);
        if (Utils.getScreenOrientation(this) == Configuration.ORIENTATION_LANDSCAPE) {
            params.width = width / 2;
            if (tablet) params.width -= actionBarSize + 30;
        } else {
            params.width = tablet ? width / 2 : width - actionBarSize;
        }

        return params;
    }

}
