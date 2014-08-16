package com.olecco.android.PictureFetcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by olecco on 16.08.2014.
 */
public class ImageLoader {

    private final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private final int cacheSize = maxMemory / 8;
    private LruCache<File, Bitmap> mThumbnailsCache = new LruCache<File, Bitmap>(cacheSize) {
        @Override
        protected int sizeOf(File key, Bitmap value) {
            return (value.getRowBytes() * value.getHeight()) / 1024; // in KB
        }
    };

    private boolean mPaused;

    public void loadThumbnail(File file, ImageView imageView) {
//        if (!mPaused) {
            LoadingTaskWrapper wrapper = (LoadingTaskWrapper) imageView.getTag();
            if (wrapper!= null && wrapper.getTask() != null) {
                wrapper.getTask().cancel(true);
                wrapper.getTask().setImageView(null);
            }

            if ((wrapper != null && !file.equals(wrapper.getFile())) || wrapper == null) {
                Bitmap bitmap = mThumbnailsCache.get(file);
                if (bitmap != null) {
                    setBitmapToImageView(bitmap, imageView);
                }
                else {
                    setStubToImageView(imageView);
                    ImageLoadingTask task = new ImageLoadingTask(file, imageView);
                    wrapper = new LoadingTaskWrapper(file, task);
                    imageView.setTag(wrapper);
                    task.execute();
                }
            }

  //      }
    }

    private void setStubToImageView(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_image_stub);
    }

    public void setPaused(boolean value) {
        mPaused = value;
    }

    public static Bitmap decodeSampledBitmapFromFile(File file, int actualWidth, int actualHeight) {
        if (actualWidth == 0 && actualHeight == 0) {
            return BitmapFactory.decodeFile(file.getPath());
        }
        else {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            options.inSampleSize = calculateSampleSize(options, actualWidth, actualHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(file.getPath(), options);
        }
    }

    private static void setBitmapToImageView(Bitmap bitmap, ImageView imageView) {
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int actualWidth, int actualHeight) {
        final int bmWidth = options.outWidth;
        final int bmHeight = options.outHeight;
        int inSampleSize = 1;

        if (bmHeight > actualHeight || bmWidth > actualWidth) {
            final int heightRatio = Math.round((float) bmHeight / (float) actualHeight);
            final int widthRatio = Math.round((float) bmWidth / (float) actualWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    private static class LoadingTaskWrapper {

        private File mFile;
        private ImageLoadingTask mTask;

        public LoadingTaskWrapper(File file, ImageLoadingTask task) {
            mFile = file;
            mTask = task;
        }

        private File getFile() {
            return mFile;
        }

        private ImageLoadingTask getTask() {
            return mTask;
        }
    }

    private class ImageLoadingTask extends AsyncTask<Void, Void, Bitmap> {

        private File mFile;
        private WeakReference<ImageView> mImageViewRef;

        public ImageLoadingTask(File file, ImageView imageView) {
            mFile = file;
            setImageView(imageView);
        }

        public void setImageView(ImageView imageView) {
            mImageViewRef = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if (!isCancelled()) {
                ImageView imageView = mImageViewRef.get();
                if (imageView != null) {
                    int actualWidth = imageView.getMeasuredWidth();
                    int actualHeight = imageView.getMeasuredHeight();
                    Bitmap bitmap = decodeSampledBitmapFromFile(mFile, actualWidth, actualHeight);
                    return bitmap;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ImageView imageView = mImageViewRef.get();
                if (imageView != null) {
                    LoadingTaskWrapper wrapper = (LoadingTaskWrapper) imageView.getTag();
                    if (wrapper != null && mFile.equals(wrapper.getFile())) {
                        setBitmapToImageView(bitmap, imageView);
                    }
                }
            }
        }
    }
}
