package com.litmonk.connectmgr.mqtt.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class SslUtil {
    private static final String SSL_VERSION = "TLSv1.2";
    private static final String SUN_X_509 = "SunX509";

    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public static SSLContext getSslContext(String keyStoreFile, String keyStoreType, String keyStorePwd,
                                           String trustStoreFile, String trustStoreType, String trustStorePwd)
            throws Exception {
        SSLContext sslContext = SSLContext.getInstance(SSL_VERSION);
        KeyManager[] keyManagers;
        TrustManager[] trustManagers;
        try (FileInputStream inputStream = new FileInputStream(keyStoreFile)) {
            keyManagers = getKeyManagers(inputStream, keyStoreType, keyStorePwd);
        }
        try (FileInputStream inputStream = new FileInputStream(trustStoreFile)) {
            trustManagers = getTrustManagers(inputStream, trustStoreType, trustStorePwd);
        }

        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    public static KeyManager[] getKeyManagers(InputStream inputStream, String keyStoreType, String keyStorePwd)
            throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(SUN_X_509);
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(inputStream, keyStorePwd.toCharArray());
        keyManagerFactory.init(keyStore, keyStorePwd.toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    public static TrustManager[] getTrustManagers(InputStream inputStream, String trustStoreType, String trustStorePwd)
            throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(SUN_X_509);
        KeyStore keyStore = KeyStore.getInstance(trustStoreType);
        keyStore.load(inputStream, trustStorePwd.toCharArray());
        trustManagerFactory.init(keyStore);
        return trustManagerFactory.getTrustManagers();
    }

    /**
     * 获取信任所有服务端证书的SSLSocketFactory
     *
     * @param keyStoreFile
     * @param keyStoreType
     * @param keyStorePwd
     * @return
     * @throws Exception
     */
    public static SSLSocketFactory getTrustAllSocketFactory(String keyStoreFile, String keyStoreType, String keyStorePwd)
            throws Exception {
        SSLContext sslContext = SSLContext.getInstance(SSL_VERSION);
        KeyManager[] keyManagers;
        try (InputStream inputStream = SslUtil.class.getResourceAsStream(keyStoreFile)) {
            keyManagers = getKeyManagers(inputStream, keyStoreType, keyStorePwd);
        }

        sslContext.init(keyManagers, trustAllCerts, new SecureRandom());

        return sslContext.getSocketFactory();
    }

    /**
     * 获取信任所有服务端证书的SSLSocketFactory
     *
     * @param certPath 证书（crt）
     * @param privateKeyPath 证书私钥（key）
     * @return
     * @throws Exception
     */
    public static SSLSocketFactory getTrustAllSocketFactory(String certPath, String privateKeyPath)
            throws Exception {
        SSLContext sslContext = SSLContext.getInstance(SSL_VERSION);
        KeyManager[] keyManagers = getKeyManagers(new File(privateKeyPath), new File(certPath));

        sslContext.init(keyManagers, trustAllCerts, new SecureRandom());

        return sslContext.getSocketFactory();
    }

    /**
     * 读取RSA私钥
     * @param file
     * @return
     * @throws Exception
     */
    public static RSAPrivateKey readPrivateKey(File file) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (FileReader keyReader = new FileReader(file)) {
            PemReader pemReader = new PemReader(keyReader);
            PemObject pemObject = pemReader.readPemObject();
            byte[] context = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(context);
            return (RSAPrivateKey)factory.generatePrivate(privKeySpec);
        }
    }

    /**
     * 读取crt证书信息
     * @param file
     * @return
     * @throws Exception
     */
    public static X509Certificate readX509Certificate(File file) throws Exception {
        try (InputStream inStream = new FileInputStream(file)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate)certFactory.generateCertificate(inStream);
        }
    }

    /**
     * 读取证书公钥信息
     * @param file
     * @return
     * @throws Exception
     */
    public static PublicKey readPublicKey(File file) throws Exception {
        X509Certificate cert = readX509Certificate(file);
        return cert.getPublicKey();
    }

    public static KeyManager[] getKeyManagers(File privKeyFile, File certFile) throws Exception {
        RSAPrivateKey privateKey = readPrivateKey(privKeyFile);
        X509Certificate cert = readX509Certificate(certFile);

        char[] pwd = "".toCharArray();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, pwd);
        keyStore.setKeyEntry("private", privateKey, pwd, new Certificate[] { cert });

        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmFactory.init(keyStore, pwd);

        return kmFactory.getKeyManagers();
    }

    static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    } };
}