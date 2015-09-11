/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC);

    private static final Typeface ITALIC_TYPEFACE =
            Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

//    private LayoutInflater mLayoutInflater;
//    private LinearLayout mLinearLayout;
//    private TextView tvCurrentTime;

    @Override
    public Engine onCreateEngine() {
//        mLayoutInflater = LayoutInflater.from(this);
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mLinebreak;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mDatePaint;
        Paint mMaxTempPaint;
        Paint mMinTempPaint;
        Paint mImagePaint;

        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        java.text.DateFormat mDateFormat;

        boolean mAmbient;

        Time mTime;

        float mXOffset;
        float mYOffset;
        float mLineHeight;
        float mLineWidth;
        float mTextSeparatorWidth;
        Bitmap mWeatherImage;
        Bitmap mGrayWeatherBitmap;

//        private LinearLayout mLinearLayout;
        private TextView tvTime;
        private LinearLayout mLinearLayout;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
//            mLinearLayout = (LinearLayout) mLayoutInflater.inflate(R.layout.sunshine_watch_face, null, false);
//            tvCurrentTime = (TextView)mLinearLayout.findViewById(R.id.tv_time);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mLineHeight = resources.getDimension(R.dimen.regular_line_height);
            mLineWidth = resources.getDimension(R.dimen.line_width);
            mTextSeparatorWidth = resources.getDimension(R.dimen.text_separator);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.primary));

            mHourPaint = createTextPaint(resources.getColor(R.color.digital_text), BOLD_TYPEFACE);
            mMinutePaint = createTextPaint(resources.getColor(R.color.digital_text), NORMAL_TYPEFACE);
            mMaxTempPaint = createTextPaint(resources.getColor(R.color.digital_text), BOLD_TYPEFACE);
            mMaxTempPaint.setTextAlign(Paint.Align.CENTER);
            mMinTempPaint = createTextPaint(resources.getColor(R.color.digital_text), NORMAL_TYPEFACE);
            mDatePaint = createTextPaint(resources.getColor(R.color.digital_text), ITALIC_TYPEFACE);
            mDatePaint.setTextAlign(Paint.Align.CENTER);
            mImagePaint = new Paint();
            mImagePaint.setTextAlign(Paint.Align.CENTER);
            mLinebreak = new Paint();
            mLinebreak.setColor(resources.getColor(R.color.digital_text));

            int imageSize = (int)resources.getDimension(R.dimen.weather_image_size);
            //TODO remove this test image
            mWeatherImage = getResizedBitmap(BitmapFactory.decodeResource(resources, R.drawable.art_clear), imageSize,imageSize);

            mTime = new Time();
            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private void initFormats() {
            mDayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(SunshineWatchFace.this);
            mDateFormat.setCalendar(mCalendar);
        }

//        private void initGrayBackgroundBitmap() {
//            mGrayWeatherBitmap = Bitmap.createBitmap(
//                    mWeatherImage.getWidth(),
//                    mWeatherImage.getHeight(),
//                    Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(mGrayWeatherBitmap);
//            Paint grayPaint = new Paint();
//            ColorMatrix colorMatrix = new ColorMatrix();
//            colorMatrix.setSaturation(0);
//            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
//            grayPaint.setColorFilter(filter);
//            canvas.drawBitmap(mWeatherImage, mYOffset, mYOffset + mLineHeight * 4, mImagePaint);
//        }

        private Paint createTextPaint(int textColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            /* Scale loaded background image (more efficient) if surface dimensions change. */

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);
            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mMaxTempPaint.setTextSize(textSize);
            mMinTempPaint.setTextSize(textSize);
            mDatePaint.setTextSize(dateSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mBackgroundPaint.setColor(getColor(R.color.black));
                    mBackgroundPaint.setAntiAlias(!inAmbientMode);
                    mHourPaint.setAntiAlias(!inAmbientMode);
                    mLinebreak.setAntiAlias(!inAmbientMode);
                    mMinutePaint.setAntiAlias(!inAmbientMode);
                    mDatePaint.setAntiAlias(!inAmbientMode);
                    mMaxTempPaint.setAntiAlias(!inAmbientMode);
                    mMinTempPaint.setAntiAlias(!inAmbientMode);
                    mImagePaint.setAntiAlias(!inAmbientMode);

                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();
            String hour = String.format("%d", mTime.hour);
            String minutes = mAmbient ? String.format(":%02d", mTime.minute) : String.format(":%02d:%02d", mTime.minute, mTime.second);

            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            int middleX = bounds.width()/2;

            float positionTime = middleX - (mHourPaint.measureText(hour)+mMinutePaint.measureText(minutes))/2;
            canvas.drawText(hour, positionTime, mYOffset, mHourPaint);

            positionTime += mHourPaint.measureText(hour);
            canvas.drawText(minutes, positionTime, mYOffset, mMinutePaint);

            canvas.drawText(
                    mDateFormat.format(mDate),
                    middleX, mYOffset + mLineHeight * 2, mDatePaint);

            canvas.drawLine(middleX - mLineWidth / 2, mYOffset + mLineHeight * 3, middleX + mLineWidth / 2, mYOffset + mLineHeight * 3 + 1, mLinebreak);

            String maxTemp = "25";
            String minTemp = "16";

            canvas.drawText(maxTemp, middleX, mYOffset + mLineHeight * 7, mMaxTempPaint);
            canvas.drawText(minTemp, middleX + mMaxTempPaint.measureText(maxTemp), mYOffset + mLineHeight * 7, mMinTempPaint);
            canvas.drawBitmap(mWeatherImage, middleX - mMaxTempPaint.measureText(maxTemp) * 2,  mYOffset + mLineHeight * 4, mImagePaint);

        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
