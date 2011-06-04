/*
    Open Manager For Tablets, an open source file manager for the Android system
    Copyright (C) 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.nexes.manager.tablet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.io.File;

public class ThumbnailCreator {
	private int mWidth;
	private int mHeight;
	private ArrayList<Bitmap> mCacheBitmap;
	private SoftReference<Bitmap> mThumb;

	public ThumbnailCreator(int width, int height) {
		mWidth = width;
		mHeight = height;
		mCacheBitmap = new ArrayList<Bitmap>();
	}
	
	public Bitmap hasBitmapCached(int index) {
		if(mCacheBitmap.isEmpty())
			return null;
		
		try {
			return mCacheBitmap.get(index);
			
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			return null;
		}		
	}
	
	public void clearBitmapCache() {
		mCacheBitmap.clear();
	}
	
		
	public void setBitmapToImageView(final String imageSrc, final Handler handle, final int pos) {
		
		final File file = new File(imageSrc);
		
		if(mCacheBitmap.size() > pos && mCacheBitmap.get(pos) != null)
			return;
		
		Thread thread = new Thread() {
			public void run() {
				synchronized (this) {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 16;
					
					mThumb = (file.length() > 100000) ?
							 new SoftReference<Bitmap>(BitmapFactory.decodeFile(imageSrc, options)) : 
							 new SoftReference<Bitmap>(Bitmap.createScaledBitmap(
									 						  BitmapFactory.decodeFile(imageSrc),
									 						  mWidth,
									 						  mHeight,
									 						  false));
					mCacheBitmap.add(mThumb.get());
					
					handle.post(new Runnable() {
						public void run() {
							Message msg = handle.obtainMessage();
							msg.obj = mThumb.get();
							msg.sendToTarget();
						}
					});
				}
			}
		};
		
		thread.start();
	}
}