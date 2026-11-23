import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class HtmlParser {

    public enum TipoNodo {
        INICIO_ETIQUETA,
        FIN_ETIQUETA,
        ETIQUETA_SIMPLE,
        TEXTO
    }

    public record NodoHtml(
            TipoNodo tipo,
            String etiqueta,
            String texto,
            Map<String, String> atributos
    ) {}

    public List<NodoHtml> parsear(String html) throws IOException {
        List<NodoHtml> nodos = new ArrayList<>();
        ParserDelegator delegator = new ParserDelegator();

        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            @Override
            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                nodos.add(new NodoHtml(
                        TipoNodo.INICIO_ETIQUETA,
                        t.toString().toLowerCase(),
                        null,
                        extraerAtributos(a)
                ));
            }

            @Override
            public void handleEndTag(HTML.Tag t, int pos) {
                nodos.add(new NodoHtml(
                        TipoNodo.FIN_ETIQUETA,
                        t.toString().toLowerCase(),
                        null,
                        Map.of()
                ));
            }

            @Override
            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                nodos.add(new NodoHtml(
                        TipoNodo.ETIQUETA_SIMPLE,
                        t.toString().toLowerCase(),
                        null,
                        extraerAtributos(a)
                ));
            }

            @Override
            public void handleText(char[] data, int pos) {
                nodos.add(new NodoHtml(
                        TipoNodo.TEXTO,
                        null,
                        new String(data),
                        Map.of()
                ));
            }
        };

        delegator.parse(new StringReader(html), callback, true);
        return nodos;
    }

    private Map<String, String> extraerAtributos(AttributeSet attrs) {
        Map<String, String> map = new LinkedHashMap<>();
        if (attrs == null) {
            return map;
        }
        Enumeration<?> names = attrs.getAttributeNames();
        while (names.hasMoreElements()) {
            Object key = names.nextElement();
            Object val = attrs.getAttribute(key);
            if (key != null && val != null) {
                map.put(key.toString().toLowerCase(), val.toString());
            }
        }
        return map;
    }
}