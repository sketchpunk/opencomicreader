package com.sketchpunk.ocomicreader.lib;

import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.opengles.GL10;

import com.sketchpunk.ocomicreader.ui.ComicPageView;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.opengl.GLES10;
import android.preference.PreferenceManager;
import android.view.Display;
import android.widget.Toast;

public class ComicLoader implements PageLoader.CallBack {// LoadImageView.OnImageLoadingListener,LoadImageView.OnImageLoadedListener{
	public static interface CallBack {
		public void onPageLoaded(boolean isSuccess, int currentPage);
	}

	public static iComicArchive getArchiveInstance(String path) {
		String ext = sage.io.Path.getExt(path).toLowerCase(Locale.getDefault());
		iComicArchive o = null;

		if (ext.equals("zip") || ext.equals("cbz")) {
			o = new ComicZip();
			if (o.loadFile(path))
				return o;
			o.close();
		} else if (ext.equals("rar") || ext.equals("cbr")) {
			o = new ComicRar();
			if (o.loadFile(path))
				return o;
			o.close();
		} else {
			if (new File(path).isDirectory()) {
				o = new ComicFld();
				if (o.loadFile(path))
					return o;
			}
		}

		return null;
	}

	private int mPageLen, mCurrentPage;
	private int mMaxSize;
	private CallBack mCallBack;
	private boolean mIsPreloading;

	private ComicPageView mImageView;
	private PageLoader mPageLoader;
	private iComicArchive mArchive;
	private List<String> mPageList;
	private Bitmap mCurrentBmp = null, mNextBmp = null, mPrevBmp = null;
	private Context mContext = null;

	public ComicLoader(Context context, ComicPageView o) {
		mImageView = o;
		mContext = context;

		// Save Callback
		if (context instanceof CallBack)
			mCallBack = (CallBack) context;

		// Get preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		mIsPreloading = prefs.getBoolean("preLoading", false);

		// Get the window size
		Display display = ((Activity) context).getWindowManager()
				.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		mPageLoader = new PageLoader();
		mCurrentPage = -1;

		int[] maxTextureSize = new int[1];
		GLES10.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

		mMaxSize = maxTextureSize[0]; // MaxTextureSize
	}

	/*
	 * Getters Since the event has been created, these getters plus the
	 * variables won't be needed anymore.
	 */
	public int getCurrentPage() {
		return mCurrentPage;
	}

	public int getPageCount() {
		return mPageLen;
	}

	/* Methods */
	public boolean close() {
		try {
			mPageLoader.close(); // cancel any tasks that may be running.

			if (mArchive != null) {
				mArchive.close();
				mArchive = null;
			}

			if (mCurrentBmp != null) {
				mImageView.setImageBitmap(null);
				mCurrentBmp.recycle();
				mCurrentBmp = null;
			}

			if (mNextBmp != null) {
				mNextBmp.recycle();
				mNextBmp = null;
			}

			if (mPrevBmp != null) {
				mPrevBmp.recycle();
				mPrevBmp = null;
			}

			mCallBack = null;
			mImageView = null;
			return true;
		} catch (Exception e) {
			System.out.println("Error closing archive " + e.getMessage());
		}

		return false;
	}

	// Load a list of images in the archive file, need path to stream out the
	// file.
	public boolean loadArchive(String path) {
		try {
			mArchive = ComicLoader.getArchiveInstance(path);
			if (mArchive == null)
				return false;

			// Get page list
			mPageList = mArchive.getPageList();
			if (mPageList != null) {
				mPageLen = mPageList.size();
				return true;
			}

			// if non found, then just close the archive.
			mArchive.close();
			mArchive = null;
		} catch (Exception e) {
			System.err.println("LoadArchive " + e.getMessage());
		}

		return false;
	}

	public void refreshOrientation() {
		Display display = ((Activity) mContext).getWindowManager()
				.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
	}

	/* Paging Methods */
	public int gotoPage(int pos) {
		if (pos < 1 || pos > mPageLen || pos == mCurrentPage + 1)
			return 0;

		// Arrays start at 0, but pages start at 1.
		if (pos == mCurrentPage + 1)
			return nextPage();
		else if (pos == mCurrentPage - 1)
			return prevPage();
		else {
			mCurrentPage = pos - 1; // Page to Index Conversion
			mPageLoader.loadImage((PageLoader.CallBack) this,
					mPageList.get(mCurrentPage), mMaxSize, mArchive, 0);
		}
		return 1;
	}

	public int nextPage() {
		if (mCurrentPage + 1 >= mPageLen)
			return 0;

		if (!mIsPreloading) {
			mCurrentPage++;
			mPageLoader.loadImage((PageLoader.CallBack) this,
					mPageList.get(mCurrentPage), mMaxSize, mArchive, 0);
		} else {
			// mPageLoader.cancelTask(); //Cancel any loading to prevent any
			// bitmaps from changing.
			if (mPageLoader.isLoading())
				return -1;

			mCurrentPage++;
			if (mPrevBmp != null) {
				mPrevBmp.recycle();
				mPrevBmp = null;
			} // Clean up prev image
			if (mCurrentBmp != null)
				// save current to prev in case user wants to go back one
				mPrevBmp = mCurrentBmp;

			// if we have a next image, set view to that and preload next.
			if (mNextBmp != null) {
				mCurrentBmp = mNextBmp;
				mNextBmp = null;

				if (mCurrentPage + 1 < mPageLen)
					mPageLoader.loadImage((PageLoader.CallBack) this,
							mPageList.get(mCurrentPage + 1), mMaxSize,
							mArchive, 1);
				mImageView.setImageBitmap(mCurrentBmp);

				if (mCallBack != null)
					mCallBack.onPageLoaded(true, mCurrentPage + 1);
			} else {
				mPageLoader.loadImage((PageLoader.CallBack) this,
						mPageList.get(mCurrentPage), mMaxSize, mArchive, 0);
			}
		}

		return 1;
	}

	public int prevPage() {
		if (mCurrentPage - 1 < 0)
			return 0;

		if (!mIsPreloading) {
			mCurrentPage--;
			mPageLoader.loadImage((PageLoader.CallBack) this,
					mPageList.get(mCurrentPage), mMaxSize, mArchive, 0);
		} else {
			if (mPageLoader.isLoading())
				return -1;

			mCurrentPage--;
			if (mNextBmp != null) {
				mNextBmp.recycle();
				mNextBmp = null;
			} // Clean up next image
			if (mCurrentBmp != null)
				// save current to next in case user wants to go forward
				mNextBmp = mCurrentBmp;

			// if we have a prev image, set view to that and preload prev.
			if (mPrevBmp != null) {
				mCurrentBmp = mPrevBmp;
				mPrevBmp = null;

				if (mCurrentPage - 1 >= 0)
					mPageLoader.loadImage((PageLoader.CallBack) this,
							mPageList.get(mCurrentPage - 1), mMaxSize,
							mArchive, 2);
				mImageView.setImageBitmap(mCurrentBmp);

				if (mCallBack != null)
					mCallBack.onPageLoaded(true, mCurrentPage + 1);
			} else {
				mPageLoader.loadImage((PageLoader.CallBack) this,
						mPageList.get(mCurrentPage), mMaxSize, mArchive, 0);
			}
		}

		return 1;
	}

	/* Image Loading */
	@Override
	public void onImageLoaded(String errMsg, Bitmap bmp, int imgType) {
		if (errMsg != null) {
			Toast.makeText(mContext, errMsg, Toast.LENGTH_LONG).show();
		}

		// if we have a new image and an old image.
		if (bmp != null) {
			switch (imgType) {
			case 0:
				if (mCurrentBmp != null) {
					mCurrentBmp.recycle();
					mCurrentBmp = null;
				}
				mCurrentBmp = bmp;
				mImageView.setImageBitmap(mCurrentBmp);
				if (mCallBack != null)
					mCallBack.onPageLoaded((bmp != null), mCurrentPage + 1);

				if (mIsPreloading && mCurrentPage + 1 < mPageLen) {
					mPageLoader.loadImage((PageLoader.CallBack) this,
							mPageList.get(mCurrentPage + 1), mMaxSize,
							mArchive, 1);
				}// if

				break;
			case 1:
				if (mNextBmp != null) {
					mNextBmp.recycle();
					mNextBmp = null;
				}
				mNextBmp = bmp;

				// Preload prev if not available.
				if (mIsPreloading && mCurrentPage - 1 >= 0 && mPrevBmp == null) {
					mPageLoader.loadImage((PageLoader.CallBack) this,
							mPageList.get(mCurrentPage - 1), mMaxSize,
							mArchive, 2);
				}

				break;
			case 2:
				if (mPrevBmp != null) {
					mPrevBmp.recycle();
					mPrevBmp = null;
				}
				mPrevBmp = bmp;
				break;
			}
		}

		bmp = null;
	}
}
