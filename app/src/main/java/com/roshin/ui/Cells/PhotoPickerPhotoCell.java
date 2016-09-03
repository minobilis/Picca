/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Author Nikolai Kudashov, Ivan Roshinsky.
 */

package com.roshin.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.roshin.gallery.AndroidUtilities;
import com.roshin.gallery.AnimatorListenerAdapterProxy;

import com.roshin.Picca.R;
import com.roshin.ui.Components.BackupImageView;
import com.roshin.ui.Components.CheckBox;
import com.roshin.ui.Components.LayoutHelper;

public class PhotoPickerPhotoCell extends FrameLayout {

    public BackupImageView photoImage;
    public FrameLayout checkFrame;
    public CheckBox checkBox;
    private AnimatorSet animator;
    public int itemWidth;

    public PhotoPickerPhotoCell(Context context) {
        super(context);

        photoImage = new BackupImageView(context);
        addView(photoImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        checkFrame = new FrameLayout(context);
        addView(checkFrame, LayoutHelper.createFrame(42, 42, Gravity.RIGHT | Gravity.TOP));

        checkBox = new CheckBox(context, R.drawable.checkbig);
        checkBox.setSize(30);
        checkBox.setCheckOffset(AndroidUtilities.dp(1));
        checkBox.setDrawBackground(true);
        checkBox.setColor(0xff3ccaef);
        addView(checkBox, LayoutHelper.createFrame(30, 30, Gravity.RIGHT | Gravity.TOP, 0, 4, 4, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY));
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
            animator.playTogether(ObjectAnimator.ofFloat(photoImage, "scaleX", checked ? 0.85f : 1.0f),
                    ObjectAnimator.ofFloat(photoImage, "scaleY", checked ? 0.85f : 1.0f));
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
            photoImage.setScaleX(checked ? 0.85f : 1.0f);
            photoImage.setScaleY(checked ? 0.85f : 1.0f);
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
