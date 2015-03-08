package com.valvesoftware.android.steam.community;

import com.valvesoftware.android.steam.community.Config.SteamUniverse;

public class Config {
    public static String APP_VERSION = null;
    public static int APP_VERSION_ID = 0;
    public static final String STEAM_UNIVERSE_DEV_HOST = "vitaliy";
    public static final SteamUniverse STEAM_UNIVERSE_WEBAPI;
    public static final SteamUniverse STEAM_UNIVERSE_WEBPHP;
    public static final String URL_COMMUNITY_BASE;
    public static final String URL_COMMUNITY_BASE_INSECURE;
    public static final String URL_MOBILE_CRASH_UPLOAD;
    public static final String URL_MOBILE_UPDATE = "http://m.valvesoftware.com/";
    public static final String URL_STORE_BASE;
    public static final String URL_STORE_BASE_INSECURE;
    public static final String URL_WEBAPI_BASE;
    public static final String URL_WEBAPI_BASE_INSECURE;

    public static class Debug {
        public static final boolean AggressiveActivityKilling = false;
        public static final boolean DisableOverTheAir = false;
        public static final boolean DiskCacheForceMiss = false;
        public static final boolean HockeyUpdaterForceTest = false;
        public static final int HttpFakeLatencyMilliseconds = 0;
        public static final boolean JobQueuesDebug = false;
        public static final int UmqFakePollFailurePercentage = 0;
    }

    public static class Feature {
        public static final boolean LoginRequiredToUseApp = true;
        public static final boolean SteamGuardShipping = false;
    }

    public static class Logging {

        public static class Activities {
            public static final boolean chat = false;
            public static final boolean listdbrefresh = false;
            public static final boolean login = false;
            public static final boolean tabgroup = false;
            public static final boolean tabs = false;
            public static final boolean uri = false;
        }

        public static class C2DM {
            public static final boolean app2google = false;
            public static final boolean pushnotifications = false;
            public static final boolean service = false;
            public static final boolean unsupported = false;
        }

        public static class DebugUtil {
            public static final boolean log_collected_info = false;
            public static final boolean log_sql = false;
            public static final boolean log_uploaded_dumps = false;
            public static final boolean retain_collected_after_upload = false;
            public static final boolean verbose_collection = false;
        }

        public static class SteamDB {
            public static final boolean cache_files = false;
            public static final boolean cache_read_hit = false;
            public static final boolean cache_read_miss = false;
            public static final boolean cache_write = false;
            public static final boolean disk_errors = false;
            public static final boolean http_request = false;
            public static final boolean http_response_fail = false;
            public static final boolean http_response_ok = false;
            public static final boolean parse_errors = false;
            public static final boolean text_full_docs = false;
        }

        public static class ThreadPool {
            public static final boolean start_stop = false;
        }

        public static class UMQ {
            public static final boolean log_exceptions = false;
            public static final boolean log_sql = false;
            public static final boolean messages = false;
            public static final boolean parse_errors = false;
            public static final boolean relationship = false;
            public static final boolean state = false;
        }
    }

    public enum SteamUniverse {
        Public,
        Beta,
        Dev
    }

    public static class WebAPI {
        public static final int MAX_IDS_PER_CALL = 100;
        public static final String OAUTH_CLIENT_ID;

        static {
            String str = STEAM_UNIVERSE_WEBAPI == SteamUniverse.Public ? "DE45CD61" : STEAM_UNIVERSE_WEBAPI == SteamUniverse.Beta ? "7DC60112" : "E77327FA";
            OAUTH_CLIENT_ID = str;
        }
    }

    static {
        APP_VERSION = "1.0";
        APP_VERSION_ID = 0;
        STEAM_UNIVERSE_WEBAPI = SteamUniverse.Public;
        STEAM_UNIVERSE_WEBPHP = SteamUniverse.Public;
        String str = STEAM_UNIVERSE_WEBAPI == SteamUniverse.Public ? "https://api.steampowered.com:443" : STEAM_UNIVERSE_WEBAPI == SteamUniverse.Beta ? "https://api-beta.steampowered.com:443" : "https://vitaliy.valvesoftware.com:8283";
        URL_WEBAPI_BASE = str;
        str = STEAM_UNIVERSE_WEBAPI == SteamUniverse.Public ? "http://api.steampowered.com:80" : STEAM_UNIVERSE_WEBAPI == SteamUniverse.Beta ? "http://api-beta.steampowered.com:80" : "http://vitaliy.valvesoftware.com:8282";
        URL_WEBAPI_BASE_INSECURE = str;
        StringBuilder append = new StringBuilder().append("https://");
        str = STEAM_UNIVERSE_WEBPHP == SteamUniverse.Public ? "steamcommunity.com" : STEAM_UNIVERSE_WEBPHP == SteamUniverse.Beta ? "beta.steamcommunity.com" : "vitaliy.valvesoftware.com/community";
        URL_COMMUNITY_BASE = append.append(str).toString();
        append = new StringBuilder().append("http://");
        str = STEAM_UNIVERSE_WEBPHP == SteamUniverse.Public ? "steamcommunity.com" : STEAM_UNIVERSE_WEBPHP == SteamUniverse.Beta ? "beta.steamcommunity.com" : "vitaliy.valvesoftware.com/community";
        URL_COMMUNITY_BASE_INSECURE = append.append(str).toString();
        append = new StringBuilder().append("https://");
        str = STEAM_UNIVERSE_WEBPHP == SteamUniverse.Public ? "store.steampowered.com" : STEAM_UNIVERSE_WEBPHP == SteamUniverse.Beta ? "store-beta.steampowered.com" : "vitaliy.valvesoftware.com/store";
        URL_STORE_BASE = append.append(str).toString();
        append = new StringBuilder().append("http://");
        str = STEAM_UNIVERSE_WEBPHP == SteamUniverse.Public ? "store.steampowered.com" : STEAM_UNIVERSE_WEBPHP == SteamUniverse.Beta ? "store-beta.steampowered.com" : "vitaliy.valvesoftware.com/store";
        URL_STORE_BASE_INSECURE = append.append(str).toString();
        if (STEAM_UNIVERSE_WEBPHP != SteamUniverse.Dev) {
            str = "http://m.valvesoftware.com/androidsubmit1";
        } else {
            str = "http://vitaliy.valvesoftware.com/crashupload/androidsubmit1";
        }
        URL_MOBILE_CRASH_UPLOAD = str;
    }
}
