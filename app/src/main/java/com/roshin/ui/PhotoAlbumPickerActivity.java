/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Author Nikolai Kudashov, Ivan Roshinsky.
 */

package com.roshin.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.roshin.Picca.R;
import com.roshin.gallery.AndroidUtilities;
import com.roshin.gallery.ApplicationLoader;
import com.roshin.gallery.LocaleController;
import com.roshin.gallery.MediaController;
import com.roshin.gallery.NotificationCenter;
import com.roshin.ui.ActionBar.ActionBar;
import com.roshin.ui.ActionBar.ActionBarMenu;
import com.roshin.ui.ActionBar.ActionBarMenuItem;
import com.roshin.ui.ActionBar.BaseFragment;
import com.roshin.ui.ActionBar.MenuDrawable;
import com.roshin.ui.ActionBar.SimpleTextView;
import com.roshin.ui.ActionBar.Theme;
import com.roshin.ui.Adapters.BaseFragmentAdapter;
import com.roshin.ui.Cells.PhotoPickerAlbumsCell;
import com.roshin.ui.Cells.PhotoPickerSearchCell;
import com.roshin.ui.Components.LayoutHelper;
import com.roshin.ui.Components.NumberTextView;

import java.util.ArrayList;
import java.util.HashMap;

public class PhotoAlbumPickerActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int DELETE_ALBUMS_COMMAND = 2;
    private static final int SORTING_DROPDOWN_MENU = 4;
    private static final int EXTRA_MENU = 5;
    private static final int SORT_BY_NAME_ASC = 6;
    private static final int SORT_BY_NAME_DESC = 7;
    private static final int SORT_BY_DATE_ASC = 8;
    private static final int SORT_BY_DATE_DESC = 9;
    private static final int OPEN_SETTINGS_COMMAND = 10;

    private final BaseFragment chatActivity;

    private SimpleTextView actionModeTextView;
    private SimpleTextView actionModeSubTextView;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    private NumberTextView selectedAlbumsCountTextView;

    public interface PhotoAlbumPickerActivityDelegate {
        void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions, ArrayList<MediaController.SearchImage> webPhotos);
        boolean didSelectVideo(String path);
        void startPhotoSelectActivity();
    }

    private ArrayList<MediaController.AlbumEntry> albumsSorted = null;
    private ArrayList<MediaController.AlbumEntry> videoAlbumsSorted = null;
    private HashMap<Integer, MediaController.PhotoEntry> selectedPhotos = new HashMap<>(); // TODO: 14.08.2016 move to PhotoPickerActivity
    private HashMap<Integer, MediaController.AlbumEntry> selectedAlbums = new HashMap<>();
    private HashMap<String, MediaController.SearchImage> selectedWebPhotos = new HashMap<>();
    private HashMap<String, MediaController.SearchImage> recentImagesWebKeys = new HashMap<>();
    private HashMap<String, MediaController.SearchImage> recentImagesGifKeys = new HashMap<>();
    private ArrayList<MediaController.SearchImage> recentWebImages = new ArrayList<>();
    private ArrayList<MediaController.SearchImage> recentGifImages = new ArrayList<>();
    private boolean loading = false;

    private int columnsCount = 2;
    private ListView listView;
    private ListAdapter listAdapter;
    private FrameLayout progressView;
    private TextView emptyView;
    private boolean sendPressed;
    private boolean singlePhoto;
    private boolean allowGifs;
    private int selectedMode;


    private PhotoAlbumPickerActivityDelegate delegate;

    public PhotoAlbumPickerActivity(boolean singlePhoto, boolean allowGifs, BaseFragment chatActivity) {
        super();
        classGuid = 0;
        this.chatActivity = chatActivity;
        this.singlePhoto = singlePhoto;
        this.allowGifs = allowGifs;
    }

    @Override
    public boolean onFragmentCreate() {
        loading = true;
        MediaController.loadGalleryPhotosAlbums(classGuid);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.albumsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.fileDeleted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.albumSelectedStateChange);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.albumsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.fileDeleted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.albumSelectedStateChange);
        super.onFragmentDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View createView(final Context context) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();

        actionBar.setBackgroundColor(Theme.ACTION_BAR_MEDIA_PICKER_COLOR);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        final MenuDrawable menuDrawable = new MenuDrawable();
        actionBar.setBackButtonDrawable(menuDrawable);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                switch (id){
                    case -1:
                        if (actionBar.isActionModeShowed()) {
                            selectedAlbums.clear();
                            deselectAllAlbums();
                            actionBar.hideActionMode();
                            break;
                        }
                        if (!parentLayout.getDrawerLayoutContainer().isDrawerOpened()) {
                            parentLayout.getDrawerLayoutContainer().openDrawer(false);
                        } else {
                            parentLayout.getDrawerLayoutContainer().closeDrawer(false);
                        }
                        break;
                    case DELETE_ALBUMS_COMMAND:
                        deleteSelectedAlbumsWithContent();
                        break;
                    case OPEN_SETTINGS_COMMAND:
                        presentFragment(new SettingsActivity());
                        break;
                    case SORT_BY_NAME_ASC:
                        editor.putInt("sortAlbumsBy", MediaController.SORT_BY_NAME_ASC);
                        editor.apply();
                        MediaController.sortAlbumsBy(albumsSorted, MediaController.SORT_BY_NAME_ASC);
                        listAdapter.notifyDataSetChanged();
                        break;
                    case SORT_BY_NAME_DESC:
                        editor.putInt("sortAlbumsBy", MediaController.SORT_BY_NAME_DESC);
                        editor.apply();
                        MediaController.sortAlbumsBy(albumsSorted, MediaController.SORT_BY_NAME_DESC);
                        listAdapter.notifyDataSetChanged();
                        break;
                    case SORT_BY_DATE_ASC:
                        editor.putInt("sortAlbumsBy", MediaController.SORT_BY_DATE_ASC);
                        editor.apply();
                        MediaController.sortAlbumsBy(albumsSorted, MediaController.SORT_BY_DATE_ASC);
                        listAdapter.notifyDataSetChanged();
                        break;
                    case SORT_BY_DATE_DESC:
                        editor.putInt("sortAlbumsBy", MediaController.SORT_BY_DATE_DESC);
                        editor.apply();
                        MediaController.sortAlbumsBy(albumsSorted, MediaController.SORT_BY_DATE_DESC);
                        listAdapter.notifyDataSetChanged();
                        break;
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();

        ActionBarMenuItem sortMenu = menu.addItem(SORTING_DROPDOWN_MENU, R.drawable.ic_sort_white_24dp);
        sortMenu.addSubItem(SORT_BY_NAME_ASC, LocaleController.getString("SortByNameAsc", R.string.SortByNameAsc), 0);
        sortMenu.addSubItem(SORT_BY_NAME_DESC, LocaleController.getString("SortByNameDesc", R.string.SortByNameDesc), 0);
        sortMenu.addSubItem(SORT_BY_DATE_ASC, LocaleController.getString("SortByDateAsc", R.string.SortByDateAsc), 0);
        sortMenu.addSubItem(SORT_BY_DATE_DESC, LocaleController.getString("SortByDateDesc", R.string.SortByDateDesc), 0);

        ActionBarMenuItem extendedMenu = menu.addItem(EXTRA_MENU, R.drawable.ic_ab_other);
        extendedMenu.addSubItem(OPEN_SETTINGS_COMMAND, LocaleController.getString("Settings", R.string.Settings), R.drawable.menu_settings);


        final ActionBarMenu actionMode = actionBar.createActionMode();
        selectedAlbumsCountTextView = new NumberTextView(actionMode.getContext());
        selectedAlbumsCountTextView.setTextSize(18);
        selectedAlbumsCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedAlbumsCountTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionMode.addView(selectedAlbumsCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));

        FrameLayout actionModeTitleContainer = new FrameLayout(context) {

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);

                setMeasuredDimension(width, height);

                actionModeTextView.setTextSize(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 18 : 20);
                actionModeTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.AT_MOST));

                if (actionModeSubTextView.getVisibility() != GONE) {
                    actionModeSubTextView.setTextSize(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 14 : 16);
                    actionModeSubTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.AT_MOST));
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                int height = bottom - top;

                int textTop;
                if (actionModeSubTextView.getVisibility() != GONE) {
                    textTop = (height / 2 - actionModeTextView.getTextHeight()) / 2 + AndroidUtilities.dp(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 3);
                } else {
                    textTop = (height - actionModeTextView.getTextHeight()) / 2;
                }
                actionModeTextView.layout(0, textTop, actionModeTextView.getMeasuredWidth(), textTop + actionModeTextView.getTextHeight());

                if (actionModeSubTextView.getVisibility() != GONE) {
                    textTop = height / 2 + (height / 2 - actionModeSubTextView.getTextHeight()) / 2 - AndroidUtilities.dp(!AndroidUtilities.isTablet() && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 1 : 1);
                    actionModeSubTextView.layout(0, textTop, actionModeSubTextView.getMeasuredWidth(), textTop + actionModeSubTextView.getTextHeight());
                }
            }
        };
        actionMode.addView(actionModeTitleContainer, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));
        actionModeTitleContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        actionModeTitleContainer.setVisibility(View.GONE);

        actionModeTextView = new SimpleTextView(context);
        actionModeTextView.setTextSize(18);
        actionModeTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        actionModeTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionModeTextView.setText(LocaleController.getString("Edit", R.string.Edit));
        actionModeTitleContainer.addView(actionModeTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionModeSubTextView = new SimpleTextView(context);
        actionModeSubTextView.setGravity(Gravity.LEFT);
        actionModeSubTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionModeTitleContainer.addView(actionModeSubTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        actionModeViews.add(actionMode.addItem(DELETE_ALBUMS_COMMAND, R.drawable.ic_ab_fwd_delete, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xff000000);
        actionBar.setTitle(LocaleController.getString("Albums", R.string.Albums));

        if (Build.VERSION.SDK_INT >= 19){
            getParentActivity().getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        } else if (Build.VERSION.SDK_INT >= 16) {
            actionBar.setOccupyStatusBar(true);
            getParentActivity().getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        listView = new ListView(context);
        listView.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setSelector(new ColorDrawable(0));
        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setDrawingCacheEnabled(false);
        listView.setScrollingCacheEnabled(false);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        //layoutParams.bottomMargin = AndroidUtilities.dp(48);
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        AndroidUtilities.setListViewEdgeEffectColor(listView, 0xff333333);

        emptyView = new TextView(context);
        emptyView.setTextColor(0xff808080);
        emptyView.setTextSize(20);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        emptyView.setText(LocaleController.getString("NoPhotos", R.string.NoPhotos));
        frameLayout.addView(emptyView);
        layoutParams = (FrameLayout.LayoutParams) emptyView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        //layoutParams.bottomMargin = AndroidUtilities.dp(48);
        emptyView.setLayoutParams(layoutParams);
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        progressView = new FrameLayout(context);
        progressView.setVisibility(View.GONE);
        frameLayout.addView(progressView);
        layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        //layoutParams.bottomMargin = AndroidUtilities.dp(48);
        progressView.setLayoutParams(layoutParams);

        ProgressBar progressBar = new ProgressBar(context);
        progressView.addView(progressBar);
        layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
        layoutParams.width = LayoutHelper.WRAP_CONTENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER;
        progressView.setLayoutParams(layoutParams);

        if (loading && (albumsSorted == null || albumsSorted != null && albumsSorted.isEmpty())) {
            progressView.setVisibility(View.VISIBLE);
            listView.setEmptyView(null);
        } else {
            progressView.setVisibility(View.GONE);
            listView.setEmptyView(emptyView);
        }

        return fragmentView;
    }

    private void deleteSelectedAlbumsWithContent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("DeleteQuestion", R.string.DeleteQuestion));
        builder.setMessage(LocaleController.getString("AreYouSureDeleteAlbum", R.string.AreYouSureDeleteAlbum));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (int j = 0; j < listView.getCount();j++) {
                    ((PhotoPickerAlbumsCell)listView.getChildAt(j)).deleteSelectedAlbumsWithContent();
                }
                deselectAllAlbums();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar.isActionModeShowed()) {
            actionBar.hideActionMode();
            selectedAlbums.clear();
            deselectAllAlbums();
            return false;
        }
        return true;
    }

    private void deselectAllAlbums() {
        for (int i = 0; i < listView.getCount(); i++) {
            PhotoPickerAlbumsCell cell = (PhotoPickerAlbumsCell)listView.getChildAt(i);
            if (cell != null) cell.deselectAllAlbums();
        }
        if (actionBar.isActionModeShowed()){
            actionBar.hideActionMode();
        }
        selectedAlbums.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        fixLayout();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.albumsDidLoaded) {
            int guid = (Integer) args[0];
            if (classGuid == guid) {
                albumsSorted = (ArrayList<MediaController.AlbumEntry>) args[1];
                videoAlbumsSorted = (ArrayList<MediaController.AlbumEntry>) args[3];
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
                if (listView != null && listView.getEmptyView() == null) {
                    listView.setEmptyView(emptyView);
                }
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
                loading = false;
            }
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        } else if (id == NotificationCenter.recentImagesDidLoaded) {
            int type = (Integer) args[0];
            if (type == 0) {
                recentWebImages = (ArrayList<MediaController.SearchImage>) args[1];
                recentImagesWebKeys.clear();
                for (MediaController.SearchImage searchImage : recentWebImages) {
                    recentImagesWebKeys.put(searchImage.id, searchImage);
                }
            } else if (type == 1) {
                recentGifImages = (ArrayList<MediaController.SearchImage>) args[1];
                recentImagesGifKeys.clear();
                for (MediaController.SearchImage searchImage : recentGifImages) {
                    recentImagesGifKeys.put(searchImage.id, searchImage);
                }
            }
        } else if (id == NotificationCenter.albumSelectedStateChange){
            MediaController.AlbumEntry albumEntry = (MediaController.AlbumEntry) args[0];
            boolean isChecked = (boolean) args[1];

            if (isChecked){
                if (!selectedAlbums.containsKey(albumEntry.bucketId))
                    selectedAlbums.put(albumEntry.bucketId, albumEntry);

                if (!actionBar.isActionModeShowed()) {
                    actionBar.showActionMode();
                }
            } else {
                if (selectedAlbums.containsKey(albumEntry.bucketId))
                    selectedAlbums.remove(albumEntry.bucketId);

                if (actionBar.isActionModeShowed() && selectedAlbums.isEmpty()) {
                    actionBar.hideActionMode();
                }
            }
            updateActionModeTitle();

        } else if (id == NotificationCenter.fileDeleted){
            if (actionBar.isActionModeShowed() && selectedAlbums.isEmpty()){
                actionBar.hideActionMode();
            }
        }
    }

    private void updateActionModeTitle() {
        if (!actionBar.isActionModeShowed()) {
            return;
        }
        if (!selectedAlbums.isEmpty()) {
            selectedAlbumsCountTextView.setNumber(selectedAlbums.size(), true);
        }
    }

    public void setDelegate(PhotoAlbumPickerActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private void sendSelectedPhotos() {
        if (selectedPhotos.isEmpty() && selectedWebPhotos.isEmpty() || delegate == null || sendPressed) {
            return;
        }
        sendPressed = true;
        ArrayList<String> photos = new ArrayList<>();
        ArrayList<String> captions = new ArrayList<>();
        for (HashMap.Entry<Integer, MediaController.PhotoEntry> entry : selectedPhotos.entrySet()) {
            MediaController.PhotoEntry photoEntry = entry.getValue();
            if (photoEntry.imagePath != null) {
                photos.add(photoEntry.imagePath);
                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
            } else if (photoEntry.path != null) {
                photos.add(photoEntry.path);
                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
            }
        }
        ArrayList<MediaController.SearchImage> webPhotos = new ArrayList<>();
        boolean gifChanged = false;
        boolean webChange = false;
        for (HashMap.Entry<String, MediaController.SearchImage> entry : selectedWebPhotos.entrySet()) {
            MediaController.SearchImage searchImage = entry.getValue();
            if (searchImage.imagePath != null) {
                photos.add(searchImage.imagePath);
                captions.add(searchImage.caption != null ? searchImage.caption.toString() : null);
            } else {
                webPhotos.add(searchImage);
            }
            searchImage.date = (int) (System.currentTimeMillis() / 1000);

            if (searchImage.type == 0) {
                webChange = true;
                MediaController.SearchImage recentImage = recentImagesWebKeys.get(searchImage.id);
                if (recentImage != null) {
                    recentWebImages.remove(recentImage);
                    recentWebImages.add(0, recentImage);
                } else {
                    recentWebImages.add(0, searchImage);
                }
            } else if (searchImage.type == 1) {
                gifChanged = true;
                MediaController.SearchImage recentImage = recentImagesGifKeys.get(searchImage.id);
                if (recentImage != null) {
                    recentGifImages.remove(recentImage);
                    recentGifImages.add(0, recentImage);
                } else {
                    recentGifImages.add(0, searchImage);
                }
            }
        }

        delegate.didSelectPhotos(photos, captions, webPhotos);
    }

    private void fixLayout() {
        AndroidUtilities.checkDisplaySize();
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    fixLayoutInternal();
                    if (listView != null) {
                        listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
        }
    }

    private void fixLayoutInternal() {
        if (getParentActivity() == null) {
            return;
        }

        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();
        columnsCount = 2;
        if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
            columnsCount = 4;
        }
        listAdapter.notifyDataSetChanged();
    }

    private void openPhotoPicker(MediaController.AlbumEntry albumEntry, int type) {
        ArrayList<MediaController.SearchImage> recentImages = null;
        if (albumEntry == null) {
            if (type == 0) {
                recentImages = recentWebImages;
            } else if (type == 1) {
                recentImages = recentGifImages;
            }
        }
        PhotoPickerActivity fragment = new PhotoPickerActivity(type, albumEntry, selectedPhotos, selectedWebPhotos, recentImages, singlePhoto, chatActivity);
        fragment.setDelegate(new PhotoPickerActivity.PhotoPickerActivityDelegate() {
            @Override
            public void selectedPhotosChanged() {
            }

            @Override
            public void actionButtonPressed(boolean canceled) {
                removeSelfFromStack();
                if (!canceled) {
                    sendSelectedPhotos();
                }
            }

            @Override
            public boolean didSelectVideo(String path) {
                removeSelfFromStack();
                return delegate.didSelectVideo(path);
            }
        });
        presentFragment(fragment);
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            if (singlePhoto || selectedMode == 0) {
                if (singlePhoto) {
                    return albumsSorted != null ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0;
                }
                return 1 + (albumsSorted != null ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0);
            } else {
                return (videoAlbumsSorted != null ? (int) Math.ceil(videoAlbumsSorted.size() / (float) columnsCount) : 0);
            }
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == 0) {
                PhotoPickerAlbumsCell photoPickerAlbumsCell;
                if (view == null) {
                    view = new PhotoPickerAlbumsCell(mContext);
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                    photoPickerAlbumsCell.setDelegate(new PhotoPickerAlbumsCell.PhotoPickerAlbumsCellDelegate() {
                        @Override
                        public void didSelectAlbum(MediaController.AlbumEntry albumEntry, PhotoPickerAlbumsCell.AlbumView albumView) {
                            if (!selectedAlbums.isEmpty()){
                                albumView.reverseChecked();
                                if (albumView.checkBox.isChecked()){
                                    if (!selectedAlbums.containsKey(albumEntry.bucketId))
                                        selectedAlbums.put(albumEntry.bucketId, albumEntry);

                                    if (!actionBar.isActionModeShowed()) {
                                        actionBar.showActionMode();
                                    }
                                } else {
                                    if (selectedAlbums.containsKey(albumEntry.bucketId))
                                        selectedAlbums.remove(albumEntry.bucketId);

                                    if (actionBar.isActionModeShowed() && selectedAlbums.isEmpty()) {
                                        actionBar.hideActionMode();
                                    }
                                }
                                updateActionModeTitle();
                            } else openPhotoPicker(albumEntry, 0);
                        }
                    });
                } else {
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                }
                photoPickerAlbumsCell.setAlbumsCount(columnsCount);
                for (int a = 0; a < columnsCount; a++) {
                    int index;
                    if (singlePhoto || selectedMode == 1) {
                        index = i * columnsCount + a;
                    } else {
                        index = (i - 1) * columnsCount + a;
                    }
                    if (singlePhoto || selectedMode == 0) {
                        if (index < albumsSorted.size()) {
                            MediaController.AlbumEntry albumEntry = albumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    } else {
                        if (index < videoAlbumsSorted.size()) {
                            MediaController.AlbumEntry albumEntry = videoAlbumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    }
                }
                photoPickerAlbumsCell.requestLayout();
            } else if (type == 1) {
                if (view == null) {
                    view = new PhotoPickerSearchCell(mContext, allowGifs);
                    ((PhotoPickerSearchCell) view).setDelegate(new PhotoPickerSearchCell.PhotoPickerSearchCellDelegate() {
                        @Override
                        public void didPressedSearchButton(int index) {
                            openPhotoPicker(null, index);
                        }
                    });
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (singlePhoto || selectedMode == 1) {
                return 0;
            }
            if (i == 0) {
                return 1;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            if (singlePhoto || selectedMode == 1) {
                return 1;
            }
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }
    }
}
