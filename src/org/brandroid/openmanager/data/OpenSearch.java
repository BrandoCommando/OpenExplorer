package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.utils.Logger;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

public class OpenSearch extends OpenPath
{
	private final String mQuery;
	private final OpenPath mBasePath;
	private final List<OpenPath> mResultsArray;
	private final SearchProgressUpdateListener mListener;
	private Thread mSearchThread = null;
	private boolean mCancelled = false;
	private boolean mFinished = false;
	private int mSearchedDirs = 0;
	private long mLastUpdate = 0;
	private int mLastSent = 0;
	private long mStart = 0;
	private final boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && true;

	public OpenSearch(String query, OpenPath base, SearchProgressUpdateListener listener)
	{
		mResultsArray = new ArrayList<OpenPath>();
		mQuery = query;
		mBasePath = base;
		mListener = listener;
	}
	public OpenSearch(String query, OpenPath base, SearchProgressUpdateListener listener, ArrayList<Parcelable> results)
	{
		mResultsArray = new ArrayList<OpenPath>();
		mQuery = query;
		mBasePath = base;
		mListener = listener;
		for(Parcelable p : results)
			mResultsArray.add(FileManager.getOpenCache(p.toString()));
	}
	
	public interface SearchProgressUpdateListener
	{
		void onAddResults(OpenPath[] results);
		void onUpdate();
		void onFinish();
	}
	
	public void cancel() { mCancelled = true; }
	
	public boolean isRunning() { return mCancelled || mFinished ? false : true; }
	
	public void start() throws IOException
	{
		mStart = new Date().getTime();
		if(DEBUG)
			Logger.LogDebug("OpenSearch.start()");
		
		if(mSearchThread != null && mSearchThread.isAlive())
			mSearchThread.interrupt();
		
		if(Thread.currentThread().equals(OpenExplorer.UiThread))
			throw new IOException("Please run from non-UI thread!");
		
		if(DEBUG)
			Logger.LogDebug("OpenSearch started!");
		SearchWithin(mBasePath);
		mFinished = true;
		mListener.onUpdate();
		mListener.onFinish();
		if(DEBUG)
			Logger.LogDebug("OpenSearch finished!");
	}
	private void SearchWithin(OpenPath dir)
	{
		if(dir == null) return;
		mSearchedDirs++;
		OpenPath[] kids = null;
		try {
			kids = dir.listFiles();
		} catch(IOException e) { return; }
		for(OpenPath kid : kids)
		{
			if(mCancelled) return;
			if(kid == null || !kid.exists()) continue;
			if(kid.getName() == null) continue;
			if(isMatch(kid.getName().toLowerCase(), getQuery().toLowerCase()))
				if(!mResultsArray.contains(kid))
					mResultsArray.add(kid);
			if(new Date().getTime() - mLastUpdate > 500)
			{
				publishProgress();
				try {
					// It appears that no matter which thread this is run on,
					// we need to explicitly sleep in order to not block the UI thread
					Thread.sleep(10);
				} catch(InterruptedException e) { }
			}
			if(kid.isDirectory() && !mCancelled)
				SearchWithin(kid);
		}
	}
	
	public void publishProgress()
	{
		int sz = mResultsArray.size();
		if(sz > mLastSent)
		{
			int cnt = sz - mLastSent;
			OpenPath[] toSend = mResultsArray.subList(mLastSent, sz).toArray(new OpenPath[cnt]);
			mLastSent = sz;
			mListener.onAddResults(toSend);
		}
		mListener.onUpdate();
	}

	private boolean isMatch(String a, String b)
	{
		return a.toLowerCase().indexOf(b.toLowerCase()) > -1 ? true : false;
	}
	
	@Override
	public boolean showChildPath() {
		return true;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "\"" + mQuery + "\" (" + mResultsArray.size() + ")";
	}

	@Override
	public String getPath() {
		return getUri().toString();
	}
	
	public OpenPath getBasePath() { return mBasePath; }
	public String getQuery() { return mQuery; }
	public final List<OpenPath> getResults() { return mResultsArray; }

	@Override
	public String getAbsolutePath() {
		return getUri().toString();
	}

	@Override
	public void setPath(String path) { }

	@Override
	public long length() {
		return mResultsArray.size();
	}

	@Override
	public OpenPath getParent() {
		return null;
	}

	@Override
	public OpenPath getChild(String name) {
		for(OpenPath kid : mResultsArray)
			if(kid.getName().equalsIgnoreCase(name))
				return kid;
		return null;
	}

	@Override
	public OpenPath[] list() throws IOException {
		return mResultsArray.toArray(new OpenPath[(int)length()]);
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		start();
		return mResultsArray.toArray(new OpenPath[(int)length()]);
	}

	@Override
	public Boolean isDirectory() {
		return true;
	}

	@Override
	public Boolean isFile() {
		return false;
	}

	@Override
	public Boolean isHidden() {
		return false;
	}

	@Override
	public Uri getUri() {
		return Uri.parse("content://org.brandroid.openmanager/search/" +
				Uri.encode(mQuery) +
				(mBasePath != null ? "/" + mBasePath.getPath() : ""));
	}

	@Override
	public Long lastModified() {
		return mStart;
	}

	@Override
	public Boolean canRead() {
		return true;
	}

	@Override
	public Boolean canWrite() {
		return false;
	}

	@Override
	public Boolean canExecute() {
		return false;
	}

	@Override
	public Boolean exists() {
		return true;
	}

	@Override
	public Boolean requiresThread() {
		return true;
	}

	@Override
	public Boolean delete() {
		return false;
	}

	@Override
	public Boolean mkdir() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

	@Override
	public void clearChildren()
	{
		mResultsArray.clear();
		//start();
		mListener.onUpdate();
	}
}
