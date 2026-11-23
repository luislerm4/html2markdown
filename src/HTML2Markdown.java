import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HTML2Markdown {

    public static void main(String[] args) {
        System.out.println("HTML2Markdown");

        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Uso: java HTML2Markdown <url-http-o-https> [archivo-salida.md]");
            System.exit(1);
        }

        String urlTexto = args[0].trim();
        System.out.println("URL: " + urlTexto);

        URL url;
        try {
            URI uri = new URI(urlTexto);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new MalformedURLException("La URL debe incluir protocolo y host.");
            }
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            System.err.println("Error: URL invalida -> " + e.getMessage());
            System.exit(1);
            return;
        }

        Path rutaSalida = (args.length >= 2 && !args[1].isBlank())
                ? Path.of(args[1].trim())
                : Path.of(resolverNombrePredeterminado(url));

        WebClient webClient = new WebClient();
        HtmlParser parser = new HtmlParser();
        MarkdownConverter converter = new MarkdownConverter();
        MarkdownWriter writer = new MarkdownWriter();

        try {
            System.out.println("Conectando con el servidor...");
            String html = webClient.descargarHTML(url);
            System.out.println("Respuesta HTTP: 200");

            System.out.println("Analizando HTML...");
            List<HtmlParser.NodoHtml> nodos = parser.parsear(html);

            System.out.println("Convirtiendo a Markdown...");
            String markdown = converter.convertir(nodos, url.toString());

            writer.guardar(rutaSalida, markdown);
            long tamanoBytes = Files.size(rutaSalida);

            System.out.println("Archivo generado: " + rutaSalida);
            System.out.println("Tamano: " + tamanoBytes + " bytes");
            System.out.println("Conversion finalizada.");
        } catch (IOException e) {
            System.err.println("Error durante el proceso: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String resolverNombrePredeterminado(URL url) {
        String path = url.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "salida.md";
        }
        String ultimoSegmento = path.substring(path.lastIndexOf('/') + 1);
        if (ultimoSegmento.isBlank()) {
            return "salida.md";
        }
        int punto = ultimoSegmento.lastIndexOf('.');
        String base = (punto > 0) ? ultimoSegmento.substring(0, punto) : ultimoSegmento;
        return base + ".md";
    }
}