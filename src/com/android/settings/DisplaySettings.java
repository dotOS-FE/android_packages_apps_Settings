/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import static android.os.UserHandle.USER_SYSTEM;

import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;

import android.os.SystemProperties;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import androidx.preference.*;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.BrightnessLevelPreferenceController;
import com.android.settings.display.CameraGesturePreferenceController;
import com.android.settings.display.LiftToWakePreferenceController;
import com.android.settings.display.NightDisplayPreferenceController;
import com.android.settings.display.NightModePreferenceController;
import com.android.settings.display.ScreenSaverPreferenceController;
import com.android.settings.display.ShowOperatorNamePreferenceController;
import com.android.settings.display.TapToWakePreferenceController;
import com.android.settings.display.ThemePreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.display.VrDisplayPreferenceController;
import com.android.settings.display.EmulateDisplayCutoutPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

import com.dotfe.support.preferences.SystemSettingListPreference;
import com.dotfe.support.preferences.SystemSettingSwitchPreference;
import com.android.internal.util.custom.Utils;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class DisplaySettings extends DashboardFragment {
    private static final String TAG = "DisplaySettings";

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";

    private IOverlayManager mOverlayManager;
    private PackageManager mPackageManager;    
    private static final String SLIDER_STYLE  = "slider_style";

    private SystemSettingListPreference mSlider;
    private Handler mHandler;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.display_settings;
    }

    @Override
    public void onCreate(Bundle icicle) {
	getActivity().getActionBar().hide();
	super.onCreate(icicle);
        final ContentResolver resolver = getActivity().getContentResolver();
        Context mContext = getContext();
        mOverlayManager = IOverlayManager.Stub.asInterface(
        ServiceManager.getService(Context.OVERLAY_SERVICE));
        mSlider = (SystemSettingListPreference) findPreference(SLIDER_STYLE);
        mCustomSettingsObserver.observe();        

    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SLIDER_STYLE  ),
                    false, this, UserHandle.USER_ALL);                  
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.SLIDER_STYLE  ))) {
                updateSlider();
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mSlider) {
            mCustomSettingsObserver.observe();
            return true;
        }
        return false;
    }

    private void updateSlider() {
        ContentResolver resolver = getActivity().getContentResolver();

        boolean sliderDefault = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SLIDER_STYLE , 0, UserHandle.USER_CURRENT) == 0;
        boolean sliderOOS = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SLIDER_STYLE , 0, UserHandle.USER_CURRENT) == 1;
        boolean sliderAosp = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SLIDER_STYLE , 0, UserHandle.USER_CURRENT) == 2;
        boolean sliderRUI = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SLIDER_STYLE , 0, UserHandle.USER_CURRENT) == 3;
        boolean sliderA12 = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SLIDER_STYLE , 0, UserHandle.USER_CURRENT) == 4;                

        if (sliderDefault) {
            setDefaultSlider(mOverlayManager);
        } else if (sliderOOS) {
            enableSlider(mOverlayManager, "com.android.theme.systemui_slider_oos");
        } else if (sliderAosp) {
            enableSlider(mOverlayManager, "com.android.theme.systemui_slider.aosp");
        } else if (sliderRUI) {
            enableSlider(mOverlayManager, "com.android.theme.systemui_slider.rui");
        } else if (sliderA12) {
            enableSlider(mOverlayManager, "com.android.theme.systemui_slider.a12");            

        }
    }

    public static void setDefaultSlider(IOverlayManager overlayManager) {
        for (int i = 0; i < SLIDERS.length; i++) {
            String sliders = SLIDERS[i];
            try {
                overlayManager.setEnabled(sliders, false, USER_SYSTEM);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void enableSlider(IOverlayManager overlayManager, String overlayName) {
        try {
            for (int i = 0; i < SLIDERS.length; i++) {
                String sliders = SLIDERS[i];
                try {
                    overlayManager.setEnabled(sliders, false, USER_SYSTEM);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            overlayManager.setEnabled(overlayName, true, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void handleOverlays(String packagename, Boolean state, IOverlayManager mOverlayManager) {
        try {
            mOverlayManager.setEnabled(packagename,
                    state, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static final String[] SLIDERS = {
        "com.android.theme.systemui_slider_oos",
        "com.android.theme.systemui_slider.aosp",
        "com.android.theme.systemui_slider.rui",
        "com.android.theme.systemui_slider.a12"        
    };

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_display;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new CameraGesturePreferenceController(context));
        controllers.add(new LiftToWakePreferenceController(context));
        controllers.add(new NightDisplayPreferenceController(context));
        controllers.add(new NightModePreferenceController(context));
        controllers.add(new ScreenSaverPreferenceController(context));
        controllers.add(new TapToWakePreferenceController(context));
        controllers.add(new TimeoutPreferenceController(context, KEY_SCREEN_TIMEOUT));
        controllers.add(new VrDisplayPreferenceController(context));
        controllers.add(new ShowOperatorNamePreferenceController(context));
        controllers.add(new ThemePreferenceController(context));
        controllers.add(new EmulateDisplayCutoutPreferenceController(context));
        controllers.add(new BrightnessLevelPreferenceController(context, lifecycle));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.display_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}
