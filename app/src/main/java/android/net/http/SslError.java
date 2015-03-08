package android.net.http;

import java.security.cert.X509Certificate;

public class SslError {
    public static final int SSL_EXPIRED = 1;
    public static final int SSL_IDMISMATCH = 2;
    public static final int SSL_MAX_ERROR = 4;
    public static final int SSL_NOTYETVALID = 0;
    public static final int SSL_UNTRUSTED = 3;
    SslCertificate mCertificate;
    int mErrors;

    public SslError(int error, SslCertificate certificate) {
        addError(error);
        this.mCertificate = certificate;
    }

    public SslError(int error, X509Certificate certificate) {
        addError(error);
        this.mCertificate = new SslCertificate(certificate);
    }

    public SslCertificate getCertificate() {
        return this.mCertificate;
    }

    public boolean addError(int error) {
        boolean rval = error >= 0 && error < 4;
        if (rval) {
            this.mErrors = (1 << error) | this.mErrors;
        }
        return rval;
    }

    public boolean hasError(int error) {
        boolean rval;
        if (error < 0 || error >= 4) {
            rval = false;
        } else {
            rval = true;
        }
        if (rval) {
            return (this.mErrors & (1 << error)) != 0;
        } else {
            return rval;
        }
    }

    public int getPrimaryError() {
        if (this.mErrors != 0) {
            for (int error = SSL_UNTRUSTED; error >= 0; error--) {
                if ((this.mErrors & (1 << error)) != 0) {
                    return error;
                }
            }
        }
        return SSL_NOTYETVALID;
    }

    public String toString() {
        return "primary error: " + getPrimaryError() + " certificate: " + getCertificate();
    }
}
