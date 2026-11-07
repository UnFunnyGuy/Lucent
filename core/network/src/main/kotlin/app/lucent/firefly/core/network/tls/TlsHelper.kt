package app.lucent.firefly.core.network.tls

import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Utility for configuring custom TLS trust managers, enabling support for self-signed
 * or internal CA certificates commonly used in self-hosted Firefly III instances.
 */
object TlsHelper {

    /**
     * Creates a combined [X509TrustManager] containing default system certificates
     * and an optional custom PEM-encoded certificate.
     */
    fun createTrustManager(customCertPem: String? = null): X509TrustManager {
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
        }

        // Load system default certificates
        val defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }
        val defaultTrustManager = defaultTmf.trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()

        defaultTrustManager?.acceptedIssuers?.forEachIndexed { index, cert ->
            trustStore.setCertificateEntry("system_$index", cert)
        }

        // Load custom certificate if present
        if (!customCertPem.isNullOrBlank()) {
            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(
                ByteArrayInputStream(customCertPem.trimIndent().toByteArray())
            ) as X509Certificate
            trustStore.setCertificateEntry("custom_homelab_ca", cert)
        }

        val customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }

        return customTmf.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()
    }

    /**
     * Builds an [SSLSocketFactory] using the provided [trustManager].
     */
    fun createSslSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }
        return sslContext.socketFactory
    }
}
