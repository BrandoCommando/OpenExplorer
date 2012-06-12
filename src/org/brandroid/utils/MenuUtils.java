package org.brandroid.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.menu;
import org.brandroid.openmanager.activities.OpenExplorer;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.TouchDelegate;
import android.widget.Button;
import android.widget.ImageButton;

public class MenuUtils {
	public static final boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && false;
	
	public static void transferMenu(Menu from, Menu to) { transferMenu(from, to, true); }
	public static void transferMenu(Menu from, Menu to, Boolean clearFrom) {
		if(from == null || to == null) return;
		to.clear();
		for(int i=0; i<from.size(); i++)
			transferMenu(from.getItem(i), to);
		if(clearFrom)
			from.clear();
	}
	public static MenuItem transferMenu(MenuItem item, Menu to)
	{
		if(!item.isVisible()) return null;

		MenuItem newm = to.add(item.getGroupId(), item.getItemId(), item.getOrder(), item.getTitle())
			.setEnabled(item.isEnabled())
			.setCheckable(item.isCheckable())
			.setChecked(item.isChecked())
			.setVisible(item.isVisible())
			.setNumericShortcut(item.getNumericShortcut())
			.setAlphabeticShortcut(item.getAlphabeticShortcut())
			.setIcon(item.getIcon());
		
		if(item.hasSubMenu())
			transferMenu(item.getSubMenu(),
				newm.getSubMenu(), false);
		
		return newm;
	}
	public static void setMenuChecked(Menu menu, boolean checked, int toCheck, int... toOppose)
	{
		for(int id : toOppose)
			if(menu.findItem(id) != null)
				menu.findItem(id).setChecked(!checked);
		if(menu.findItem(toCheck) != null)
			menu.findItem(toCheck).setChecked(checked);
	}

	public static void setMenuVisible(Menu menu, boolean visible, int... ids)
	{
		if(menu == null) return;
		if(ids.length == 0)
			setMenuVisible(menu, visible, getMenuIDs(menu));
		for(int id : ids)
			if(menu.findItem(id) != null && !visible)
				menu.removeItem(id);
			else if(menu.findItem(id) != null && visible)
				menu.findItem(id).setVisible(visible);
			else for(int i=0; i<menu.size(); i++)
				if(menu.getItem(i).hasSubMenu())
					setMenuVisible(menu.getItem(i).getSubMenu(), visible, ids);
	}
	public static void setMenuShowAsAction(Menu menu, int show, int... ids)
	{
		if(OpenExplorer.BEFORE_HONEYCOMB) return;
		for(int id : ids)
			if(menu.findItem(id) != null)
			{
				MenuItem item = (MenuItem)menu.findItem(id);
				MenuItemCompat.setShowAsAction(item, show);
			}
		/*
		for(int id : ids)
			if(menu.findItem(id) != null)
			{
				MenuItem item = menu.findItem(id);
				item.setShowAsAction(show);
			}*/
	}
	
	public static int[] getMenuIDs(Menu menu)
	{
		int[] ret = new int[menu.size()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = menu.getItem(i).getItemId();
		return ret;
	}
	
	public static void setMenuEnabled(Menu menu, boolean enabled, int... ids)
	{
		if(ids.length == 0)
			setMenuEnabled(menu, enabled, getMenuIDs(menu));
		for(int id : ids)
		{
			MenuItem item = menu.findItem(id);
			if(item == null) continue;
			item.setEnabled(enabled);
			if(enabled)
				item.setVisible(true);
		}
	}
	public static void mergeDuplicateMenus(Menu menu)
	{
		ArrayList<Integer> holder = new ArrayList<Integer>();
		//ArrayList<Integer> buffer = new ArrayList<int>();
		for(int i = menu.size() - 1; i >= 0; i--)
		{
			MenuItem item = menu.getItem(i);
			if(holder.contains(item.getItemId())) {
				
			}
		}
	}
	public static void fillSubMenus(int[] search, int[] replace, Menu menu, MenuInflater inflater)
	{
		if(search == null || replace == null || menu == null || inflater == null) return;
		for(int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			try {
				int index = Utils.getArrayIndex(search, item.getItemId());
				if(index > -1 && item.getSubMenu() != null)
				{
					inflater.inflate(replace[index], item.getSubMenu());
					//Logger.LogDebug("Inflating 0x" + Integer.toHexString(replace[index]) + " to " + item.getTitle());
				}
			} catch(Exception e) { Logger.LogWarning("Couldn't fill submenu (0x" + Integer.toHexString(item.getItemId()) + ")"); }
		}
	}
	public static int countVisibleMenus(Menu menu) {
		int ret = 0;
		for(int i = 0; i < menu.size(); i++)
			if(menu.getItem(i).isVisible())
				ret++;
		return ret;
	}
	public static void hideMenuGrandChildren(MenuBuilder menu) {
		for(int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			if(item.hasSubMenu() && item.getSubMenu() != null && item.getSubMenu().size() > 0)
			{
				item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						return true;
					}
				});
				Menu sub = item.getSubMenu();
				sub.clear();
			}
		}
	}
	public static void scanMenuShortcuts(Menu menu)
	{
		for(int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			if(item.hasSubMenu()) scanMenuShortcuts(item.getSubMenu());
			if(item.getAlphabeticShortcut() <= 0) continue;
			//if(OpenExplorer.mMenuShortcuts.get(item.getAlphabeticShortcut()) != null)
			//	break;
			int sc = item.getAlphabeticShortcut() - ('a' - KeyEvent.KEYCODE_A);
			if(item.getAlphabeticShortcut() == '*')
				sc = KeyEvent.KEYCODE_DEL;
			// KeyEvent.KEYCODE_A = 27, Ascii for 'a' is 97, so add 80 to map to ascii
			if(DEBUG)
				Logger.LogDebug("Menu Shortcut (" + item.getAlphabeticShortcut() + ":" + sc + ") = " + item);
			OpenExplorer.mMenuShortcuts.put(sc, item);
		}
	}
	public static int[] getMenuShortcuts(Context context)
	{
		if(OpenExplorer.mMenuShortcuts == null) {
			OpenExplorer.mMenuShortcuts = new SparseArray<MenuItem>();
			MenuBuilder menu = new MenuBuilder(context);
			MenuUtils.scanMenuShortcuts(menu, new MenuInflater(context));
		}
		int[] ret = new int[OpenExplorer.mMenuShortcuts.size()];
		for(int i = 0; i < OpenExplorer.mMenuShortcuts.size(); i++)
			ret[i] = OpenExplorer.mMenuShortcuts.keyAt(i);
		return ret;
	}
	public static MenuItem getMenuShortcut(KeyEvent event)
	{
		if(OpenExplorer.mMenuShortcuts == null) return null;
		int keyCode = event.getKeyCode();
		if(event.isAltPressed())
			keyCode = event.getUnicodeChar();
		return OpenExplorer.mMenuShortcuts.get(keyCode);
	}
	public static MenuItem getMenuShortcut(int keyCode)
	{
		if(OpenExplorer.mMenuShortcuts == null) return null;
		return OpenExplorer.mMenuShortcuts.get(keyCode);
	}
	public final static int[] MENU_LOOKUP_IDS = new int[]{};//	R.id.menu_view,		R.id.menu_sort,		R.id.menu_content_ops,	R.id.content_paste,R.id.menu_text_view,	R.id.menu_text_ops};
	public final static int[] MENU_LOOKUP_SUBS = new int[]{};//	R.menu.content_view,R.menu.content_sort,R.menu.content_ops,		R.menu.multiselect,R.menu.text_view,	R.menu.text_file};
	public static void fillSubMenus(Menu menu, MenuInflater inflater)
	{
		//if(!(menu instanceof MenuBuilder))
		if(!OpenExplorer.USE_PRETTY_MENUS)
		{
			fillSubMenus(MENU_LOOKUP_IDS, MENU_LOOKUP_SUBS, menu, inflater);
		}
	}
	public static void scanMenuShortcuts(Menu menu, MenuInflater inflater)
	{
		if(OpenExplorer.mMenuShortcuts != null) return;
		OpenExplorer.mMenuShortcuts = new SparseArray<MenuItem>();
		for(int menuId : new int[]{R.menu.global,R.menu.content,R.menu.content_sort,R.menu.content_view,R.menu.content_ops,R.menu.text_editor,R.menu.text_file,R.menu.text_view,R.menu.multiselect,R.menu.context_file})
		{
			menu.clear();
			inflater.inflate(menuId, menu);
			scanMenuShortcuts(menu);
		}
	}

    public static int getMenuLookupID(int id)
	{
		return Utils.getArrayIndex(MenuUtils.MENU_LOOKUP_IDS, id);
	}

	public static int getMenuLookupSub(int index)
	{
		int index2 = getMenuLookupID(index);
		if(index2 > -1)
			index = index2;
		if(index > -1 && index < MenuUtils.MENU_LOOKUP_SUBS.length)
			return MenuUtils.MENU_LOOKUP_SUBS[index];
		else return Utils.getArrayIndex(MenuUtils.MENU_LOOKUP_SUBS, index);
	}
	
}
