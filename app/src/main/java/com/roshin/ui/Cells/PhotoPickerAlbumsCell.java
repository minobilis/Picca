/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Author Nikolai Kudashov, Ivan Roshinsky.
 */

package com.roshin.ui.Cells;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.roshin.Picca.R;
import com.roshin.gallery.AndroidUtilities;
import com.roshin.gallery.AnimatorListenerAdapterProxy;
import com.roshin.gallery.ApplicationLoader;
import com.roshin.gallery.FileLog;
import com.roshin.gallery.MediaController;
import com.roshin.gallery.NotificationCenter;
import com.roshin.ui.Components.BackupImageView;
import com.roshin.ui.Components.CheckBox;
import com.roshin.ui.Components.LayoutHelper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoPickerAlbumsCell extends FrameLayout {

    public interface PhotoPickerAlbumsCellDelegate {
        void didSelectAlbum(MediaController.AlbumEntry albumEntry, AlbumView albumView);
    }

    private static final ExecutorService DELETE_SERVICE = Executors.newSingleThreadExecutor();

    private AlbumView[] albumViews;
    private MediaController.AlbumEntry[] albumEntries;
    private int albumsCount;
    private PhotoPickerAlbumsCellDelegate delegate;

    public class AlbumView extends FrameLayout {

        private BackupImageView imageView;
        private TextView nameTextView;
        private TextView countTextView;
        private View selector;

        public FrameLayout checkFrame;
        public CheckBox checkBox;
        private AnimatorSet animator;

        public AlbumView(Context context) {
            super(context);

            imageView = new BackupImageView(context);
            addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            checkFrame = new FrameLayout(context);
            addView(checkFrame, LayoutHelper.createFrame(42, 42, Gravity.RIGHT | Gravity.TOP));

            checkBox = new CheckBox(context, R.drawable.checkbig);
            checkBox.setSize(30);
            checkBox.setCheckOffset(AndroidUtilities.dp(1));
            checkBox.setDrawBackground(true);
            checkBox.setColor(0xff3ccaef);
            checkBox.setVisibility(GONE);
            addView(checkBox, LayoutHelper.createFrame(30, 30, Gravity.RIGHT | Gravity.TOP, 0, 4, 4, 0));

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setBackgroundColor(0x7f000000);
            addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 28, Gravity.LEFT | Gravity.BOTTOM));

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            int albumTextSize = preferences.getInt("album_font_size", AndroidUtilities.isTablet() ? 15 : 13);

            nameTextView = new TextView(context);
            nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, albumTextSize);
            nameTextView.setTextColor(0xffffffff);
            nameTextView.setSingleLine(true);
            nameTextView.setEllipsize(TextUtils.TruncateAt.END);
            nameTextView.setMaxLines(1);
            nameTextView.setGravity(Gravity.CENTER_VERTICAL);
            linearLayout.addView(nameTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 8, 0, 0, 0));

            countTextView = new TextView(context);
            countTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, albumTextSize);
            countTextView.setTextColor(0xffaaaaaa);
            countTextView.setSingleLine(true);
            countTextView.setEllipsize(TextUtils.TruncateAt.END);
            countTextView.setMaxLines(1);
            countTextView.setGravity(Gravity.CENTER_VERTICAL);
            linearLayout.addView(countTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, 4, 0, 4, 0));

            selector = new View(context);
            selector.setBackgroundResource(R.drawable.list_selector);
            addView(selector, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (Build.VERSION.SDK_INT >= 21) {
                selector.drawableHotspotChanged(event.getX(), event.getY());
            }
            return super.onTouchEvent(event);
        }

        public void setChecked(final boolean checked, final boolean animated) {
            checkBox.setChecked(checked, animated);
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            if (animated) {
                if (checked) {
                    setBackgroundColor(0xff0A0A0A);
                }
                animator = new AnimatorSet();
                animator.playTogether(ObjectAnimator.ofFloat(imageView, "scaleX", checked ? 0.85f : 1.0f),
                        ObjectAnimator.ofFloat(imageView, "scaleY", checked ? 0.85f : 1.0f));
                animator.setDuration(200);
                animator.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (animator != null && animator.equals(animation)) {
                            animator = null;
                            if (!checked) {
                                setBackgroundColor(0);
                            }
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        if (animator != null && animator.equals(animation)) {
                            animator = null;
                        }
                    }
                });
                animator.start();
            } else {
                setBackgroundColor(checked ? 0xff0A0A0A : 0);
                imageView.setScaleX(checked ? 0.85f : 1.0f);
                imageView.setScaleY(checked ? 0.85f : 1.0f);
            }
        }

        public int reverseChecked(){
            if (checkBox.isChecked()) {
                uncheck();
                return -1;
            } else {
                check();
                return 1;
            }
        }

        public void check(){
            checkBox.setVisibility(View.VISIBLE);
            setChecked(true, true);
        }

        public void uncheck(){
            setChecked(false, true);
            checkBox.setVisibility(View.GONE);
        }
    }

    public PhotoPickerAlbumsCell(Context context) {
        super(context);
        albumEntries = new MediaController.AlbumEntry[4];
        albumViews = new AlbumView[4];
        for (int a = 0; a < 4; a++) {
            albumViews[a] = new AlbumView(context);
            addView(albumViews[a]);
            albumViews[a].setVisibility(INVISIBLE);
            albumViews[a].setTag(a);

            albumViews[a].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (delegate != null) {
                        delegate.didSelectAlbum(albumEntries[(Integer) v.getTag()], albumViews[(Integer) v.getTag()]);
                    }
                }
            });

            albumViews[a].setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    albumViews[(Integer) v.getTag()].reverseChecked();
                    NotificationCenter.getInstance().postNotificationName(
                            NotificationCenter.albumSelectedStateChange,
                            albumEntries[(Integer) v.getTag()],
                            albumViews[(Integer) v.getTag()].checkBox.isChecked());
                    return true;
                }
            });
        }
    }

    public void setAlbumsCount(int count) {
        for (int a = 0; a < albumViews.length; a++) {
            albumViews[a].setVisibility(a < count ? VISIBLE : INVISIBLE);
        }
        albumsCount = count;
    }

    public void setDelegate(PhotoPickerAlbumsCellDelegate delegate) {
        this.delegate = delegate;
    }

    public void setAlbum(int a, MediaController.AlbumEntry albumEntry) {
        albumEntries[a] = albumEntry;

        if (albumEntry != null) {
            AlbumView albumView = albumViews[a];
            albumView.imageView.setOrientation(0, true);
            if (albumEntry.coverPhoto != null && albumEntry.coverPhoto.path != null) {
                albumView.imageView.setOrientation(albumEntry.coverPhoto.orientation, true);
                if (albumEntry.coverPhoto.isVideo) {
                    albumView.imageView.setImage("vthumb://" + albumEntry.coverPhoto.imageId + ":" + albumEntry.coverPhoto.path, null, getContext().getResources().getDrawable(R.drawable.nophotos));
                } else {
                    albumView.imageView.setImage("thumb://" + albumEntry.coverPhoto.imageId + ":" + albumEntry.coverPhoto.path, null, getContext().getResources().getDrawable(R.drawable.nophotos));
                }
            } else {
                albumView.imageView.setImageResource(R.drawable.nophotos);
            }
            albumView.nameTextView.setText(albumEntry.bucketName);
            albumView.countTextView.setText(String.format("%d", albumEntry.photos.size()));
        } else {
            albumViews[a].setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int itemWidth;
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - ((albumsCount + 1) * AndroidUtilities.dp(4))) / albumsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x - ((albumsCount + 1) * AndroidUtilities.dp(4))) / albumsCount;
        }

        for (int a = 0; a < albumsCount; a++) {
            LayoutParams layoutParams = (LayoutParams) albumViews[a].getLayoutParams();
            layoutParams.topMargin = AndroidUtilities.dp(4);
            layoutParams.leftMargin = (itemWidth + AndroidUtilities.dp(4)) * a;
            layoutParams.width = itemWidth;
            layoutParams.height = itemWidth;
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            albumViews[a].setLayoutParams(layoutParams);
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(4) + itemWidth, MeasureSpec.EXACTLY));
    }

    public void deleteSelectedAlbumsWithContent() {
        for (int i = 0; i < albumEntries.length; i++){
            MediaController.AlbumEntry entry = albumEntries[i];
            AlbumView albumView = albumViews[i];
            if (entry != null && albumView != null && albumViews[i].checkBox.isChecked()) {
                try {
                    if (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 23 && ApplicationLoader.applicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        File firstPicture = new File(entry.photos.get(0).path);
                        final File albumDirectory = firstPicture.getParentFile();
                        for (final MediaController.PhotoEntry photoEntry : entry.photos){
                            final File file = new File(photoEntry.path);
                            DELETE_SERVICE.submit(new Runnable() {
                                @Override
                                public void run() {
                                    if (file.exists() && !file.isDirectory() && file.delete()) {
                                        MediaScannerConnection.scanFile(
                                                getContext(),
                                                new String[]{photoEntry.path},
                                                null, null);

                                        AndroidUtilities.runOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.fileDeleted, photoEntry);
                                            }
                                        });

                                        if (albumDirectory.isDirectory() && albumDirectory.listFiles().length == 0) {
                                            albumDirectory.delete();
                                        }
                                    }
                                }
                            });
                        }
                    }
                } catch (Throwable e) {
                    FileLog.e("picca", e);
                }
            }
        }
    }

    public void deselectAllAlbums() {
        for (int i = 0; i < albumEntries.length; i++){
            MediaController.AlbumEntry albumEntry = albumEntries[i];
            AlbumView albumView = albumViews[i];
            if (albumEntry != null && albumView != null && albumViews[i].checkBox.isChecked()) {
                albumViews[i].reverseChecked();
            }
        }
    }
}
