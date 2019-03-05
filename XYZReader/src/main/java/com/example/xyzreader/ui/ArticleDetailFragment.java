package com.example.xyzreader.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final int LONG_TEXT = 1;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private ImageView mImageToolBar;
    private FloatingActionButton mFloatingActionButton;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mToolbar;

    private TextView mTitleView;
    private TextView mBylineView;
    private TextView mBodyView;
    private ProgressBar mProgressBarBody;

    /**
     * Reference code:
     * https://github.com/larissacazi/MaterialDesignProject/blob/master/xyz-reader-starter-code/XYZReader/src/main/java/com/example/xyzreader/ui/ArticleDetailActivity.java
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case LONG_TEXT:
                    mBodyView.setText(msg.obj.toString());
                    showProgressLoading(false);
                    break;
            }
        }
    };


    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static android.support.v4.app.Fragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);

    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);

        getActivityCast().supportPostponeEnterTransition();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mAppBarLayout = mRootView.findViewById(R.id.appbar_article_detail);
        mCollapsingToolbarLayout = mRootView.findViewById(R.id.collapsing_toolbar_layout_detail);
        mImageToolBar = mRootView.findViewById(R.id.toolbar_image_view);
        mToolbar = mRootView.findViewById(R.id.toolbar_article_detail);
        mFloatingActionButton = mRootView.findViewById(R.id.share_fab);
        mFloatingActionButton.setOnClickListener(getClickForFloatingButton(getActivityCast()));

        mTitleView = mRootView.findViewById(R.id.article_detail_title);
        mBylineView = mRootView.findViewById(R.id.article_detail_byline);
        mBodyView = mRootView.findViewById(R.id.article_detail_body);
        mProgressBarBody = mRootView.findViewById(R.id.pb_loading_article_detail_body);

        bindViews();
        return mRootView;
    }

    private void updateStatusBar(int color) {
        mRootView.findViewById(R.id.meta_bar)
                .setBackgroundColor(color);
        if (mAppBarLayout != null && mCollapsingToolbarLayout != null) {
            mAppBarLayout.setBackgroundColor(color);
            mCollapsingToolbarLayout.setContentScrimColor(color);
            mCollapsingToolbarLayout.setStatusBarScrimColor(color);
        }
        Window window = getActivity().getWindow();
        window.setStatusBarColor(color);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        showProgressLoading(true);
        mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mBylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                mBylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            /**
             * Reference code:
             * https://github.com/larissacazi/MaterialDesignProject/blob/master/xyz-reader-starter-code/XYZReader/src/main/java/com/example/xyzreader/ui/ArticleDetailActivity.java
             */
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String body = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")).toString();
                    mHandler.sendMessage(mHandler.obtainMessage(LONG_TEXT, body));
                }
            });
            thread.start();

            Picasso.with(getActivityCast()).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .into(mImageToolBar, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable)mImageToolBar.getDrawable()).getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                updateStatusBar(mMutedColor);
                            }
                            getActivityCast().supportStartPostponedEnterTransition();
                        }

                        @Override
                        public void onError() {
                            getActivityCast().supportStartPostponedEnterTransition();
                        }
                    });

            if(mToolbar != null){
                mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivityCast().onBackPressed();
                    }
                });
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
//        mCursor = null;
//        bindViews();
    }

    public View.OnClickListener getClickForFloatingButton(final Activity activity){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(activity)
                        .setType("text/plain")
                        //TODO: change shared text
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        };
    }

    public void showProgressLoading(boolean show){
        if(show){
            mBodyView.setVisibility(View.GONE);
            mProgressBarBody.setVisibility(View.VISIBLE);
        } else {
            mBodyView.setVisibility(View.VISIBLE);
            mProgressBarBody.setVisibility(View.GONE);
        }
    }
}
