/*
 * Copyright (c) 2014 The Android Open Source Project
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

package org.mythtv.leanfront.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.leanback.widget.TitleViewAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.FetchVideoService;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.model.ListItem;
import org.mythtv.leanfront.model.MyHeaderItem;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.presenter.CardPresenter;
import org.mythtv.leanfront.presenter.IconHeaderItemPresenter;
import org.mythtv.leanfront.recommendation.UpdateRecommendationsService;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mCategoryRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 123; // Unique ID for Category Loader.
    private CursorObjectAdapter videoCursorAdapter;
//    private int mSelectedRow = -1;
//    private int mSelectedItem = -1;
    private int mType;
    public static final String KEY_TYPE = "LEANFRONT_TYPE";
    // Type applicable to main screen
    public static final int TYPE_TOPLEVEL = 1;
    // Types applicable to main screen or row
    public static final int TYPE_RECGROUP = 2;
    // Types applicable to main screen row, or cell
    public static final int TYPE_VIDEODIR = 3;
    // Types applicable to row or cell
    public static final int TYPE_SERIES = 4;
    // Types applicable to cell
    public static final int TYPE_EPISODE = 5;
    public static final int TYPE_VIDEO = 6;
    public static final int TYPE_TOP_ALL = 7;
    public static final int TYPE_RECGROUP_ALL = 8;
    // Special row type
    public static final int TYPE_SETTINGS = 20;
    public static final String KEY_BASENAME = "LEANFRONT_BASENAME";
    public static final String KEY_ROWNAME = "LEANFRONT_ROWNAME";
    public static final String KEY_ITEMNAME = "LEANFRONT_ITEMNAME";
    // mBase is the current recgroup or directory being displayed.
    private String mBaseName;
    private String mSelectedRowName;
    private int mSelectedRowType;
    private String mSelectedItemName;
    private int mSelectedItemType;
    private boolean mLoadStarted = false;

    private static ScheduledExecutorService executor = null;
    private MythTask mythTask = new MythTask();
    private long mLastLoadTime = 0;
    public static long mLoadNeededTime = System.currentTimeMillis();
    public static long mFetchTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mType = intent.getIntExtra(KEY_TYPE, TYPE_TOPLEVEL);
        if (mType != TYPE_TOPLEVEL) {
            mBaseName = intent.getStringExtra(KEY_BASENAME);
            mSelectedRowName = intent.getStringExtra(KEY_ROWNAME);
            mSelectedRowType = TYPE_SERIES;
            startLoader();
        }
    }


    // Fetch video list from MythTV into local database
    public void startFetch() {
        // Start an Intent to fetch the videos.
        Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
        getActivity().startService(serviceIntent);
    }

    // Load user interface from local database.
    public void startLoader() {
        if (!mLoadStarted) {
            Lifecycle.State state = getLifecycle().getCurrentState();
            if (state == Lifecycle.State.STARTED
               || state == Lifecycle.State.RESUMED) {
                mLoaderManager = LoaderManager.getInstance(this);
                mLoaderManager.restartLoader(CATEGORY_LOADER, null, this);
                mLoadStarted = true;
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();
        setupEventListeners();
        prepareEntranceTransition();

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.
        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mCategoryRowAdapter);

        updateRecommendations();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;
        if (executor != null)
            executor.shutdownNow();
        executor = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundTimer();
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(mythTask,0,240, TimeUnit.SECONDS);
        }
        // If it's been more than an hour, refresh
        if (mFetchTime < System.currentTimeMillis() - 60*60*1000)
            startFetch();
        else if (mLastLoadTime < mLoadNeededTime)
            startLoader();
    }

    @Override
    public void onPause() {
        int selectedRowNum = getSelectedPosition();
        mSelectedRowName = null;
        mSelectedRowType = -1;
        int selectedItemNum = -1;
        mSelectedItemName = null;
        mSelectedItemType = -1;
        if (selectedRowNum >= 0) {
            ListRow selectedRow = (ListRow) mCategoryRowAdapter.get(selectedRowNum);
            ListItem headerItem = (ListItem) selectedRow.getHeaderItem();
            mSelectedRowName = headerItem.getName();
            mSelectedRowType = headerItem.getItemType();
            ListRowPresenter.ViewHolder selectedViewHolder
                    = (ListRowPresenter.ViewHolder) getRowsSupportFragment()
                    .getRowViewHolder(selectedRowNum);
            if (selectedViewHolder != null)
                selectedItemNum = selectedViewHolder.getSelectedPosition();
            if (selectedItemNum >= 0) {
                ObjectAdapter itemAdapter = selectedRow.getAdapter();
                mSelectedItemName = ((ListItem) itemAdapter.get(selectedItemNum)).getName();
                mSelectedItemType = ((ListItem) itemAdapter.get(selectedItemNum)).getItemType();
            }
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        if (mType == TYPE_TOPLEVEL)
            setBadgeDrawable(
                    getActivity().getResources().getDrawable(R.drawable.mythtv_320x180_icon, null));
        setTitle(mBaseName);
        showTitle(TitleViewAdapter.FULL_VIEW_VISIBLE);
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.fastlane_background));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter();
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private void startBackgroundTimer() {
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY);
    }

    private void updateRecommendations() {
        Intent recommendationIntent = new Intent(getActivity(), UpdateRecommendationsService.class);
        getActivity().startService(recommendationIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String orderby =  VideoContract.VideoEntry.COLUMN_TITLE + ","
                +VideoContract.VideoEntry.COLUMN_AIRDATE  + ","
                +VideoContract.VideoEntry.COLUMN_STARTTIME;

        if (mType == TYPE_TOPLEVEL)
            orderby = VideoContract.VideoEntry.COLUMN_RECGROUP + "," + orderby;

        Loader ret = new CursorLoader(
                getContext(),
                VideoContract.VideoEntry.CONTENT_URI, // Table to query
                null, // Projection to return - null means return all fields
                null, // Selection clause
                null,  // Select based on the category id.
                orderby);
        // Map video results from the database to Video objects.
        videoCursorAdapter =
                new CursorObjectAdapter(new CardPresenter());
        videoCursorAdapter.setMapper(new VideoCursorMapper());
        return ret;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // the mLoadStarted check is needed becuase for some reason onLoadFinished
        // gets called every time the screen goes into the BG and this causes
        // the current selection and focus to be lost.
        if (data != null && mLoadStarted) {
            mLoadStarted = false;
            final int loaderId = loader.getId();
            if (loaderId == CATEGORY_LOADER) {
                int recgroupIndex =
                        data.getColumnIndex(VideoContract.VideoEntry.COLUMN_RECGROUP);
                int titleIndex =
                        data.getColumnIndex(VideoContract.VideoEntry.COLUMN_TITLE);
                int airdateIndex =
                        data.getColumnIndex(VideoContract.VideoEntry.COLUMN_AIRDATE);
                int starttimeIndex =
                        data.getColumnIndex(VideoContract.VideoEntry.COLUMN_STARTTIME);
                SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat dbTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                boolean cursorHasData = data.moveToFirst();
                int selectedRowNum = -1;
                int selectedItemNum = -1;

                // Every time we have to re-get the category loader, we must re-create the sidebar.
                mCategoryRowAdapter.clear();
                ArrayObjectAdapter objectAdapter = null;
                SparseArrayObjectAdapter allObjectAdapter = null;
                videoCursorAdapter.changeCursor(data);

                String currentCategory = null;
                String currentItem = null;
                int currentRowNum = -1;
                int allRowNum = -1;

                // Create row for "All\t"
                int allType = TYPE_RECGROUP_ALL;
                String allTitle = null;
                if (mType == TYPE_TOPLEVEL) {
                    allTitle = getString(R.string.all_title) + "\t";
                    allType = TYPE_TOP_ALL;
                }
                if (mType == TYPE_RECGROUP) {
                    allTitle = mBaseName + "\t";
                    allType = TYPE_RECGROUP_ALL;
                }
                if (mType == TYPE_TOPLEVEL || mType == TYPE_RECGROUP) {
                    MyHeaderItem header = new MyHeaderItem(allTitle,
                            allType);
                    allObjectAdapter = new SparseArrayObjectAdapter(new CardPresenter());
                    ListRow row = new ListRow(header, allObjectAdapter);
                    row.setContentDescription(allTitle);
                    mCategoryRowAdapter.add(row);
                    allRowNum = mCategoryRowAdapter.size() - 1;
                    if (mSelectedRowType == allType
                        && mSelectedRowName.equals(allTitle))
                        selectedRowNum = allRowNum;
                }

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (cursorHasData && !data.isAfterLast()) {

                    int categoryIndex = -1;
                    int itemType = -1;
                    int rowType = -1;

                    String recgroup = data.getString(recgroupIndex);

                    // For Rec Group type, only use recordings from that recording group.
                    // categories are titles.
                    if (mType == TYPE_RECGROUP) {
                        categoryIndex = titleIndex;
                        if ((getString(R.string.all_title) + "\t").equals(mBaseName)) {
                            // Do not mix deleted episodes in the All group
                            if ("Deleted".equals(recgroup)) {
                                data.moveToNext();
                                continue;
                            }
                        } else {
                            if (!mBaseName.equals(recgroup)) {
                                data.moveToNext();
                                continue;
                            }
                        }
                        rowType = TYPE_SERIES;
                        itemType = TYPE_EPISODE;
                    }

                    // For Top Level type, only use 1 recording from each title
                    // categories are recgroups
                    if (mType == TYPE_TOPLEVEL) {
                        categoryIndex = recgroupIndex;
                        String title = data.getString(titleIndex);
                        if (title.equals(currentItem)) {
                            data.moveToNext();
                            continue;
                        }
                        currentItem = title;
                        rowType = TYPE_RECGROUP;
                        itemType = TYPE_SERIES;
                    }
                    String category = data.getString(categoryIndex);

                    // Change of row
                    if (!category.equals(currentCategory)) {
                        // Finish off prior row
                        if (objectAdapter != null) {
                            // Create header for this category.
                            MyHeaderItem header = new MyHeaderItem(currentCategory,
                                    rowType);
                            ListRow row = new ListRow(header, objectAdapter);
                            row.setContentDescription(currentCategory);
                            mCategoryRowAdapter.add(row);
                        }
                        currentRowNum = mCategoryRowAdapter.size();
                        objectAdapter = new ArrayObjectAdapter(new CardPresenter());
                        currentCategory = category;
                        if (mSelectedRowType == rowType
                                && currentCategory.equals(mSelectedRowName))
                            selectedRowNum = currentRowNum;
                    }
                    Video video = (Video) videoCursorAdapter.get(data.getPosition());
                    video.type = itemType;
                    objectAdapter.add(video);
                    if (selectedRowNum == currentRowNum) {
                        if (video.getItemType() == mSelectedItemType
                            && video.getName().equals(mSelectedItemName))
                            selectedItemNum = objectAdapter.size() - 1;
                    }

                    if (allObjectAdapter != null) {
                        int position;
                        String dateStr = data.getString(starttimeIndex);
                        try {
                            Date date = dbTimeFormat.parse(dateStr);
                            // 525960 minutes in a year
                            // Get position as number of minutes since 1970
                            position = (int) (date.getTime() / 60000L);
                            // Add 70 years in case it is before 1970
                            position += 36817200;
                        } catch (ParseException | NullPointerException e) {
                            e.printStackTrace();
                            position = 0;
                        }
                        // Make sure we have an empty slot
                        try {
                            while (allObjectAdapter.lookup(position) != null)
                                position++;
                        } catch (ArrayIndexOutOfBoundsException e) { }

                        allObjectAdapter.set(position,video);

                        if (selectedRowNum == allRowNum) {
                            if (video.getItemType() == mSelectedItemType
                                    && video.getName().equals(mSelectedItemName))
                                selectedItemNum = position;
                        }
                    }

                    data.moveToNext();
                }
                // Finish off prior row
                if (objectAdapter != null) {
                    // Create header for this category.
                    MyHeaderItem header = new MyHeaderItem(currentCategory,
                            TYPE_RECGROUP);
                    ListRow row = new ListRow(header, objectAdapter);
                    mCategoryRowAdapter.add(row);
                }

                // Create a row for this special case with more samples.
                MyHeaderItem gridHeader = new MyHeaderItem(getString(R.string.personal_settings),
                        TYPE_SETTINGS);
                CardPresenter presenter = new CardPresenter();
                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(presenter);
                ListRow row = new ListRow(gridHeader, gridRowAdapter);
                mCategoryRowAdapter.add(row);

                if (selectedRowNum == allRowNum)
                    selectedItemNum = allObjectAdapter.indexOf(selectedItemNum);


                SelectionSetter setter = new SelectionSetter(selectedRowNum,selectedItemNum);

                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(setter, 100);

            }
            mLastLoadTime = System.currentTimeMillis();
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int loaderId = loader.getId();
        if (loaderId == CATEGORY_LOADER)
            mCategoryRowAdapter.clear();
    }

    public int getType() {
        return mType;
    }

    private class UpdateBackgroundTask implements Runnable {
        @Override
        public void run() {
            if (mBackgroundURI != null) {
                updateBackground(mBackgroundURI.toString());
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            ListItem li = (ListItem) item;
            int liType = li.getItemType();
            Activity context = getActivity();
            Bundle bundle;
            switch (liType) {
                case TYPE_EPISODE:
                case TYPE_VIDEO:
                    Video video = (Video) item;
                    Intent intent = new Intent(context, VideoDetailsActivity.class);
                    intent.putExtra(VideoDetailsActivity.VIDEO, video);

                    bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            (Activity)context,
                            ((ImageCardView) itemViewHolder.view).getMainImageView(),
                            VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                    getActivity().startActivity(intent, bundle);
                    break;
                case TYPE_SERIES:
                    MyHeaderItem headerItem = (MyHeaderItem) row.getHeaderItem();
                    intent = new Intent(context, MainActivity.class);
                    intent.putExtra(KEY_TYPE,MainFragment.TYPE_RECGROUP);
                    intent.putExtra(KEY_BASENAME,headerItem.getName());
                    intent.putExtra(KEY_ROWNAME,((Video)li).title);
                    bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation((Activity)context)
                                    .toBundle();
                    context.startActivity(intent, bundle);
                    break;
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Video) {
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);
                startBackgroundTimer();
            }

        }
    }

    private static class MythTask implements Runnable{
        boolean mVersionMessageShown = false;
        @Override
        public void run() {
            boolean connection = false;
            boolean connectionfail = false;
            while (!connection) {
                int toastMsg = 0;
                int toastLeng = 0;
                try {
                    if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState()
                            == Lifecycle.State.CREATED) {
                        // process is now in the background
                        return;
                    }
                    String result = null;
                    String url = XmlNode.mythApiUrl(
                            "/Myth/DelayShutdown");
                    if (url == null)
                        return;
                    XmlNode bkmrkData = XmlNode.fetch(url, "POST");
                    result = bkmrkData.getString();
                    connection = true;
                } catch (SocketException e) {
                    toastMsg = R.string.msg_no_connection;
                    toastLeng = Toast.LENGTH_LONG;
                    connectionfail = true;
                } catch (FileNotFoundException e) {
                    if (!mVersionMessageShown) {
                        if (!mVersionMessageShown) {
                            toastMsg = R.string.msg_no_delayshutdown;
                            toastLeng = Toast.LENGTH_LONG;
                            mVersionMessageShown = true;
                        }
                        connection = true;
                    }
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                }
                if (toastMsg != 0) {
                    Activity activity = MainActivity.getContext();
                    if (activity == null)
                        return;
                    ToastShower toastShower = new ToastShower(activity, toastMsg, toastLeng);
                    activity.runOnUiThread(toastShower);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e2) {
                    }
                }
            }
            if (connectionfail) {
                Activity activity = MainActivity.getContext();
                if (activity == null)
                    return;
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.getContext().getMainFragment().startFetch();
                    }
                });
            }
        }
    }
    private static class ToastShower implements Runnable {

        private Activity activity;
        private int toastMsg;
        private int toastLeng;

        public ToastShower(Activity activity, int toastMsg, int toastLeng) {
            this.activity = activity;
            this.toastMsg = toastMsg;
            this.toastLeng = toastLeng;
        }
        public void run() {
            // show toast here
            Toast.makeText(activity,
                    activity.getString(toastMsg), toastLeng)
                    .show();
        }
    }

    private class SelectionSetter implements Runnable {

        private int selectedRowNum;
        private int selectedItemNum;

        public SelectionSetter(int selectedRowNum, int selectedItemNum) {
            this.selectedRowNum = selectedRowNum;
            this.selectedItemNum = selectedItemNum;
        }
        public void run() {
            if (selectedRowNum != -1) {
                if (selectedItemNum == -1)
                    setSelectedPosition(selectedRowNum);
                else
                    getRowsSupportFragment().setSelectedPosition(selectedRowNum, false,
                            new ListRowPresenter.SelectItemViewHolderTask(selectedItemNum));
            }

        }
    }

}
