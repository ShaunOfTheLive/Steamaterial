package com.valvesoftware.android.steam.community;

import android.net.Uri;
import com.valvesoftware.android.steam.community.SteamUriHandler.Command;
import com.valvesoftware.android.steam.community.SteamUriHandler.CommandProperty;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;

public class SteamUriHandler {
    public static final String MOBILE_PROTOCOL = "steammobile://";
    public static final String TAG = "SteamUriHandler";

    public enum Command {
        openurl,
        settitle,
        login,
        closethis,
        notfound,
        agecheck,
        agecheckfailed,
        opencategoryurl,
        errorrecovery,
        reloadpage,
        chat,
        openexternalurl,
        mobileloginsucceeded,
        application_internal
    }

    public enum CommandProperty {
        url,
        call,
        title,
        steamid,
        oauth_token,
        webcookie
    }

    public static class Result {
        public Command command;
        public boolean handled;
        public Properties props;

        public Result() {
            this.handled = false;
        }

        public String getProperty(CommandProperty eProperty) {
            return this.props.getProperty(eProperty.toString());
        }

        public String getProperty(CommandProperty eProperty, String sDefaultValue) {
            return this.props.getProperty(eProperty.toString(), sDefaultValue);
        }
    }

    public static Result HandleSteamURI(Uri uri) {
        String uriString = uri.toString();
        String params = uri.getEncodedQuery();
        Result result = new Result();
        if (uriString.startsWith(MOBILE_PROTOCOL)) {
            try {
                uriString = uriString.substring(MOBILE_PROTOCOL.length());
                int nPosQ = uriString.indexOf("?");
                if (nPosQ > 0) {
                    uriString = uriString.substring(0, nPosQ);
                }
                result.command = Command.valueOf(uriString);
                result.handled = true;
            } catch (RuntimeException e) {
            }
        }
        if (result.handled) {
            try {
                result.props = new Properties();
                if (params != null) {
                    if (result.command == Command.mobileloginsucceeded) {
                        try {
                            JSONObject jsonDoc = new JSONObject(Uri.decode(params));
                            JSONArray arrChildren = jsonDoc.names();
                            for (int jj = 0; jj < arrChildren.length(); jj++) {
                                try {
                                    String key = (String) arrChildren.opt(jj);
                                    result.props.put(key, jsonDoc.get(key).toString());
                                } catch (Exception e2) {
                                }
                            }
                        } catch (Exception e3) {
                        }
                    } else {
                        result.props.load(new ByteArrayInputStream(params.getBytes()));
                    }
                }
            } catch (IOException e4) {
            }
        }
        return result;
    }
}
