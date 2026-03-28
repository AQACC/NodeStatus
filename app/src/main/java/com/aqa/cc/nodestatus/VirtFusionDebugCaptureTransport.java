package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionHttpRequest;
import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionHttpTransport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

final class VirtFusionDebugCaptureTransport implements VirtFusionHttpTransport {
    private static final int TIMEOUT_MILLIS = 30_000;
    private static final String SESSION_RENEWAL_FILE_NAME = "session-renewal.txt";

    private final Path debugDirectory;
    private final SSLContext sslContext;
    private final boolean captureEnabled;
    private final SessionCookieJar sessionCookieJar;
    private final List<String> observedSetCookieNames = new ArrayList<>();
    private int observedSetCookieCount;
    private String lastSetCookieUri = "";
    private Instant lastSetCookieAt;

    VirtFusionDebugCaptureTransport(
            @NonNull String cookieHeader,
            @NonNull String xsrfHeader,
            boolean allowInsecureTls,
            boolean captureEnabled
    ) {
        this.debugDirectory = AppSnapshotFiles.virtFusionRefreshDebugDirectoryPath(NodeStatusApp.appContext());
        this.sslContext = allowInsecureTls ? buildInsecureSslContext() : null;
        this.captureEnabled = captureEnabled;
        this.sessionCookieJar = new SessionCookieJar(cookieHeader, xsrfHeader);
    }

    @NonNull
    String getLatestCookieHeader() {
        return sessionCookieJar.getCookieHeader();
    }

    @NonNull
    String getLatestXsrfHeader() {
        return sessionCookieJar.getXsrfHeader();
    }

    boolean hasSessionUpdates() {
        return sessionCookieJar.hasChanged();
    }

    @Override
    public @NonNull String get(@NonNull VirtFusionHttpRequest request) {
        HttpURLConnection connection = null;
        VirtFusionHttpRequest effectiveRequest = sessionCookieJar.apply(request);
        String captureKey = captureKey(effectiveRequest.getUri());
        try {
            connection = (HttpURLConnection) new URL(effectiveRequest.getUri().toString()).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            for (Map.Entry<String, String> header : effectiveRequest.getHeaders().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            if (connection instanceof HttpsURLConnection && sslContext != null) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConnection.setHostnameVerifier((HostnameVerifier) (hostname, session) -> true);
            }

            int statusCode = connection.getResponseCode();
            String body = readBody(statusCode >= 200 && statusCode <= 299
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            recordSessionRenewal(connection.getHeaderFields(), effectiveRequest.getUri());

            if (statusCode < 200 || statusCode > 299) {
                IllegalArgumentException exception =
                        new IllegalArgumentException("Request failed with HTTP " + statusCode + " for " + effectiveRequest.getUri());
                captureExchange(captureKey, effectiveRequest, statusCode, body, exception, null);
                throw exception;
            }
            sessionCookieJar.mergeResponseHeaders(connection.getHeaderFields());
            captureExchange(captureKey, effectiveRequest, statusCode, body, null, null);
            return body;
        } catch (IOException exception) {
            captureExchange(
                    captureKey,
                    effectiveRequest,
                    -1,
                    "",
                    exception,
                    maybeProbeTlsDiagnostics(effectiveRequest.getUri(), exception)
            );
            throw new IllegalStateException("Request failed for " + effectiveRequest.getUri(), exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream input = stream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().reduce("", (left, right) -> left.isEmpty() ? right : left + "\n" + right);
        }
    }

    private void captureExchange(
            String captureKey,
            @NonNull VirtFusionHttpRequest request,
            int statusCode,
            @NonNull String body,
            Exception exception,
            TlsDiagnostics tlsDiagnostics
    ) {
        if (!captureEnabled || captureKey == null) {
            return;
        }

        try {
            File directory = debugDirectory.toFile();
            if (!directory.exists() && !directory.mkdirs()) {
                return;
            }
            if (isRefreshStart(captureKey)) {
                clearExistingCaptureFiles(directory);
            }

            Path requestPath = debugDirectory.resolve(captureKey + ".request.txt");
            Path responsePath = debugDirectory.resolve(captureKey + ".response.json");
            Path errorPath = debugDirectory.resolve(captureKey + ".error.txt");
            Path sessionRenewalPath = debugDirectory.resolve(SESSION_RENEWAL_FILE_NAME);

            writeUtf8(requestPath, buildRequestDump(request, statusCode));
            writeUtf8(sessionRenewalPath, buildSessionRenewalDump());

            if (exception == null && statusCode >= 0) {
                writeUtf8(responsePath, body);
                deleteIfExists(errorPath);
            } else {
                deleteIfExists(responsePath);
                writeUtf8(errorPath, buildErrorDump(statusCode, body, exception, tlsDiagnostics));
            }
        } catch (IOException ignored) {
            // Debug capture must never break refresh behavior.
        }
    }

    private void recordSessionRenewal(
            Map<String, List<String>> responseHeaders,
            @NonNull URI requestUri
    ) {
        if (responseHeaders == null || responseHeaders.isEmpty()) {
            return;
        }

        List<String> setCookieValues = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
            String headerName = entry.getKey();
            if (headerName == null || !"set-cookie".equalsIgnoreCase(headerName)) {
                continue;
            }
            if (entry.getValue() != null) {
                setCookieValues.addAll(entry.getValue());
            }
        }
        if (setCookieValues.isEmpty()) {
            return;
        }

        observedSetCookieCount += setCookieValues.size();
        lastSetCookieUri = requestUri.toString();
        lastSetCookieAt = Instant.now();
        for (String headerValue : setCookieValues) {
            String cookieName = parseSetCookieName(headerValue);
            if (cookieName == null || observedSetCookieNames.contains(cookieName)) {
                continue;
            }
            observedSetCookieNames.add(cookieName);
        }
    }

    private void writeUtf8(@NonNull Path path, @NonNull String content) throws IOException {
        File file = path.toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create " + parent);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private void deleteIfExists(@NonNull Path path) {
        File file = path.toFile();
        if (file.exists()) {
            // Ignore the result because these files are purely diagnostic.
            file.delete();
        }
    }

    private void clearExistingCaptureFiles(@NonNull File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                // Ignore the result because these files are purely diagnostic.
                file.delete();
            }
        }
    }

    @NonNull
    private String buildRequestDump(@NonNull VirtFusionHttpRequest request, int statusCode) {
        List<Map.Entry<String, String>> headers = new ArrayList<>(request.getHeaders().entrySet());
        headers.sort(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER));

        StringBuilder builder = new StringBuilder();
        builder.append("capturedAt=").append(Instant.now()).append('\n');
        builder.append("uri=").append(request.getUri()).append('\n');
        if (statusCode >= 0) {
            builder.append("statusCode=").append(statusCode).append('\n');
        }
        for (Map.Entry<String, String> header : headers) {
            builder.append(header.getKey())
                    .append('=')
                    .append(redactHeader(header.getKey(), header.getValue()))
                    .append('\n');
        }
        return builder.toString();
    }

    @NonNull
    private String buildErrorDump(
            int statusCode,
            @NonNull String body,
            Exception exception,
            TlsDiagnostics tlsDiagnostics
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("capturedAt=").append(Instant.now()).append('\n');
        if (statusCode >= 0) {
            builder.append("statusCode=").append(statusCode).append('\n');
        }
        builder.append("exception=").append(exception == null ? "" : exception.toString()).append('\n');
        if (tlsDiagnostics != null) {
            builder.append('\n')
                    .append("[tls-diagnostics]\n")
                    .append(tlsDiagnostics.toDebugString());
        }
        if (!body.isBlank()) {
            builder.append('\n').append(body);
        }
        return builder.toString();
    }

    private boolean isRefreshStart(String captureKey) {
        return "server-list".equals(captureKey) || "account".equals(captureKey);
    }

    private TlsDiagnostics maybeProbeTlsDiagnostics(@NonNull URI uri, @NonNull IOException exception) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !isTlsFailure(exception)) {
            return null;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return TlsDiagnostics.forError("TLS probe skipped because request host is missing.");
        }

        int port = uri.getPort() > 0 ? uri.getPort() : 443;
        try {
            SSLContext context = buildInsecureSslContext();
            try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket()) {
                socket.connect(new InetSocketAddress(host, port), TIMEOUT_MILLIS);
                socket.setSoTimeout(TIMEOUT_MILLIS);
                socket.startHandshake();

                SSLSession session = socket.getSession();
                Certificate[] peerCertificates = session.getPeerCertificates();
                List<CertificateDetails> certificates = new ArrayList<>();
                for (int index = 0; index < peerCertificates.length; index++) {
                    Certificate certificate = peerCertificates[index];
                    if (certificate instanceof X509Certificate) {
                        X509Certificate x509Certificate = (X509Certificate) certificate;
                        certificates.add(
                                new CertificateDetails(
                                        index,
                                        x509Certificate.getSubjectX500Principal().getName(),
                                        x509Certificate.getIssuerX500Principal().getName(),
                                        x509Certificate.getNotBefore().toInstant().toString(),
                                        x509Certificate.getNotAfter().toInstant().toString()
                                )
                        );
                    } else {
                        certificates.add(
                                new CertificateDetails(
                                        index,
                                        certificate.getType(),
                                        certificate.getType(),
                                        "<unknown>",
                                        "<unknown>"
                                )
                        );
                    }
                }
                return TlsDiagnostics.forPeerCertificates(
                        host,
                        port,
                        session.getProtocol(),
                        session.getCipherSuite(),
                        certificates
                );
            }
        } catch (Exception probeException) {
            return TlsDiagnostics.forError(probeException.toString());
        }
    }

    private boolean isTlsFailure(@NonNull Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SSLException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String captureKey(@NonNull URI uri) {
        String[] segments = uri.getPath().split("/");
        int serverIndex = -1;
        for (int i = 0; i < segments.length; i++) {
            if ("server".equals(segments[i]) || "servers".equals(segments[i])) {
                serverIndex = i;
                break;
            }
        }

        if (serverIndex < 0) {
            return uri.getPath().endsWith("/account") ? "account" : null;
        }

        int remainingSegments = segments.length - serverIndex - 1;
        if (remainingSegments <= 0) {
            return "server-list";
        }

        String serverId = sanitizeSegment(segments[serverIndex + 1]);
        if (remainingSegments >= 3 && "traffic".equals(segments[serverIndex + 2]) && "statistics".equals(segments[serverIndex + 3])) {
            return "server-traffic-statistics-" + serverId;
        }
        if (remainingSegments >= 2 && "traffic".equals(segments[serverIndex + 2])) {
            return "server-traffic-" + serverId;
        }

        String query = uri.getQuery();
        if (query != null && query.toLowerCase(Locale.US).contains("remotestate=true")) {
            return "server-detail-" + serverId + "-remote-state";
        }
        return "server-detail-" + serverId;
    }

    @NonNull
    private String buildSessionRenewalDump() {
        StringBuilder builder = new StringBuilder();
        builder.append("capturedAt=").append(Instant.now()).append('\n');
        builder.append("setCookieObserved=").append(observedSetCookieCount > 0).append('\n');
        builder.append("setCookieCount=").append(observedSetCookieCount).append('\n');
        builder.append("setCookieNames=")
                .append(observedSetCookieNames.isEmpty() ? "<blank>" : String.join(", ", observedSetCookieNames))
                .append('\n');
        builder.append("sessionRenewed=").append(sessionCookieJar.hasChanged()).append('\n');
        builder.append("cookieHeaderChanged=").append(sessionCookieJar.isCookieHeaderChanged()).append('\n');
        builder.append("xsrfHeaderChanged=").append(sessionCookieJar.isXsrfHeaderChanged()).append('\n');
        builder.append("renewalApplied=").append(sessionCookieJar.hasChanged()).append('\n');
        builder.append("latestCookieHeaderLength=").append(sessionCookieJar.getCookieHeader().length()).append('\n');
        builder.append("latestXsrfHeaderLength=").append(sessionCookieJar.getXsrfHeader().length()).append('\n');
        builder.append("lastSetCookieAt=")
                .append(lastSetCookieAt == null ? "<blank>" : lastSetCookieAt.toString())
                .append('\n');
        builder.append("lastSetCookieUri=")
                .append(lastSetCookieUri == null || lastSetCookieUri.isBlank() ? "<blank>" : lastSetCookieUri)
                .append('\n');
        if (observedSetCookieCount == 0) {
            builder.append("summary=No Set-Cookie response headers were observed during this refresh.\n");
        } else if (sessionCookieJar.hasChanged()) {
            builder.append("summary=Detected Set-Cookie response headers and applied a renewed session.\n");
        } else {
            builder.append("summary=Detected Set-Cookie response headers, but the stored session values did not change.\n");
        }
        return builder.toString().trim();
    }

    @NonNull
    private String sanitizeSegment(@NonNull String raw) {
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String parseSetCookieName(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String[] parts = headerValue.split(";", 2);
        if (parts.length == 0) {
            return null;
        }
        String firstPart = parts[0].trim();
        int separatorIndex = firstPart.indexOf('=');
        if (separatorIndex <= 0) {
            return null;
        }
        return firstPart.substring(0, separatorIndex).trim();
    }

    @NonNull
    private String redactHeader(@NonNull String name, @NonNull String value) {
        if ("authorization".equalsIgnoreCase(name)
                || "cookie".equalsIgnoreCase(name)
                || "x-xsrf-token".equalsIgnoreCase(name)
                || "x-csrf-token".equalsIgnoreCase(name)) {
            return "<redacted>";
        }
        return value;
    }

    @NonNull
    private SSLContext buildInsecureSslContext() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            // Intentionally relaxed for local debugging parity with the existing debug switch.
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // Intentionally relaxed for local debugging parity with the existing debug switch.
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAll, new SecureRandom());
            return context;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to initialize insecure TLS context.", exception);
        }
    }

    private static final class TlsDiagnostics {
        private final String host;
        private final int port;
        private final String protocol;
        private final String cipherSuite;
        private final List<CertificateDetails> certificates;
        private final String error;

        private TlsDiagnostics(
                String host,
                int port,
                String protocol,
                String cipherSuite,
                List<CertificateDetails> certificates,
                String error
        ) {
            this.host = host;
            this.port = port;
            this.protocol = protocol;
            this.cipherSuite = cipherSuite;
            this.certificates = certificates;
            this.error = error;
        }

        static TlsDiagnostics forPeerCertificates(
                String host,
                int port,
                String protocol,
                String cipherSuite,
                List<CertificateDetails> certificates
        ) {
            return new TlsDiagnostics(host, port, protocol, cipherSuite, certificates, null);
        }

        static TlsDiagnostics forError(String error) {
            return new TlsDiagnostics(null, -1, null, null, List.of(), error);
        }

        @NonNull
        String toDebugString() {
            StringBuilder builder = new StringBuilder();
            if (error != null) {
                builder.append("probeError=").append(error).append('\n');
                return builder.toString();
            }

            builder.append("host=").append(host).append('\n');
            builder.append("port=").append(port).append('\n');
            builder.append("protocol=").append(protocol).append('\n');
            builder.append("cipherSuite=").append(cipherSuite).append('\n');
            builder.append("chainLength=").append(certificates.size()).append('\n');
            for (CertificateDetails certificate : certificates) {
                builder.append("cert[").append(certificate.index).append("].subject=")
                        .append(certificate.subject).append('\n');
                builder.append("cert[").append(certificate.index).append("].issuer=")
                        .append(certificate.issuer).append('\n');
                builder.append("cert[").append(certificate.index).append("].notBefore=")
                        .append(certificate.notBefore).append('\n');
                builder.append("cert[").append(certificate.index).append("].notAfter=")
                        .append(certificate.notAfter).append('\n');
            }
            return builder.toString();
        }
    }

    private static final class CertificateDetails {
        private final int index;
        private final String subject;
        private final String issuer;
        private final String notBefore;
        private final String notAfter;

        private CertificateDetails(
                int index,
                @NonNull String subject,
                @NonNull String issuer,
                @NonNull String notBefore,
                @NonNull String notAfter
        ) {
            this.index = index;
            this.subject = subject;
            this.issuer = issuer;
            this.notBefore = notBefore;
            this.notAfter = notAfter;
        }
    }
}
