package com.olecco.android.PictureFetcher.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.olecco.android.PictureFetcher.FileUtils;
import com.olecco.android.PictureFetcher.ImageLoader;
import com.olecco.android.PictureFetcher.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by olecco on 16.08.2014.
 */
public class FilesFragment extends Fragment {

    private final static int DEFAULT_FILES_CHUNK_SIZE = 20;

    private List<File> mFiles = new ArrayList<File>();
    private ListView mFileList;
    private FileListAdapter mAdapter;
    private ImageFetcherThread mFetcherThread;
    private ImageLoader mImageLoader = new ImageLoader();

    private Object mSync = new Object();

    private Handler mNotifyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg != null && msg.obj != null) {
                List<File> buffer = (List<File>) msg.obj;
                mFiles.addAll(buffer);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
//            if (scrollState == SCROLL_STATE_IDLE) {
//                mImageLoader.setPaused(true);
//            }
//            else {
//                mImageLoader.setPaused(false);
//            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean isLast = firstVisibleItem + visibleItemCount == totalItemCount;
            if (isLast) {
                continueFetching();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.files_fragment, null);

        mFileList = (ListView) view.findViewById(R.id.fileList);
        mAdapter = new FileListAdapter();
        mFileList.setOnScrollListener(mScrollListener);
        mFileList.setAdapter(mAdapter);



        Button btnTest = (Button) view.findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startFilesFetching();


            }
        });



        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        stopFilesFetching();
    }

    private void clearFiles() {
        mFiles.clear();
        mAdapter.notifyDataSetChanged();
    }

    private void startFilesFetching() {
        clearFiles();
        File path = FileUtils.getExternalStorage();
        mFetcherThread = new ImageFetcherThread(path, DEFAULT_FILES_CHUNK_SIZE, mSync);
        mFetcherThread.start();
    }

    private void stopFilesFetching() {
        if (mFetcherThread != null) {
            mFetcherThread.stopFetching();
            mFetcherThread.interrupt();
            mFetcherThread = null;
        }
    }

    private void continueFetching() {
        synchronized (mSync) {
            mSync.notify();
        }
    }

    private class ImageFetcherThread extends Thread {

        private FileFilter mFileExtensionFilter;
        private int mChunkSize;
        private List<File> mBuffer;
        private Object mSync;
        private File mPath;
        private AtomicBoolean mStopped = new AtomicBoolean(false);

        public ImageFetcherThread(File path, int chunkSize, Object sync) {
            mPath = path;
            mChunkSize = chunkSize;
            mBuffer = new ArrayList<File>(chunkSize);
            mSync = sync;
            mFileExtensionFilter = FileUtils.createImagesFilter();
        }

        @Override
        public void run() {
            if (mPath != null) {
                fetchImages(mPath);
            }
            notifyFilesFetched();
            Log.d("111", "thread stop");
        }

        public void stopFetching() {
            mStopped.set(true);
        }

        private boolean isStopped() {
            return isInterrupted() || mStopped.get();
        }

        private void fetchImages(File directory) {
            if (!isStopped()) {

                synchronized (mSync) {

                    Log.d("111", "dir: " + directory.toString());

                    try {
                        File[] files = mFileExtensionFilter == null ? directory.listFiles() : directory.listFiles(mFileExtensionFilter);

                        if (!isStopped()) {

                            for (File file : files) {

                                if (!isStopped() && mBuffer.size() == mChunkSize) {
                                    notifyFilesFetched();
                                    Log.d("111", "wait");
                                    mSync.wait();
                                    mBuffer.clear();
                                }

                                if (!isStopped()) {
                                    if (file.isDirectory()) {
                                        fetchImages(file);
                                    } else {
                                        mBuffer.add(file);
                                    }
                                }
                                else {
                                    break;
                                }
                            }
                        }

                    } catch (InterruptedException e) {

                    }
                }
            }
        }

        private void notifyFilesFetched() {
            if (!isStopped()) {
                Message message = mNotifyHandler.obtainMessage();
                message.obj = mBuffer;
                message.sendToTarget();
            }
        }

    }

    private class FileListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mFiles.size();
        }

        @Override
        public Object getItem(int position) {
            return mFiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FileViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.file_list_item, null);
                holder = new FileViewHolder();
                holder.fileImage = (ImageView) convertView.findViewById(R.id.fileImage);
                holder.fileText = (TextView) convertView.findViewById(R.id.fileText);
                convertView.setTag(holder);
            }
            else {
                holder = (FileViewHolder) convertView.getTag();
            }

            File file = (File) getItem(position);
            holder.fileText.setText(file.getName());
            mImageLoader.loadThumbnail(file, holder.fileImage);

            return convertView;
        }
    }

    private static class FileViewHolder {
        ImageView fileImage;
        TextView fileText;
    }
}
