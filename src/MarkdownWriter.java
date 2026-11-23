import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkdownWriter {

    public void guardar(Path destino, String contenido) throws IOException {
        Path padre = destino.toAbsolutePath().getParent();
        if (padre != null && !Files.exists(padre)) {
            Files.createDirectories(padre);
        }
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
    }
}