import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Pattern CHARSET_PATTERN =
            Pattern.compile("charset\\s*=\\s*\"?([^;\\s\"]+)\"?", Pattern.CASE_INSENSITIVE);

    public String descargarHTML(URL url) throws IOException {
        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new IOException("Solo se admiten URLs HTTP o HTTPS: " + url);
        }

        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "HTML2Markdown-Java/1.0");

        if (conn instanceof HttpURLConnection httpConn) {
            httpConn.setInstanceFollowRedirects(true);
            int statusCode = httpConn.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Respuesta HTTP no exitosa: " + statusCode);
            }
        }

        String contentType = conn.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
            throw new IOException("El contenido no es HTML (Content-Type: " + contentType + ")");
        }

        Charset charset = extraerCharset(contentType);

        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int leidos;

            while ((leidos = in.read(buffer)) != -1) {
                totalBytes += leidos;
                if (totalBytes > MAX_BYTES) {
                    throw new IOException("El documento HTML excede el limite permitido de " + MAX_BYTES + " bytes.");
                }
                out.write(buffer, 0, leidos);
            }

            return out.toString(charset);
        } finally {
            if (conn instanceof HttpURLConnection httpConn) {
                httpConn.disconnect();
            }
        }
    }

    private Charset extraerCharset(String contentType) {
        Matcher matcher = CHARSET_PATTERN.matcher(contentType);
        if (matcher.find()) {
            String nombreCharset = matcher.group(1).trim();
            try {
                if (Charset.isSupported(nombreCharset)) {
                    return Charset.forName(nombreCharset);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return StandardCharsets.UTF_8;
    }
}