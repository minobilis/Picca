/*
 * This is the source code of Picca for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Author Nikolai Kudashov.
 */

package com.roshin.ui.Cells;

import android.content.Context;
import android.view.View;

import com.roshin.Picca.R;
import com.roshin.gallery.AndroidUtilities;


public class ShadowSectionCell extends View {

    private int size = 12;

    public ShadowSectionCell(Context context) {
        super(context);
        setBackgroundResource(R.drawable.greydivider);
    }

    public void setSize(int value) {
        size = value;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(size), MeasureSpec.EXACTLY));
    }
}
