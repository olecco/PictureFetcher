package com.olecco.android.PictureFetcher;

import android.os.Environment;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by olecco on 16.08.2014.
 */
public class FileUtils {

    public static class FileExtentionFilter implements FileFilter {
        private String[] mExtentions;

        public FileExtentionFilter(String[] extentions) {
            mExtentions = extentions;
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            if (mExtentions == null) {
                return true;
            }
            if (mExtentions.length == 0) {
                return true;
            }
            for (String ext : mExtentions) {
                if (file.getName().toLowerCase().endsWith(ext.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (String ext : mExtentions) {
                builder.append("*" + ext + "  ");
            }
            return builder.toString();
        }

        public String[] getExtentions() {
            return mExtentions;
        }
    }

    public static File getExternalStorage() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable()) {
            return Environment.getExternalStorageDirectory();
        }
        return null;
    }

    public static FileFilter createImagesFilter() {
        return new FileExtentionFilter(new String[] { "png", "jpg", "jpeg", "bmp" });
    }
}
