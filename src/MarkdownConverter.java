import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarkdownConverter {

    private static final Set<String> ETIQUETAS_IGNORADAS = Set.of("script", "style", "head");

    private static class ContextoLista {
        final boolean ordenada;
        int contador;

        ContextoLista(boolean ordenada) {
            this.ordenada = ordenada;
            this.contador = 0;
        }
    }

    public String convertir(List<HtmlParser.NodoHtml> nodos, String urlBase) {
        StringBuilder sb = new StringBuilder();
        Deque<ContextoLista> pilaListas = new ArrayDeque<>();

        int nivelIgnorado = 0;
        boolean enPre = false;
        boolean enCode = false;
        boolean enBlockquote = false;
        String enlaceActivoHref = null;
        StringBuilder textoEnlace = new StringBuilder();

        for (HtmlParser.NodoHtml nodo : nodos) {
            switch (nodo.tipo()) {
                case INICIO_ETIQUETA -> {
                    String tag = nodo.etiqueta();
                    if (ETIQUETAS_IGNORADAS.contains(tag)) {
                        nivelIgnorado++;
                        break;
                    }
                    if (nivelIgnorado > 0) {
                        break;
                    }

                    switch (tag) {
                        case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            asegurarLineaNueva(sb, true);
                            int nivel = tag.charAt(1) - '0';
                            sb.append("#".repeat(nivel)).append(" ");
                        }
                        case "p" -> asegurarLineaNueva(sb, true);
                        case "blockquote" -> {
                            enBlockquote = true;
                            asegurarLineaNueva(sb, true);
                            sb.append("> ");
                        }
                        case "ul" -> {
                            asegurarLineaNueva(sb, pilaListas.isEmpty());
                            pilaListas.push(new ContextoLista(false));
                        }
                        case "ol" -> {
                            asegurarLineaNueva(sb, pilaListas.isEmpty());
                            pilaListas.push(new ContextoLista(true));
                        }
                        case "li" -> {
                            asegurarLineaNueva(sb, false);
                            int profundidad = Math.max(0, pilaListas.size() - 1);
                            sb.append("  ".repeat(profundidad));
                            ContextoLista listaActual = pilaListas.peek();
                            if (listaActual != null && listaActual.ordenada) {
                                listaActual.contador++;
                                sb.append(listaActual.contador).append(". ");
                            } else {
                                sb.append("- ");
                            }
                        }
                        case "strong", "b" -> sb.append("**");
                        case "em", "i" -> sb.append("*");
                        case "code" -> {
                            enCode = true;
                            if (!enPre) {
                                sb.append("`");
                            }
                        }
                        case "pre" -> {
                            enPre = true;
                            asegurarLineaNueva(sb, true);
                            sb.append("```\n");
                        }
                        case "a" -> {
                            enlaceActivoHref = resolverUrl(urlBase, nodo.atributos().get("href"));
                            textoEnlace.setLength(0);
                        }
                        default -> {
                        }
                    }
                }

                case FIN_ETIQUETA -> {
                    String tag = nodo.etiqueta();
                    if (ETIQUETAS_IGNORADAS.contains(tag)) {
                        if (nivelIgnorado > 0) {
                            nivelIgnorado--;
                        }
                        break;
                    }
                    if (nivelIgnorado > 0) {
                        break;
                    }

                    switch (tag) {
                        case "h1", "h2", "h3", "h4", "h5", "h6", "p", "blockquote" -> {
                            if ("blockquote".equals(tag)) {
                                enBlockquote = false;
                            }
                            sb.append("\n\n");
                        }
                        case "ul", "ol" -> {
                            if (!pilaListas.isEmpty()) {
                                pilaListas.pop();
                            }
                            if (pilaListas.isEmpty()) {
                                sb.append("\n\n");
                            } else {
                                sb.append("\n");
                            }
                        }
                        case "li" -> sb.append("\n");
                        case "strong", "b" -> sb.append("**");
                        case "em", "i" -> sb.append("*");
                        case "code" -> {
                            if (!enPre) {
                                sb.append("`");
                            }
                            enCode = false;
                        }
                        case "pre" -> {
                            enPre = false;
                            asegurarLineaNueva(sb, false);
                            sb.append("```\n\n");
                        }
                        case "a" -> {
                            String texto = textoEnlace.toString().trim();
                            if (texto.isEmpty() && enlaceActivoHref != null) {
                                texto = enlaceActivoHref;
                            }
                            if (enlaceActivoHref != null && !enlaceActivoHref.isBlank()) {
                                sb.append("[").append(texto).append("](").append(enlaceActivoHref).append(")");
                            } else {
                                sb.append(texto);
                            }
                            enlaceActivoHref = null;
                            textoEnlace.setLength(0);
                        }
                        default -> {
                        }
                    }
                }

                case ETIQUETA_SIMPLE -> {
                    if (nivelIgnorado > 0) {
                        break;
                    }
                    String tag = nodo.etiqueta();
                    Map<String, String> attrs = nodo.atributos();

                    if ("br".equals(tag)) {
                        sb.append("  \n");
                    } else if ("hr".equals(tag)) {
                        asegurarLineaNueva(sb, true);
                        sb.append("---\n\n");
                    } else if ("img".equals(tag)) {
                        String alt = attrs.getOrDefault("alt", "").trim();
                        String src = resolverUrl(urlBase, attrs.get("src"));
                        if (src != null && !src.isBlank()) {
                            sb.append("![").append(escaparMarkdown(alt)).append("](").append(src).append(")");
                        }
                    }
                }

                case TEXTO -> {
                    if (nivelIgnorado > 0 || nodo.texto() == null) {
                        break;
                    }
                    String fragmento = nodo.texto();
                    if (enPre) {
                        sb.append(fragmento);
                        break;
                    }

                    String limpio = normalizarEspacios(fragmento);
                    if (limpio.isEmpty()) {
                        break;
                    }

                    if (enlaceActivoHref != null) {
                        textoEnlace.append(limpio);
                    } else {
                        String salidaTexto = enCode ? limpio : escaparMarkdown(limpio);
                        if (enBlockquote) {
                            salidaTexto = salidaTexto.replace("\n", "\n> ");
                        }
                        sb.append(salidaTexto);
                    }
                }
            }
        }

        return limpiarSaltosExcesivos(sb.toString()).trim() + "\n";
    }

    private String normalizarEspacios(String texto) {
        return texto.replaceAll("[ \\t\\r\\n]+", " ");
    }

    private String escaparMarkdown(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`");
    }

    private String resolverUrl(String urlBase, String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        String refTrim = referencia.trim();
        try {
            URI baseUri = URI.create(urlBase);
            return baseUri.resolve(refTrim).toString();
        } catch (IllegalArgumentException e) {
            return refTrim;
        }
    }

    private void asegurarLineaNueva(StringBuilder sb, boolean lineaEnBlanco) {
        if (sb.length() == 0) {
            return;
        }
        if (lineaEnBlanco) {
            if (!sb.toString().endsWith("\n\n")) {
                if (sb.charAt(sb.length() - 1) == '\n') {
                    sb.append('\n');
                } else {
                    sb.append("\n\n");
                }
            }
        } else if (sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }
    }

    private String limpiarSaltosExcesivos(String texto) {
        return texto.replaceAll("\\n{3,}", "\n\n");
    }
}