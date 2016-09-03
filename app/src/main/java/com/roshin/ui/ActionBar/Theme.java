/*
 * This is the source code of Picca for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Author Nikolai Kudashov.
 */

package com.roshin.ui.ActionBar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import com.roshin.gallery.AndroidUtilities;
import com.roshin.gallery.ApplicationLoader;
import com.roshin.Picca.R;


public class Theme {

    public static final int ACTION_BAR_COLOR = 0xff527da3;
    public static final int ACTION_BAR_PHOTO_VIEWER_COLOR = 0x7f000000;
    public static final int ACTION_BAR_MEDIA_PICKER_COLOR = 0xff333333;

    public static final int ACTION_BAR_SUBTITLE_COLOR = 0xffd5e8f7;
    public static final int ACTION_BAR_PROFILE_COLOR = 0xff64d2bf;

    public static final int ACTION_BAR_ACTION_MODE_TEXT_COLOR = 0xff737373;
    public static final int ACTION_BAR_SELECTOR_COLOR = 0xff406d94;

    public static final int ACTION_BAR_PICKER_SELECTOR_COLOR = 0xff3d3d3d;
    public static final int ACTION_BAR_WHITE_SELECTOR_COLOR = 0x40ffffff;
    public static final int ACTION_BAR_AUDIO_SELECTOR_COLOR = 0x2f000000;
    public static final int ACTION_BAR_MODE_SELECTOR_COLOR = 0xfff0f0f0;
    public static final int MSG_LINK_TEXT_COLOR = 0xff2678b6;


    public static Drawable backgroundDrawableIn;
    public static Drawable backgroundDrawableInSelected;
    public static Drawable backgroundDrawableOut;
    public static Drawable backgroundDrawableOutSelected;
    public static Drawable backgroundMediaDrawableIn;
    public static Drawable backgroundMediaDrawableInSelected;
    public static Drawable backgroundMediaDrawableOut;
    public static Drawable backgroundMediaDrawableOutSelected;
    public static Drawable checkDrawable;
    public static Drawable halfCheckDrawable;
    public static Drawable clockDrawable;
    public static Drawable broadcastDrawable;
    public static Drawable checkMediaDrawable;
    public static Drawable halfCheckMediaDrawable;
    public static Drawable clockMediaDrawable;
    public static Drawable broadcastMediaDrawable;
    public static Drawable errorDrawable;
    public static Drawable systemDrawable;
    public static Drawable timeBackgroundDrawable;
    public static Drawable timeStickerBackgroundDrawable;
    public static Drawable botLink;
    public static Drawable botInline;
    public static Drawable[] clockChannelDrawable = new Drawable[2];

    public static Drawable[] cornerOuter = new Drawable[4];
    public static Drawable[] cornerInner = new Drawable[4];

    public static Drawable shareDrawable;
    public static Drawable shareIconDrawable;

    public static Drawable[] viewsCountDrawable = new Drawable[2];
    public static Drawable viewsOutCountDrawable;
    public static Drawable viewsMediaCountDrawable;

    public static Drawable geoInDrawable;
    public static Drawable geoOutDrawable;

    public static Drawable inlineDocDrawable;
    public static Drawable inlineAudioDrawable;
    public static Drawable inlineLocationDrawable;

    public static Drawable[] contactDrawable = new Drawable[2];
    public static Drawable[][] fileStatesDrawable = new Drawable[10][2];
    public static Drawable[][] photoStatesDrawables = new Drawable[13][2];
    public static Drawable[] docMenuDrawable = new Drawable[4];

    public static PorterDuffColorFilter colorFilter;
    public static PorterDuffColorFilter colorPressedFilter;
    private static int currentColor;

    private static Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static void loadRecources(Context context) {
        if (backgroundDrawableIn == null) {
            backgroundDrawableIn = context.getResources().getDrawable(R.drawable.msg_in);
            backgroundDrawableInSelected = context.getResources().getDrawable(R.drawable.msg_in_selected);
            backgroundDrawableOut = context.getResources().getDrawable(R.drawable.msg_out);
            backgroundDrawableOutSelected = context.getResources().getDrawable(R.drawable.msg_out_selected);
            backgroundMediaDrawableIn = context.getResources().getDrawable(R.drawable.msg_in_photo);
            backgroundMediaDrawableInSelected = context.getResources().getDrawable(R.drawable.msg_in_photo_selected);
            backgroundMediaDrawableOut = context.getResources().getDrawable(R.drawable.msg_out_photo);
            backgroundMediaDrawableOutSelected = context.getResources().getDrawable(R.drawable.msg_out_photo_selected);
            checkDrawable = context.getResources().getDrawable(R.drawable.msg_check);
            halfCheckDrawable = context.getResources().getDrawable(R.drawable.msg_halfcheck);
            clockDrawable = context.getResources().getDrawable(R.drawable.msg_clock);
            checkMediaDrawable = context.getResources().getDrawable(R.drawable.msg_check_w);
            halfCheckMediaDrawable = context.getResources().getDrawable(R.drawable.msg_halfcheck_w);
            clockMediaDrawable = context.getResources().getDrawable(R.drawable.msg_clock_photo);
            clockChannelDrawable[0] = context.getResources().getDrawable(R.drawable.msg_clock2);
            clockChannelDrawable[1] = context.getResources().getDrawable(R.drawable.msg_clock2_s);
            errorDrawable = context.getResources().getDrawable(R.drawable.msg_warning);
            timeBackgroundDrawable = context.getResources().getDrawable(R.drawable.phototime2_b);
            timeStickerBackgroundDrawable = context.getResources().getDrawable(R.drawable.phototime2);
            broadcastDrawable = context.getResources().getDrawable(R.drawable.broadcast3);
            broadcastMediaDrawable = context.getResources().getDrawable(R.drawable.broadcast4);
            systemDrawable = context.getResources().getDrawable(R.drawable.system);
            botLink = context.getResources().getDrawable(R.drawable.bot_link);
            botInline = context.getResources().getDrawable(R.drawable.bot_lines);

            viewsCountDrawable[0] = context.getResources().getDrawable(R.drawable.post_views);
            viewsCountDrawable[1] = context.getResources().getDrawable(R.drawable.post_views_s);
            viewsOutCountDrawable = context.getResources().getDrawable(R.drawable.post_viewsg);
            viewsMediaCountDrawable = context.getResources().getDrawable(R.drawable.post_views_w);

            fileStatesDrawable[0][0] = context.getResources().getDrawable(R.drawable.play_g);
            fileStatesDrawable[0][1] = context.getResources().getDrawable(R.drawable.play_g_s);
            fileStatesDrawable[1][0] = context.getResources().getDrawable(R.drawable.pause_g);
            fileStatesDrawable[1][1] = context.getResources().getDrawable(R.drawable.pause_g_s);
            fileStatesDrawable[2][0] = context.getResources().getDrawable(R.drawable.file_g_load);
            fileStatesDrawable[2][1] = context.getResources().getDrawable(R.drawable.file_g_load_s);
            fileStatesDrawable[3][0] = context.getResources().getDrawable(R.drawable.file_g);
            fileStatesDrawable[3][1] = context.getResources().getDrawable(R.drawable.file_g_s);
            fileStatesDrawable[4][0] = context.getResources().getDrawable(R.drawable.file_g_cancel);
            fileStatesDrawable[4][1] = context.getResources().getDrawable(R.drawable.file_g_cancel_s);
            fileStatesDrawable[5][0] = context.getResources().getDrawable(R.drawable.play_b);
            fileStatesDrawable[5][1] = context.getResources().getDrawable(R.drawable.play_b_s);
            fileStatesDrawable[6][0] = context.getResources().getDrawable(R.drawable.pause_b);
            fileStatesDrawable[6][1] = context.getResources().getDrawable(R.drawable.pause_b_s);
            fileStatesDrawable[7][0] = context.getResources().getDrawable(R.drawable.file_b_load);
            fileStatesDrawable[7][1] = context.getResources().getDrawable(R.drawable.file_b_load_s);
            fileStatesDrawable[8][0] = context.getResources().getDrawable(R.drawable.file_b);
            fileStatesDrawable[8][1] = context.getResources().getDrawable(R.drawable.file_b_s);
            fileStatesDrawable[9][0] = context.getResources().getDrawable(R.drawable.file_b_cancel);
            fileStatesDrawable[9][1] = context.getResources().getDrawable(R.drawable.file_b_cancel_s);

            photoStatesDrawables[0][0] = context.getResources().getDrawable(R.drawable.photoload);
            photoStatesDrawables[0][1] = context.getResources().getDrawable(R.drawable.photoload_pressed);
            photoStatesDrawables[1][0] = context.getResources().getDrawable(R.drawable.photocancel);
            photoStatesDrawables[1][1] = context.getResources().getDrawable(R.drawable.photocancel_pressed);
            photoStatesDrawables[2][0] = context.getResources().getDrawable(R.drawable.photogif);
            photoStatesDrawables[2][1] = context.getResources().getDrawable(R.drawable.photogif_pressed);
            photoStatesDrawables[3][0] = context.getResources().getDrawable(R.drawable.playvideo);
            photoStatesDrawables[3][1] = context.getResources().getDrawable(R.drawable.playvideo_pressed);
            photoStatesDrawables[4][0] = photoStatesDrawables[4][1] = context.getResources().getDrawable(R.drawable.burn);
            photoStatesDrawables[5][0] = photoStatesDrawables[5][1] = context.getResources().getDrawable(R.drawable.circle);
            photoStatesDrawables[6][0] = photoStatesDrawables[6][1] = context.getResources().getDrawable(R.drawable.photocheck);

            photoStatesDrawables[7][0] = context.getResources().getDrawable(R.drawable.photoload_g);
            photoStatesDrawables[7][1] = context.getResources().getDrawable(R.drawable.photoload_g_s);
            photoStatesDrawables[8][0] = context.getResources().getDrawable(R.drawable.photocancel_g);
            photoStatesDrawables[8][1] = context.getResources().getDrawable(R.drawable.photocancel_g_s);
            photoStatesDrawables[9][0] = context.getResources().getDrawable(R.drawable.doc_green);
            photoStatesDrawables[9][1] = context.getResources().getDrawable(R.drawable.doc_green);

            photoStatesDrawables[10][0] = context.getResources().getDrawable(R.drawable.photoload_b);
            photoStatesDrawables[10][1] = context.getResources().getDrawable(R.drawable.photoload_b_s);
            photoStatesDrawables[11][0] = context.getResources().getDrawable(R.drawable.photocancel_b);
            photoStatesDrawables[11][1] = context.getResources().getDrawable(R.drawable.photocancel_b_s);
            photoStatesDrawables[12][0] = context.getResources().getDrawable(R.drawable.doc_blue);
            photoStatesDrawables[12][1] = context.getResources().getDrawable(R.drawable.doc_blue_s);

            docMenuDrawable[0] = context.getResources().getDrawable(R.drawable.doc_actions_b);
            docMenuDrawable[1] = context.getResources().getDrawable(R.drawable.doc_actions_g);
            docMenuDrawable[2] = context.getResources().getDrawable(R.drawable.doc_actions_b_s);
            docMenuDrawable[3] = context.getResources().getDrawable(R.drawable.video_actions);

            contactDrawable[0] = context.getResources().getDrawable(R.drawable.contact_blue);
            contactDrawable[1] = context.getResources().getDrawable(R.drawable.contact_green);

            shareDrawable = context.getResources().getDrawable(R.drawable.share_round);
            shareIconDrawable = context.getResources().getDrawable(R.drawable.share_arrow);

            geoInDrawable = context.getResources().getDrawable(R.drawable.location_b);
            geoOutDrawable = context.getResources().getDrawable(R.drawable.location_g);

            cornerOuter[0] = context.getResources().getDrawable(R.drawable.corner_out_tl);
            cornerOuter[1] = context.getResources().getDrawable(R.drawable.corner_out_tr);
            cornerOuter[2] = context.getResources().getDrawable(R.drawable.corner_out_br);
            cornerOuter[3] = context.getResources().getDrawable(R.drawable.corner_out_bl);

            cornerInner[0] = context.getResources().getDrawable(R.drawable.corner_in_tr);
            cornerInner[1] = context.getResources().getDrawable(R.drawable.corner_in_tl);
            cornerInner[2] = context.getResources().getDrawable(R.drawable.corner_in_br);
            cornerInner[3] = context.getResources().getDrawable(R.drawable.corner_in_bl);

            inlineDocDrawable = context.getResources().getDrawable(R.drawable.bot_file);
            inlineAudioDrawable = context.getResources().getDrawable(R.drawable.bot_music);
            inlineLocationDrawable = context.getResources().getDrawable(R.drawable.bot_location);
        }

        int color = ApplicationLoader.getServiceMessageColor();
        if (currentColor != color) {
            colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
            colorPressedFilter = new PorterDuffColorFilter(ApplicationLoader.getServiceSelectedMessageColor(), PorterDuff.Mode.MULTIPLY);
            currentColor = color;
            for (int a = 0; a < 4; a++) {
                cornerOuter[a].setColorFilter(colorFilter);
                cornerInner[a].setColorFilter(colorFilter);
            }
            timeStickerBackgroundDrawable.setColorFilter(colorFilter);
        }
    }

    public static Drawable createBarSelectorDrawable(int color) {
        return createBarSelectorDrawable(color, true);
    }

    public static Drawable createBarSelectorDrawable(int color, boolean masked) {
        if (Build.VERSION.SDK_INT >= 21) {
            Drawable maskDrawable = null;
            if (masked) {
                maskPaint.setColor(0xffffffff);
                maskDrawable = new Drawable() {
                    @Override
                    public void draw(Canvas canvas) {
                        android.graphics.Rect bounds = getBounds();
                        canvas.drawCircle(bounds.centerX(), bounds.centerY(), AndroidUtilities.dp(18), maskPaint);
                    }

                    @Override
                    public void setAlpha(int alpha) {

                    }

                    @Override
                    public void setColorFilter(ColorFilter colorFilter) {

                    }

                    @Override
                    public int getOpacity() {
                        return 0;
                    }
                };
            }
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{new int[]{}},
                    new int[]{color}
            );
            return new RippleDrawable(colorStateList, null, maskDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_focused}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_activated}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{}, new ColorDrawable(0x00000000));
            return stateListDrawable;
        }
    }
}
