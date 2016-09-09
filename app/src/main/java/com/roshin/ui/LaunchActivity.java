/*
 * This is the source code of Picca for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Author Nikolai Kudashov, Ivan Roshinsky.
 */

package com.roshin.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.roshin.Picca.R;
import com.roshin.gallery.AndroidUtilities;
import com.roshin.gallery.ApplicationLoader;
import com.roshin.gallery.FileLog;
import com.roshin.gallery.NativeCrashManager;
import com.roshin.gallery.NotificationCenter;
import com.roshin.gallery.UserConfig;
import com.roshin.ui.ActionBar.ActionBarLayout;
import com.roshin.ui.ActionBar.BaseFragment;
import com.roshin.ui.ActionBar.DrawerLayoutContainer;
import com.roshin.ui.Adapters.DrawerLayoutAdapter;
import com.roshin.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class LaunchActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate, NotificationCenter.NotificationCenterDelegate {

    private static final int PERMISSION_REQUEST_CODE = 0;
    private boolean finished;
    private static ArrayList<BaseFragment> mainFragmentsStack = new ArrayList<>();
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;

    private ActionBarLayout actionBarLayout;
    protected DrawerLayoutContainer drawerLayoutContainer;
    private DrawerLayoutAdapter drawerLayoutAdapter;
    private AlertDialog visibleDialog;

    private Runnable lockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ApplicationLoader.postInitApplication();
        NativeCrashManager.handleDumpFiles(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.Theme_Picca);
        getWindow().setBackgroundDrawableResource(R.drawable.transparent);

        super.onCreate(savedInstanceState);

        AndroidUtilities.getSystemUISizes(this);

        actionBarLayout = new ActionBarLayout(this);
        drawerLayoutContainer = new DrawerLayoutContainer(this);
        setContentView(drawerLayoutContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        drawerLayoutContainer.addView(actionBarLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ListView listView = new ListView(this) {
            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }
        };
        listView.setBackgroundColor(0xffffffff);
        listView.setAdapter(drawerLayoutAdapter = new DrawerLayoutAdapter(this));
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        drawerLayoutContainer.setDrawerLayout(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        Point screenSize = AndroidUtilities.getRealScreenSize();
        layoutParams.width = Math.min(AndroidUtilities.dp(320), Math.min(screenSize.x, screenSize.y) - AndroidUtilities.dp(56));
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        listView.setLayoutParams(layoutParams);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 2) {
                    presentFragment(new SettingsActivity());
                    drawerLayoutContainer.closeDrawer(false);
                } else if (position == 3) {
                    drawerLayoutContainer.closeDrawer(false);
                }
            }
        });

        drawerLayoutContainer.setParentActionBarLayout(actionBarLayout);
        drawerLayoutContainer.setAllowOpenDrawer(true, false);

        actionBarLayout.setDrawerLayoutContainer(drawerLayoutContainer);
        actionBarLayout.init(mainFragmentsStack);
        actionBarLayout.setDelegate(this);

        final View view = getWindow().getDecorView().getRootView();
        view.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = view.getMeasuredHeight();
                if (height > AndroidUtilities.dp(100) && height < AndroidUtilities.displaySize.y && height + AndroidUtilities.dp(100) > AndroidUtilities.displaySize.y) {
                    AndroidUtilities.displaySize.y = height;
                    FileLog.e("tmessages", "fix display size y to " + AndroidUtilities.displaySize.y);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            openAlbums();
        }
    }

    public void openAlbums() {
        if (actionBarLayout.fragmentsStack.isEmpty()){
            PhotoAlbumPickerActivity albumsActivity = new PhotoAlbumPickerActivity(true, false, null);
            actionBarLayout.addFragmentToStack(albumsActivity);
        }

        actionBarLayout.showLastFragment();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbums();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onFinish() {
        if (finished) {
            return;
        }
        finished = true;
        if (lockRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(lockRunnable);
            lockRunnable = null;
        }

        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeOtherAppActivities);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.needShowAlert);
    }

    public void presentFragment(BaseFragment fragment) {
        actionBarLayout.presentFragment(fragment);
    }

    public void fixLayout() {
        if (actionBarLayout == null) {
            return;
        }
        actionBarLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (actionBarLayout != null) {
                    if (Build.VERSION.SDK_INT < 16) {
                        actionBarLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        actionBarLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
        actionBarLayout.rebuildAllFragmentViews(false);
    }

    @Override
    protected void onDestroy() {
        PhotoViewer.getInstance().destroyPhotoViewer();
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e("picca", e);
        }
        try {
            if (onGlobalLayoutListener != null) {
                final View view = getWindow().getDecorView().getRootView();
                if (Build.VERSION.SDK_INT < 16) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
                } else {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
                }
            }
        } catch (Exception e) {
            FileLog.e("picca", e);
        }
        super.onDestroy();
        onFinish();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        AndroidUtilities.checkDisplaySize();
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.closeOtherAppActivities) {
            if (args[0] != this) {
                onFinish();
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
        } else if (drawerLayoutContainer.isDrawerOpened()) {
            drawerLayoutContainer.closeDrawer(false);
        } else {
            actionBarLayout.onBackPressed();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        actionBarLayout.onLowMemory();
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        if (Build.VERSION.SDK_INT >= 23 && mode.getType() == ActionMode.TYPE_FLOATING) {
            return;
        }
        actionBarLayout.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        if (Build.VERSION.SDK_INT >= 23 && mode.getType() == ActionMode.TYPE_FLOATING) {
            return;
        }
        actionBarLayout.onActionModeFinished(mode);
    }

    @Override
    public boolean onPreIme() {
        if (PhotoViewer.getInstance().isVisible()) {
            PhotoViewer.getInstance().closePhoto(true, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && !UserConfig.isWaitingForPasscodeEnter) {
            if (actionBarLayout.fragmentsStack.size() == 1) {
                if (!drawerLayoutContainer.isDrawerOpened()) {
                    if (getCurrentFocus() != null) {
                        AndroidUtilities.hideKeyboard(getCurrentFocus());
                    }
                    drawerLayoutContainer.openDrawer(false);
                } else {
                    drawerLayoutContainer.closeDrawer(false);
                }
            } else {
                actionBarLayout.onKeyUp(keyCode, event);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean needPresentFragment(BaseFragment fragment, boolean removeLast, boolean forceWithoutAnimation, ActionBarLayout layout) {
        drawerLayoutContainer.setAllowOpenDrawer(true, false);
        return true;
    }

    @Override
    public boolean needAddFragmentToStack(BaseFragment fragment, ActionBarLayout layout) {
        drawerLayoutContainer.setAllowOpenDrawer(true, false);
        return true;
    }

    @Override
    public boolean needCloseLastFragment(ActionBarLayout layout) {
        if (layout.fragmentsStack.size() <= 1) {
            onFinish();
            finish();
            return false;
        }
        return true;
    }

    @Override
    public void onRebuildAllFragments(ActionBarLayout layout) {
        drawerLayoutAdapter.notifyDataSetChanged();
    }
}
