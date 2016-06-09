/*
 * Copyright (c) 2013-2015 by appPlant UG. All rights reserved.
 *
 * @APPPLANT_LICENSE_HEADER_START@
 *
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apache License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://opensource.org/licenses/Apache-2.0/ and read it before using this
 * file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 *
 * @APPPLANT_LICENSE_HEADER_END@
 */

package de.appplant.cordova.plugin.notification;

import android.app.Service;
import android.app.Application;
import android.app.ActivityManager;
import android.app.ActivityManager.AppTask;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;

/**
 * Abstract content receiver service for local notifications. Creates the
 * local notification and calls the event functions for further proceeding.
 */
abstract public class AbstractClickService extends Service {

    // Holds identifier of action most recently chosen  
    // Null if notification was simply clicked
    public String actionIdentifier = null;
    protected JSONObject actionObj = null;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        AbstractClickService getService() {
            return AbstractClickService.this;
        }
    }

	// This is the object that receives interactions from clients.
	private final IBinder mBinder = new LocalBinder();

    /**
     * Called when local notification was clicked to launch the main intent.
     *
     */
    @Override
    public void onCreate () {
        super.onCreate();
    }

    /**
     * Called when local notification was clicked
     *
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			Bundle bundle   = intent.getExtras();
			Context context = getApplicationContext();

			try {
				String[] data = bundle.getStringArray(Options.EXTRA);
				actionIdentifier = data[1];
				JSONObject options = new JSONObject(data[0]);
				JSONArray actions = options.getJSONArray("actions");
				for (int i = 0; i < actions.length(); i++) {
					if (actions.getJSONObject(i).get("identifier").equals(actionIdentifier)) {
						actionObj = actions.getJSONObject(i);
						break;
					}
				}

				Builder builder =
						new Builder(context, options);

				Notification notification =
						buildNotification(builder);

				onClick(notification);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return START_STICKY;
    }

	@Override
	public IBinder onBind(Intent intent) {
		//TODO haven't actually tested bound client connections
		return mBinder;
	}

    /**
     * Called when local notification was clicked by the user.
     *
     * @param notification
     *      Wrapper around the local notification
     */
    abstract public void onClick (Notification notification);

    /**
     * Build notification specified by options.
     *
     * @param builder
     *      Notification builder
     */
    abstract public Notification buildNotification (Builder builder);

    /**
     * Launch main intent from package.
     */
    public void launchApp() {
		Application app = getApplication();
        Context context = getApplicationContext();
        String pkgName  = context.getPackageName();

        Intent intent = context
                .getPackageManager()
                .getLaunchIntentForPackage(pkgName);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		boolean is_main_activity_running = false;

		ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
		//if 0 tasks are running, then assume the application was terminated and now only the
		//service is running/has been restarted. Handle this situation by starting the app's main
		//activity
		List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
		is_main_activity_running = (tasks.size() > 0);

        try {
            if (null == actionObj || actionObj.getString("activationMode").equals("foreground") || ! is_main_activity_running) {
				Intent temp_i = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
				context.sendBroadcast(temp_i);
                context.startActivity(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
