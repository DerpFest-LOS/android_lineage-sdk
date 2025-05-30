/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package lineageos.profiles;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/**
 * The {@link ConnectionSettings} class allows for creating Network/Hardware overrides
 * depending on their capabilities.
 *
 * <p>Example for enabling/disabling sync settings:
 * <pre class="prettyprint">
 * ConnectionSettings connectionSettings =
 *         new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_SYNC,
 *         shouldBeEnabled() ?
 *         {@link BooleanState#STATE_ENABLED} : {@link BooleanState#STATE_DISABLED},
 *         true)
 * profile.setConnectionSettings(connectionSettings);
 * </pre>
 */
public final class ConnectionSettings implements Parcelable {

    private int mConnectionId;
    private int mValue;
    private boolean mOverride;
    private boolean mDirty;

    /**
     * For use with {@link #PROFILE_CONNECTION_2G3G4G} to determine what subscription to control.
     */
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    /**
     * The {@link #PROFILE_CONNECTION_MOBILEDATA} allows for enabling and disabling the mobile
     * data connection. Boolean connection settings {@link BooleanState}
     */
    public static final int PROFILE_CONNECTION_MOBILEDATA = 0;

    /**
     * The {@link #PROFILE_CONNECTION_WIFI} allows for enabling and disabling the WiFi connection
     * on the device. Boolean connection settings {@link BooleanState}
     */
    public static final int PROFILE_CONNECTION_WIFI = 1;

    /**
     * The {@link #PROFILE_CONNECTION_WIFIAP} allows for enabling and disabling the WiFi hotspot
     * on the device. Boolean connection settings {@link BooleanState}
     */
    public static final int PROFILE_CONNECTION_WIFIAP = 2;

    // Deprecated
    // public static final int PROFILE_CONNECTION_WIMAX = 3;

    // Deprecated
    // public static final int PROFILE_CONNECTION_GPS = 4;

    /**
     * The {@link #PROFILE_CONNECTION_SYNC} allows for enabling and disabling the global sync state
     * on the device. Boolean connection settings {@link BooleanState}
     */
    public static final int PROFILE_CONNECTION_SYNC = 5;

    /**
     * The {@link #PROFILE_CONNECTION_LOCATION} allows for enabling and disabling location services
     * on the device. Boolean connection settings {@link BooleanState}
     */
    public static final int PROFILE_CONNECTION_LOCATION = 6;

    /**
     * The {@link #PROFILE_CONNECTION_BLUETOOTH} allows for enabling and disabling the Bluetooth device
     * (if exists) on the device. Boolean connection settings {@link BooleanState}
     */
    public static final int PROFILE_CONNECTION_BLUETOOTH = 7;

    /**
     * The {@link #PROFILE_CONNECTION_NFC} allows for enabling and disabling the NFC device
     * (if exists) on the device. Boolean connection settings {@link BooleanState}
     */
    public static final int PROFILE_CONNECTION_NFC = 8;

    /**
     * The {@link #PROFILE_CONNECTION_2G3G4G} allows for flipping between 2G/3G/4G (if exists)
     * on the device.
     */
    public static final int PROFILE_CONNECTION_2G3G4G = 9;

    // retrieved from Phone.apk
    private static final String ACTION_MODIFY_NETWORK_MODE =
            "com.android.internal.telephony.MODIFY_NETWORK_MODE";
    private static final String EXTRA_NETWORK_MODE = "networkMode";
    private static final String EXTRA_SUB_ID = "subId";

    /**
     * BooleanStates for specific {@link ConnectionSettings}
     */
    public static class BooleanState {
        /** Disabled state */
        public static final int STATE_DISABLED = 0;
        /** Enabled state */
        public static final int STATE_ENABLED = 1;
    }

    private static final int LINEAGE_MODE_2G = 0;
    private static final int LINEAGE_MODE_3G = 1;
    private static final int LINEAGE_MODE_4G = 2;
    private static final int LINEAGE_MODE_2G3G = 3;
    private static final int LINEAGE_MODE_ALL = 4;

    /** @hide */
    public static final Parcelable.Creator<ConnectionSettings> CREATOR =
            new Parcelable.Creator<ConnectionSettings>() {
        public ConnectionSettings createFromParcel(Parcel in) {
            return new ConnectionSettings(in);
        }

        @Override
        public ConnectionSettings[] newArray(int size) {
            return new ConnectionSettings[size];
        }
    };

    /**
     * Unwrap {@link ConnectionSettings} from a parcel.
     * @param parcel
     */
    public ConnectionSettings(Parcel parcel) {
        readFromParcel(parcel);
    }

    /**
     * Construct a {@link ConnectionSettings} with a connection id and default states.
     * @param connectionId ex: #PROFILE_CONNECTION_NFC
     */
    public ConnectionSettings(int connectionId) {
        this(connectionId, 0, false);
    }

    /**
     * Construct a {@link ConnectionSettings} with a connection id, default value
     * {@see BooleanState}, and if the setting should override the user defaults.
     * @param connectionId an identifier for the ConnectionSettings (ex:#PROFILE_CONNECTION_WIFI)
     * @param value default value for the ConnectionSettings (ex:{@link BooleanState#STATE_ENABLED})
     * @param override whether or not the {@link ConnectionSettings} should override user defaults
     */
    public ConnectionSettings(int connectionId, int value, boolean override) {
        mConnectionId = connectionId;
        mValue = value;
        mOverride = override;
        mDirty = false;
    }

    /**
     * Retrieve the connection id associated with the {@link ConnectionSettings}
     * @return an integer identifier
     */
    public int getConnectionId() {
        return mConnectionId;
    }

    /**
     * Get the default value for the {@link ConnectionSettings}
     * @return integer value corresponding with its state
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Set the default value for the {@link ConnectionSettings}
     * @param value {@link BooleanState}
     */
    public void setValue(int value) {
        mValue = value;
        mDirty = true;
    }

    /**
     * Set whether or not the {@link ConnectionSettings} should override default user values
     * @param override boolean override
     */
    public void setOverride(boolean override) {
        mOverride = override;
        mDirty = true;
    }

    public void setSubId(int subId) {
        mSubId = subId;
        mDirty = true;
    }

    /**
     * Check whether or not the {@link ConnectionSettings} overrides user settings.
     * @return true if override
     */
    public boolean isOverride() {
        return mOverride;
    }

    /**
     * Get the subscription id which this {@link ConnectionSettings} should apply to.
     * @return
     */
    public int getSubId() {
        return mSubId;
    }

    /** @hide */
    public boolean isDirty() {
        return mDirty;
    }

    /** @hide */
    public void processOverride(Context context) {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);

        boolean forcedState = getValue() == 1;
        boolean currentState;

        switch (getConnectionId()) {
            case PROFILE_CONNECTION_MOBILEDATA:
                List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        int subId = list.get(i).getSubscriptionId();
                        int slotIndex = list.get(i).getSimSlotIndex();
                        currentState = tm.getDataEnabled(subId);
                        if (forcedState != currentState) {
                            Settings.Global.putInt(context.getContentResolver(),
                                    Settings.Global.MOBILE_DATA + i, (forcedState) ? 1 : 0);
                            tm.setDataEnabled(subId, forcedState);
                        }
                    }
                }
                break;
            case PROFILE_CONNECTION_2G3G4G:
                Intent intent = new Intent(ACTION_MODIFY_NETWORK_MODE);
                intent.putExtra(EXTRA_NETWORK_MODE, getValue());
                intent.putExtra(EXTRA_SUB_ID, getSubId());
                context.sendBroadcast(intent, "com.android.phone.CHANGE_NETWORK_MODE");
                break;
            case PROFILE_CONNECTION_BLUETOOTH:
                int btstate = bta.getState();
                if (forcedState && (btstate == BluetoothAdapter.STATE_OFF
                        || btstate == BluetoothAdapter.STATE_TURNING_OFF)) {
                    bta.enable();
                } else if (!forcedState && (btstate == BluetoothAdapter.STATE_ON
                        || btstate == BluetoothAdapter.STATE_TURNING_ON)) {
                    bta.disable();
                }
                break;
            case PROFILE_CONNECTION_LOCATION:
                currentState = lm.isLocationEnabled();
                if (currentState != forcedState) {
                    lm.setLocationEnabledForUser(forcedState,
                            new UserHandle(UserHandle.USER_CURRENT));
                }
                break;
            case PROFILE_CONNECTION_SYNC:
                currentState = ContentResolver.getMasterSyncAutomatically();
                if (forcedState != currentState) {
                    ContentResolver.setMasterSyncAutomatically(forcedState);
                }
                break;
            case PROFILE_CONNECTION_WIFI:
                int wifiApState = wm.getWifiApState();
                currentState = wm.isWifiEnabled();
                if (currentState != forcedState) {
                    // Disable wifi tether
                    if (forcedState && (wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                            (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED)) {
                        cm.stopTethering(ConnectivityManager.TETHERING_WIFI);
                    }
                    wm.setWifiEnabled(forcedState);
                }
                break;
            case PROFILE_CONNECTION_WIFIAP:
                currentState = wm.isWifiApEnabled();
                if (currentState != forcedState) {
                    // ConnectivityManager will disable wifi
                    if (forcedState) {
                        cm.startTethering(ConnectivityManager.TETHERING_WIFI,
                                false, new OnStartTetheringCallback());
                    } else {
                        cm.stopTethering(ConnectivityManager.TETHERING_WIFI);
                    }
                }
                break;
            case PROFILE_CONNECTION_NFC:
                if (nfcAdapter != null) {
                    int adapterState = nfcAdapter.getAdapterState();
                    currentState = (adapterState == NfcAdapter.STATE_ON ||
                            adapterState == NfcAdapter.STATE_TURNING_ON);
                    if (currentState != forcedState) {
                        if (forcedState) {
                            nfcAdapter.enable();
                        } else if (!forcedState && adapterState != NfcAdapter.STATE_TURNING_OFF) {
                            nfcAdapter.disable();
                        }
                    }
                }
                break;
        }
    }

    /** @hide */
    public static ConnectionSettings fromXml(XmlPullParser xpp, Context context)
            throws XmlPullParserException, IOException {
        int event = xpp.next();
        ConnectionSettings connectionDescriptor = new ConnectionSettings(0);
        while (event != XmlPullParser.END_TAG || !xpp.getName().equals("connectionDescriptor")) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("connectionId")) {
                    connectionDescriptor.mConnectionId = Integer.parseInt(xpp.nextText());
                } else if (name.equals("value")) {
                    connectionDescriptor.mValue = Integer.parseInt(xpp.nextText());
                } else if (name.equals("override")) {
                    connectionDescriptor.mOverride = Boolean.parseBoolean(xpp.nextText());
                } else if (name.equals("subId")) {
                    connectionDescriptor.mSubId = Integer.parseInt(xpp.nextText());
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new IOException("Premature end of file while parsing connection settings");
            }
            event = xpp.next();
        }
        return connectionDescriptor;
    }

    /** @hide */
    public void getXmlString(StringBuilder builder, Context context) {
        builder.append("<connectionDescriptor>\n<connectionId>");
        builder.append(mConnectionId);
        builder.append("</connectionId>\n<value>");
        builder.append(mValue);
        builder.append("</value>\n<override>");
        builder.append(mOverride);
        builder.append("</override>\n");
        if (mConnectionId == PROFILE_CONNECTION_2G3G4G
                && mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            builder.append("<subId>").append(mSubId).append("</subId>\n");
        }
        builder.append("</connectionDescriptor>\n");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mConnectionId);
        dest.writeInt(mOverride ? 1 : 0);
        dest.writeInt(mValue);
        dest.writeInt(mDirty ? 1 : 0);

        if (mConnectionId == PROFILE_CONNECTION_2G3G4G) {
            dest.writeInt(mSubId);
        }
    }

    /** @hide */
    public void readFromParcel(Parcel in) {
        mConnectionId = in.readInt();
        mOverride = in.readInt() != 0;
        mValue = in.readInt();
        mDirty = in.readInt() != 0;

        if (mConnectionId == PROFILE_CONNECTION_2G3G4G) {
            mSubId = in.readInt();
        }
    }

    private static final class OnStartTetheringCallback
            extends ConnectivityManager.OnStartTetheringCallback {
        @Override
        public void onTetheringStarted() {
            // Do nothing
        }

        @Override
        public void onTetheringFailed() {
            // Do nothing
        }
    }
}
