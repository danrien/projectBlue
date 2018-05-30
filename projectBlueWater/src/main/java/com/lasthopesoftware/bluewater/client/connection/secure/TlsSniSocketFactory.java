/*
 * Copyright 2010-2013 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lasthopesoftware.bluewater.client.connection.secure;

import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;

import org.apache.http.conn.ssl.StrictHostnameVerifier;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Implements an HttpClient socket factory with extensive support for SSL. Many thanks to
 * http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv1-2-with-apache-httpclient for the base
 * implementation.
 * <p/>
 * Firstly, all SSL protocols that a particular Android version support will be enabled (according to
 * http://developer.android.com/reference/javax/net/ssl/SSLSocket.html). This currently includes SSL v3 and TLSv1.0,
 * v1.1 and v1.2.
 * <p/>
 * Second, SNI is supported for host name verification. For Android 4.2+, which supports it natively, the default
 * (strict) hostname verifier is used. For Android 4.1 and earlier it is possibly supported through reflexion on the
 * same methods.
 * <p/>
 * Third, self-signed certificates are supported through the checking of the received certificate key with a given SHA-1
 * encoded hex of the self-signed certificate key. When a key is given but not a correct match, the thumbprint of the
 * server certificate is given, such that the correct SHA-1 hash to use can be foudn in the log.
 * <p/>
 * Finally, the ignoring of all SSL certificates (and hostname) is possible (which is obviously very insecure!).
 */
public class TlsSniSocketFactory extends SSLSocketFactory {

	private final static HostnameVerifier hostnameVerifier = new StrictHostnameVerifier();

	private final String selfSignedCertificateKey;

	public TlsSniSocketFactory() {
		this.selfSignedCertificateKey = null;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return new String[0];
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return new String[0];
	}

	public TlsSniSocketFactory(String certKey) {
		this.selfSignedCertificateKey = certKey;
	}

	// Plain TCP/IP (layer below TLS)

	@Override
	public Socket createSocket() {
		return null;
	}

	@Override
	public Socket createSocket(String host, int port) {
		return null;
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) {
		return null;
	}

	@Override
	public Socket createSocket(InetAddress host, int port) {
		return null;
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
		return null;
	}

	// TLS layer

	@Override
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException {
		if (autoClose) {
			// we don't need the plainSocket
			plainSocket.close();
		}

		SSLCertificateSocketFactory sslSocketFactory =
			(SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);

		// For self-signed certificates use a custom trust manager
		if (selfSignedCertificateKey != null) {
			sslSocketFactory.setTrustManagers(new TrustManager[]{new SelfSignedTrustManager(selfSignedCertificateKey)});
		}

		// create and connect SSL socket, but don't do hostname/certificate verification yet
		SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName(host), port);

		// enable TLSv1.1/1.2 if available
		ssl.setEnabledProtocols(ssl.getSupportedProtocols());

		// set up SNI before the handshake
		sslSocketFactory.setHostname(ssl, host);

		// verify hostname and certificate
		SSLSession session = ssl.getSession();
		if (selfSignedCertificateKey == null && !hostnameVerifier.verify(host, session)) {
			throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);
		}

		/*DLog.d(TlsSniSocketFactory.class.getSimpleName(),
				"Established " + session.getProtocol() + " connection with " + session.getPeerHost() +
						" using " + session.getCipherSuite());*/

		return ssl;
	}

}

