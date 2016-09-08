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
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.roshin.Picca.R;
import com.roshin.gallery.AndroidUtilities;
import com.roshin.gallery.ApplicationLoader;
import com.roshin.gallery.FileLoader;
import com.roshin.gallery.FileLog;
import com.roshin.gallery.LocaleController;
import com.roshin.gallery.MediaController;
import com.roshin.gallery.NotificationCenter;
import com.roshin.tgnet.ConnectionsManager;
import com.roshin.tgnet.RequestDelegate;
import com.roshin.tgnet.TLObject;
import com.roshin.tgnet.TLRPC;
import com.roshin.ui.ActionBar.ActionBar;
import com.roshin.ui.ActionBar.ActionBarMenu;
import com.roshin.ui.ActionBar.ActionBarMenuItem;
import com.roshin.ui.ActionBar.BackDrawable;
import com.roshin.ui.ActionBar.BaseFragment;
import com.roshin.ui.ActionBar.SimpleTextView;
import com.roshin.ui.ActionBar.Theme;
import com.roshin.ui.Adapters.BaseFragmentAdapter;
import com.roshin.ui.Cells.PhotoPickerPhotoCell;
import com.roshin.ui.Components.BackupImageView;
import com.roshin.ui.Components.LayoutHelper;
import com.roshin.ui.Components.NumberTextView;
import com.roshin.ui.Components.PickerBottomLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoPickerActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, PhotoViewer.PhotoViewerProvider {

    public interface PhotoPickerActivityDelegate {
        void selectedPhotosChanged();

        void actionButtonPressed(boolean canceled);

        boolean didSelectVideo(String path);
    }

    private SimpleTextView actionModeTextView;
    private SimpleTextView actionModeSubTextView;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    private NumberTextView selectedPhotosCountTextView;

    private final static int BACK_BUTTON = -1;
    private final static int DELETE_FILES_BUTTON = 0;

    private static final ExecutorService DELETE_SERVICE = Executors.newSingleThreadExecutor();

    //private RequestQueue requestQueue;

    private int type;
    private HashMap<String, MediaController.SearchImage> selectedWebPhotos;
    private HashMap<Integer, MediaController.PhotoEntry> selectedPhotos;
    private ArrayList<MediaController.SearchImage> recentImages;

    private ArrayList<MediaController.SearchImage> searchResult = new ArrayList<>();

    private boolean searching;
    private String nextSearchBingString;
    private boolean giphySearchEndReached = true;
    private String lastSearchString;
    private boolean loadingRecent;
    private int nextGiphySearchOffset;
    private int giphyReqId;
    private int lastSearchToken;

    private MediaController.AlbumEntry selectedAlbum;

    private GridView listView;

    private ListAdapter listAdapter;
    private PickerBottomLayout pickerBottomLayout;
    private FrameLayout progressView;
    private TextView emptyView;
    private ActionBarMenuItem searchItem;
    private int itemWidth = 100;
    private boolean sendPressed;
    private boolean singlePhoto;
    private BaseFragment chatActivity;

    private PhotoPickerActivityDelegate delegate;

    public PhotoPickerActivity(int type, MediaController.AlbumEntry selectedAlbum, HashMap<Integer, MediaController.PhotoEntry> selectedPhotos, HashMap<String, MediaController.SearchImage> selectedWebPhotos, ArrayList<MediaController.SearchImage> recentImages, boolean onlyOnePhoto, BaseFragment chatActivity) {
        super();
        this.selectedAlbum = selectedAlbum;
        this.selectedPhotos = selectedPhotos;
        this.selectedWebPhotos = selectedWebPhotos;
        this.type = type;
        this.recentImages = recentImages;
        this.singlePhoto = onlyOnePhoto;
        this.chatActivity = chatActivity;
        if (selectedAlbum != null && selectedAlbum.isVideo) {
            singlePhoto = true;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recentImagesDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.fileDeleted);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recentImagesDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.fileDeleted);
        selectedPhotos.clear();
        super.onFragmentDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View createView(Context context) {
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

        actionBar.setBackgroundColor(Theme.ACTION_BAR_MEDIA_PICKER_COLOR);
        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        actionBar.setBackButtonDrawable(new BackDrawable(false));

        if (selectedAlbum != null) {
            actionBar.setTitle(selectedAlbum.bucketName);
        }

        actionBar.createMenu();

        final ActionBarMenu actionMode = actionBar.createActionMode();
        selectedPhotosCountTextView = new NumberTextView(actionMode.getContext());
        selectedPhotosCountTextView.setTextSize(18);
        selectedPhotosCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedPhotosCountTextView.setTextColor(Theme.ACTION_BAR_ACTION_MODE_TEXT_COLOR);
        actionMode.addView(selectedPhotosCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));

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

        actionModeViews.add(actionMode.addItem(DELETE_FILES_BUTTON, R.drawable.ic_ab_fwd_delete, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                switch (id) {
                    case BACK_BUTTON:
                        if (!selectedPhotos.isEmpty()){
                            deselectAllPhotos();
                        } else {
                            finishFragment();
                        }
                        break;
                    case DELETE_FILES_BUTTON:
                        deleteCheckedFiles();
                        break;
                }
            }
        });

        fragmentView = new FrameLayout(context);

        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xff000000);

        listView = new GridView(context);
        listView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setDrawSelectorOnTop(true);
        listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(true);
        listView.setNumColumns(GridView.AUTO_FIT);
        listView.setVerticalSpacing(AndroidUtilities.dp(4));
        listView.setHorizontalSpacing(AndroidUtilities.dp(4));
        listView.setSelector(R.drawable.list_selector);
        frameLayout.addView(listView);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        //layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
        listView.setLayoutParams(layoutParams);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        AndroidUtilities.setListViewEdgeEffectColor(listView, 0xff333333);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (selectedPhotos.size() > 0) {
                    setPhotoChecked(i);
                    return;
                }

                if (selectedAlbum != null && selectedAlbum.isVideo) {
                    if (i < 0 || i >= selectedAlbum.photos.size()) {
                        return;
                    }
                    if (delegate.didSelectVideo(selectedAlbum.photos.get(i).path)) {
                        finishFragment();
                    }
                } else {
                    ArrayList<Object> arrayList;
                    if (selectedAlbum != null) {
                        arrayList = (ArrayList) selectedAlbum.photos;
                    } else {
                        if (searchResult.isEmpty() && lastSearchString == null) {
                            arrayList = (ArrayList) recentImages;
                        } else {
                            arrayList = (ArrayList) searchResult;
                        }
                    }
                    if (i < 0 || i >= arrayList.size()) {
                        return;
                    }
                    if (searchItem != null) {
                        AndroidUtilities.hideKeyboard(searchItem.getSearchField());
                    }
                    PhotoViewer.getInstance().setParentActivity(getParentActivity());
                    PhotoViewer.getInstance().openPhotoForSelect(arrayList, i, singlePhoto ? 1 : 0, PhotoPickerActivity.this, chatActivity);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                setPhotoChecked(position);
                return true;
            }
        });

        if (selectedAlbum == null) {
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("ClearSearch", R.string.ClearSearch));
                        builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                recentImages.clear();
                                if (listAdapter != null) {
                                    listAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                        return true;
                    }
                    return false;
                }
            });
        }

        emptyView = new TextView(context);
        emptyView.setTextColor(0xff808080);
        emptyView.setTextSize(20);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        if (selectedAlbum != null) {
            emptyView.setText(LocaleController.getString("NoPhotos", R.string.NoPhotos));
        } else {
            if (type == 0) {
                emptyView.setText(LocaleController.getString("NoRecentPhotos", R.string.NoRecentPhotos));
            } else if (type == 1) {
                emptyView.setText(LocaleController.getString("NoRecentGIFs", R.string.NoRecentGIFs));
            }
        }
        frameLayout.addView(emptyView);
        layoutParams = (FrameLayout.LayoutParams) emptyView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        //layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
        emptyView.setLayoutParams(layoutParams);
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        if (selectedAlbum == null) {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_TOUCH_SCROLL) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                }
            });

            progressView = new FrameLayout(context);
            progressView.setVisibility(View.GONE);
            frameLayout.addView(progressView);
            layoutParams = (FrameLayout.LayoutParams) progressView.getLayoutParams();
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.height = LayoutHelper.MATCH_PARENT;
            layoutParams.bottomMargin = singlePhoto ? 0 : AndroidUtilities.dp(48);
            progressView.setLayoutParams(layoutParams);

            ProgressBar progressBar = new ProgressBar(context);
            progressView.addView(progressBar);
            layoutParams = (FrameLayout.LayoutParams) progressBar.getLayoutParams();
            layoutParams.width = LayoutHelper.WRAP_CONTENT;
            layoutParams.height = LayoutHelper.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            progressBar.setLayoutParams(layoutParams);

            updateSearchInterface();
        }

        pickerBottomLayout = new PickerBottomLayout(context);
        frameLayout.addView(pickerBottomLayout);
        layoutParams = (FrameLayout.LayoutParams) pickerBottomLayout.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.gravity = Gravity.BOTTOM;
        pickerBottomLayout.setLayoutParams(layoutParams);
        pickerBottomLayout.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delegate.actionButtonPressed(true);
                finishFragment();
            }
        });
        pickerBottomLayout.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectedPhotos();
            }
        });
        if (singlePhoto) {
            pickerBottomLayout.setVisibility(View.GONE);
        }

        listView.setEmptyView(emptyView);
        pickerBottomLayout.updateSelectedCount(selectedPhotos.size() + selectedWebPhotos.size(), true);

        return fragmentView;
    }

    private void deselectAllPhotos() {
        for (int i = 0; i < listView.getCount(); i++) {
            if (isPhotoChecked(i)){
                setPhotoChecked(i);
            }
        }
    }

    private void updateActionModeTitle() {
        if (!actionBar.isActionModeShowed()) {
            return;
        }
        if (!selectedPhotos.isEmpty()) {
            selectedPhotosCountTextView.setNumber(selectedPhotos.size(), true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        if (searchItem != null) {
            searchItem.openSearch(true);
            getParentActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
        fixLayout();
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar.isActionModeShowed()){
            deselectAllPhotos();
            return false;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayout();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        } else if (id == NotificationCenter.recentImagesDidLoaded) {
            if (selectedAlbum == null && type == (Integer) args[0]) {
                recentImages = (ArrayList<MediaController.SearchImage>) args[1];
                loadingRecent = false;
                updateSearchInterface();
            }
        } else if (id == NotificationCenter.fileDeleted) {
            MediaController.PhotoEntry deletedPhotoEntry = (MediaController.PhotoEntry) args[0];

            Iterator iterator = selectedPhotos.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry selectedPhotoEntry = (Map.Entry) iterator.next();
                if (deletedPhotoEntry == selectedPhotoEntry.getValue())
                    iterator.remove();
            }

            selectedAlbum.photos.remove(deletedPhotoEntry);
            listAdapter.notifyDataSetChanged();

            if (actionBar.isActionModeShowed() && selectedAlbum.photos.isEmpty()){
                actionBar.hideActionMode();
            }
        }
    }

    private PhotoPickerPhotoCell getCellForIndex(int index) {
        int count = listView.getChildCount();

        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view instanceof PhotoPickerPhotoCell) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                int num = (Integer) cell.photoImage.getTag();
                if (selectedAlbum != null) {
                    if (num < 0 || num >= selectedAlbum.photos.size()) {
                        continue;
                    }
                } else {
                    ArrayList<MediaController.SearchImage> array;
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        array = recentImages;
                    } else {
                        array = searchResult;
                    }
                    if (num < 0 || num >= array.size()) {
                        continue;
                    }
                }
                if (num == index) {
                    return cell;
                }
            }
        }
        return null;
    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(TLRPC.FileLocation fileLocation, int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            int coords[] = new int[2];
            cell.photoImage.getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1];
            object.parentView = listView;
            object.imageReceiver = cell.photoImage.getImageReceiver();
            object.thumb = object.imageReceiver.getBitmap();
            object.scale = cell.photoImage.getScaleX();
            cell.checkBox.setVisibility(View.GONE);
            return object;
        }
        return null;
    }

    @Override
    public void updatePhotoAtIndex(int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            if (selectedAlbum != null) {
                cell.photoImage.setOrientation(0, true);
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.path != null) {
                    cell.photoImage.setOrientation(photoEntry.orientation, true);
                    if (photoEntry.isVideo) {
                        cell.photoImage.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    } else {
                        cell.photoImage.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    }
                } else {
                    cell.photoImage.setImageResource(R.drawable.nophotos);
                }
            } else {
                ArrayList<MediaController.SearchImage> array;
                if (searchResult.isEmpty() && lastSearchString == null) {
                    array = recentImages;
                } else {
                    array = searchResult;
                }
                MediaController.SearchImage photoEntry = array.get(index);
                if (photoEntry.document != null && photoEntry.document.thumb != null) {
                    cell.photoImage.setImage(photoEntry.document.thumb.location, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.thumbUrl != null && photoEntry.thumbUrl.length() > 0) {
                    cell.photoImage.setImage(photoEntry.thumbUrl, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else {
                    cell.photoImage.setImageResource(R.drawable.nophotos);
                }
            }
        }
    }

    @Override
    public Bitmap getThumbForPhoto(TLRPC.FileLocation fileLocation, int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            return cell.photoImage.getImageReceiver().getBitmap();
        }
        return null;
    }

    @Override
    public void willSwitchFromPhoto(TLRPC.FileLocation fileLocation, int index) {
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view.getTag() == null) {
                continue;
            }
            PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
            int num = (Integer) view.getTag();
            if (selectedAlbum != null) {
                if (num < 0 || num >= selectedAlbum.photos.size()) {
                    continue;
                }
            } else {
                ArrayList<MediaController.SearchImage> array;
                if (searchResult.isEmpty() && lastSearchString == null) {
                    array = recentImages;
                } else {
                    array = searchResult;
                }
                if (num < 0 || num >= array.size()) {
                    continue;
                }
            }
        }
    }

    @Override
    public void willHidePhotoViewer() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean isPhotoChecked(int index) {
        if (selectedAlbum != null) {
            return !(index < 0 || index >= selectedAlbum.photos.size()) && selectedPhotos.containsKey(selectedAlbum.photos.get(index).imageId);
        } else {
            ArrayList<MediaController.SearchImage> array;
            if (searchResult.isEmpty() && lastSearchString == null) {
                array = recentImages;
            } else {
                array = searchResult;
            }
            return !(index < 0 || index >= array.size()) && selectedWebPhotos.containsKey(array.get(index).id);
        }
    }

    @Override
    public void setPhotoChecked(int index) {
        boolean add = true;
        if (selectedAlbum != null) {
            if (index < 0 || index >= selectedAlbum.photos.size()) {
                return;
            }
            MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
            if (selectedPhotos.containsKey(photoEntry.imageId)) {
                selectedPhotos.remove(photoEntry.imageId);
                add = false;
                if (selectedPhotos.isEmpty())
                    actionBar.hideActionMode();
            } else {
                selectedPhotos.put(photoEntry.imageId, photoEntry);
                if (!actionBar.isActionModeShowed())
                    actionBar.showActionMode();
            }
        } else {
            MediaController.SearchImage photoEntry;
            ArrayList<MediaController.SearchImage> array;
            if (searchResult.isEmpty() && lastSearchString == null) {
                array = recentImages;
            } else {
                array = searchResult;
            }
            if (index < 0 || index >= array.size()) {
                return;
            }
            photoEntry = array.get(index);
            if (selectedWebPhotos.containsKey(photoEntry.id)) {
                selectedWebPhotos.remove(photoEntry.id);
                add = false;
            } else {
                selectedWebPhotos.put(photoEntry.id, photoEntry);
            }
        }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            int num = (Integer) view.getTag();
            if (num == index) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                cell.setChecked(add, true);
                cell.checkBox.setVisibility(cell.checkBox.isChecked() ? View.VISIBLE : View.GONE);
                break;
            }
        }
        updateActionModeTitle();
        //pickerBottomLayout.updateSelectedCount(selectedPhotos.size() + selectedWebPhotos.size(), true);
        delegate.selectedPhotosChanged();
    }

    @Override
    public boolean cancelButtonPressed() {
        delegate.actionButtonPressed(true);
        finishFragment();
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
        if (selectedAlbum != null) {
            if (selectedPhotos.isEmpty()) {
                if (index < 0 || index >= selectedAlbum.photos.size()) {
                    return;
                }
                MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                selectedPhotos.put(photoEntry.imageId, photoEntry);
            }
        } else if (selectedPhotos.isEmpty()) {
            ArrayList<MediaController.SearchImage> array;
            if (searchResult.isEmpty() && lastSearchString == null) {
                array = recentImages;
            } else {
                array = searchResult;
            }
            if (index < 0 || index >= array.size()) {
                return;
            }
            MediaController.SearchImage photoEntry = array.get(index);
            selectedWebPhotos.put(photoEntry.id, photoEntry);
        }
        sendSelectedPhotos();
    }

    @Override
    public int getSelectedCount() {
        return selectedPhotos.size() + selectedWebPhotos.size();
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && searchItem != null) {
            AndroidUtilities.showKeyboard(searchItem.getSearchField());
        }
    }

    private void updateSearchInterface() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        if (searching && searchResult.isEmpty() || loadingRecent && lastSearchString == null) {
            progressView.setVisibility(View.VISIBLE);
            listView.setEmptyView(null);
            emptyView.setVisibility(View.GONE);
        } else {
            progressView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            listView.setEmptyView(emptyView);
        }
    }

    public void setDelegate(PhotoPickerActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private void sendSelectedPhotos() {
        if (selectedPhotos.isEmpty() && selectedWebPhotos.isEmpty() || delegate == null || sendPressed) {
            return;
        }
        sendPressed = true;
        delegate.actionButtonPressed(false);
        finishFragment();
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
        int position = listView.getFirstVisiblePosition();
        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();

        int columnsCount;
        if (AndroidUtilities.isTablet()) {
            columnsCount = 3;
        } else {
            if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                columnsCount = 5;
            } else {
                columnsCount = 3;
            }
        }
        listView.setNumColumns(columnsCount);
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - ((columnsCount + 1) * AndroidUtilities.dp(4))) / columnsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x - ((columnsCount + 1) * AndroidUtilities.dp(4))) / columnsCount;
        }
        listView.setColumnWidth(itemWidth);

        listAdapter.notifyDataSetChanged();
        listView.setSelection(position);

        if (selectedAlbum == null) {
            emptyView.setPadding(0, 0, 0, (int) ((AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight()) * 0.4f));
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return selectedAlbum != null;
        }

        @Override
        public boolean isEnabled(int i) {
            if (selectedAlbum == null) {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return i < recentImages.size();
                } else {
                    return i < searchResult.size();
                }
            }
            return true;
        }

        @Override
        public int getCount() {
            if (selectedAlbum == null) {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return recentImages.size();
                } else if (type == 0) {
                    return searchResult.size() + (nextSearchBingString == null ? 0 : 1);
                } else if (type == 1) {
                    return searchResult.size() + (giphySearchEndReached ? 0 : 1);
                }
            }
            return selectedAlbum.photos.size();
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
            int viewType = getItemViewType(i);
            if (viewType == 0) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                if (view == null) {
                    view = new PhotoPickerPhotoCell(mContext);
                    cell = (PhotoPickerPhotoCell) view;
                    cell.checkFrame.setVisibility(View.GONE);
                }
                cell.itemWidth = itemWidth;
                BackupImageView imageView = ((PhotoPickerPhotoCell) view).photoImage;
                imageView.setTag(i);
                view.setTag(i);
                boolean showing;
                imageView.setOrientation(0, true);

                if (selectedAlbum != null) {
                    MediaController.PhotoEntry photoEntry = selectedAlbum.photos.get(i);
                    if (photoEntry.thumbPath != null) {
                        imageView.setImage(photoEntry.thumbPath, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.path != null) {
                        imageView.setOrientation(photoEntry.orientation, true);
                        if (photoEntry.isVideo) {
                            imageView.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                        } else {
                            imageView.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                        }
                    } else {
                        imageView.setImageResource(R.drawable.nophotos);
                    }
                    cell.setChecked(selectedPhotos.containsKey(photoEntry.imageId), false);
                    showing = PhotoViewer.getInstance().isShowingImage(photoEntry.path);
                } else {
                    MediaController.SearchImage photoEntry;
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        photoEntry = recentImages.get(i);
                    } else {
                        photoEntry = searchResult.get(i);
                    }
                    if (photoEntry.thumbPath != null) {
                        imageView.setImage(photoEntry.thumbPath, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.thumbUrl != null && photoEntry.thumbUrl.length() > 0) {
                        imageView.setImage(photoEntry.thumbUrl, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.document != null && photoEntry.document.thumb != null) {
                        imageView.setImage(photoEntry.document.thumb.location, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else {
                        imageView.setImageResource(R.drawable.nophotos);
                    }
                    cell.setChecked(selectedWebPhotos.containsKey(photoEntry.id), false);
                    if (photoEntry.document != null) {
                        showing = PhotoViewer.getInstance().isShowingImage(FileLoader.getPathToAttach(photoEntry.document, true).getAbsolutePath());
                    } else {
                        showing = PhotoViewer.getInstance().isShowingImage(photoEntry.imageUrl);
                    }
                }
                imageView.getImageReceiver().setVisible(!showing, true);
                cell.checkBox.setVisibility(singlePhoto || showing ? View.GONE : View.VISIBLE);
            } else if (viewType == 1) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.media_loading_layout, viewGroup, false);
                }
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = itemWidth;
                params.height = itemWidth;
                view.setLayoutParams(params);
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (selectedAlbum != null || searchResult.isEmpty() && lastSearchString == null && i < recentImages.size() || i < searchResult.size()) {
                return 0;
            }
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            if (selectedAlbum != null) {
                return selectedAlbum.photos.isEmpty();
            } else {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return recentImages.isEmpty();
                } else {
                    return searchResult.isEmpty();
                }
            }
        }

    }
    private Context mContext;

    private void deleteCheckedFiles() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("DeleteQuestion", R.string.DeleteQuestion));
        builder.setMessage(LocaleController.getString("AreYouSureDeletePhotos", R.string.AreYouSureDeletePhotos));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    if (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 23 && ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        for (final MediaController.PhotoEntry photoEntry : selectedPhotos.values()){
                            final File file = new File(photoEntry.path);
                            DELETE_SERVICE.submit(new Runnable() {
                                @Override
                                public void run() {
                                    if (file.exists() && !file.isDirectory() && file.delete()) {
                                        MediaScannerConnection.scanFile(
                                                getParentActivity(),
                                                new String[]{photoEntry.path},
                                                null, null);

                                        AndroidUtilities.runOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.fileDeleted, photoEntry);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                        deselectAllPhotos();
                    }
                } catch (Throwable e) {
                    FileLog.e("picca", e);
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }
}
