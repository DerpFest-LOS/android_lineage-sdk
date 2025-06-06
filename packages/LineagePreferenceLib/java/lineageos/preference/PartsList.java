/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-License-Identifier: Apache-2.0
 */
package lineageos.preference;

import static com.android.internal.R.styleable.Preference;
import static com.android.internal.R.styleable.Preference_fragment;
import static com.android.internal.R.styleable.Preference_icon;
import static com.android.internal.R.styleable.Preference_key;
import static com.android.internal.R.styleable.Preference_summary;
import static com.android.internal.R.styleable.Preference_title;

import static lineageos.preference.R.styleable.lineage_Searchable;
import static lineageos.preference.R.styleable.lineage_Searchable_xmlRes;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class PartsList {

    private static final String TAG = PartsList.class.getSimpleName();

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String EXTRA_PART = ":lineage:part";

    public static final String LINEAGEPARTS_PACKAGE = "org.lineageos.lineageparts";

    public static final ComponentName LINEAGEPARTS_ACTIVITY = new ComponentName(
            LINEAGEPARTS_PACKAGE, LINEAGEPARTS_PACKAGE + ".PartsActivity");

    public static final String PARTS_ACTION_PREFIX = LINEAGEPARTS_PACKAGE + ".parts";

    private final Map<String, PartInfo> mParts = new ArrayMap<>();

    private final Context mContext;

    private static PartsList sInstance;
    private static final Object sInstanceLock = new Object();

    private PartsList(Context context) {
        mContext = context;
        loadParts();
    }

    public static PartsList get(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new PartsList(context);
            }
            return sInstance;
        }
    }

    private void loadParts() {
        synchronized (mParts) {
            final PackageManager pm = mContext.getPackageManager();
            try {
                final Resources r = pm.getResourcesForApplication(LINEAGEPARTS_PACKAGE);
                if (r == null) {
                    return;
                }
                int resId = r.getIdentifier("parts_catalog", "xml", LINEAGEPARTS_PACKAGE);
                if (resId > 0) {
                    loadPartsFromResourceLocked(r, resId, mParts);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // no lineageparts installed
            }
        }
    }

    public Set<String> getPartsList() {
        synchronized (mParts) {
            return mParts.keySet();
        }
    }

    public PartInfo getPartInfo(String key) {
        synchronized (mParts) {
            return mParts.get(key);
        }
    }

    public final PartInfo getPartInfoForClass(String clazz) {
        synchronized (mParts) {
            for (PartInfo info : mParts.values()) {
                if (info.getFragmentClass() != null && info.getFragmentClass().equals(clazz)) {
                    return info;
                }
            }
            return null;
        }
    }

    private void loadPartsFromResourceLocked(Resources res, int resid,
                                             Map<String, PartInfo> target) {
        XmlResourceParser parser = null;

        try {
            parser = res.getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"parts-catalog".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <parts-catalog> tag; found "
                                + nodeName + " at " + parser.getPositionDescription());
            }

            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("part".equals(nodeName)) {
                    TypedArray sa = res.obtainAttributes(attrs, Preference);

                    String key = null;
                    TypedValue tv = sa.peekValue(Preference_key);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            key = res.getString(tv.resourceId);
                        } else {
                            key = String.valueOf(tv.string);
                        }
                    }
                    if (key == null) {
                        throw new RuntimeException("Attribute 'key' is required");
                    }

                    final PartInfo info = new PartInfo(key);

                    tv = sa.peekValue(Preference_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setTitle(res.getString(tv.resourceId));
                        } else {
                            info.setTitle(String.valueOf(tv.string));
                        }
                    }

                    tv = sa.peekValue(Preference_summary);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setSummary(res.getString(tv.resourceId));
                        } else {
                            info.setSummary(String.valueOf(tv.string));
                        }
                    }

                    info.setFragmentClass(sa.getString(Preference_fragment));
                    info.setIconRes(sa.getResourceId(Preference_icon, 0));

                    sa = res.obtainAttributes(attrs, lineage_Searchable);
                    info.setXmlRes(sa.getResourceId(lineage_Searchable_xmlRes, 0));

                    sa.recycle();

                    target.put(key, info);

                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } finally {
            if (parser != null) parser.close();
        }
    }
}
