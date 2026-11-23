# HTML2Markdown

Aplicación de línea de comandos desarrollada en **Java 17+** utilizando exclusivamente la **API estándar de Java**, que descarga una página web mediante HTTP/HTTPS, analiza su estructura HTML y genera un archivo equivalente en formato **Markdown** codificado en UTF-8.

---

## Arquitectura del proyecto

El sistema está dividido en componentes con responsabilidades separadas:

- **`HTML2Markdown`**: Punto de entrada (`main`), validación de argumentos de línea de comandos, coordinación general, manejo de errores y resumen de ejecución.
- **`WebClient`**: Cliente HTTP basado en `URL`, `URLConnection` y `HttpURLConnection`. Gestiona tiempos de espera (*timeouts*), códigos de estado HTTP, validación de `Content-Type`, detección de codificación (`Charset`) y límite máximo de descarga.
- **`HtmlParser`**: Analizador léxico/estructural basado en `javax.swing.text.html.parser.ParserDelegator` y `HTMLEditorKit.ParserCallback` que transforma el HTML en una secuencia de eventos/nodos.
- **`MarkdownConverter`**: Motor de transformación de nodos HTML a sintaxis Markdown. Soporta encabezados (`h1`–`h6`), párrafos, énfasis (`strong`/`b`, `em`/`i`), enlaces, imágenes, listas ordenadas y no ordenadas (incluyendo anidación), citas, código en línea/bloque y resolución de URLs relativas.
- **`MarkdownWriter`**: Escritura del archivo `.md` final en codificación `UTF-8` mediante `java.nio.file.Path` y `java.nio.file.Files`.

---

## Compilación y ejecución

### Requisitos
- **JDK 17** o superior (sin dependencias ni bibliotecas externas).

### Compilar desde la raíz del proyecto
```
javac -encoding UTF-8 -d out src/*.java
```
## Preguntas de reflexión

### 1. ¿Qué papel desempeña la aplicación dentro de una arquitectura cliente-servidor?
La aplicación actúa como un **cliente HTTP** que solicita un recurso HTML a un servidor web remoto, recibe su respuesta y lo procesa en la máquina local para convertirlo a Markdown.

### 2. ¿Qué responsabilidades corresponden al servidor y cuáles al cliente?
- **Servidor:** Recibir la petición, localizar o generar el documento, responder con un código de estado HTTP (como `200` o `404`), indicar el tipo de contenido y codificación, y enviar los datos.
- **Cliente (`HTML2Markdown`):** Validar la URL, configurar *timeouts*, comprobar que la respuesta sea exitosa y de tipo HTML, respetar el límite de tamaño, analizar la estructura del HTML, convertirlo a Markdown y guardar el archivo en UTF-8.

### 3. ¿Qué diferencia existe entre recuperar HTML e interpretar su estructura?
**Recuperar HTML** es únicamente descargar los bytes o el texto de la respuesta HTTP. **Interpretar su estructura** (*parsing*) es analizar ese contenido para identificar etiquetas de apertura y cierre, atributos (`href`, `src`, `alt`), jerarquía de elementos y texto visible.

### 4. ¿Por qué no basta reemplazar etiquetas HTML con expresiones regulares?
Porque el HTML tiene estructura jerárquica y anidada, mientras que las expresiones regulares no llevan un control de profundidad ni contexto. Pueden fallar con etiquetas anidadas del mismo tipo, atributos en distinto orden, etiquetas sin cerrar o contenido dentro de bloques de código y texto.

### 5. ¿Qué dificultades presentan los elementos anidados?
Requieren llevar seguimiento del **estado y contexto actual** mientras se recorre el documento: por ejemplo, manejar la sangría en listas dentro de listas, combinar formatos (como negrita dentro de un enlace) y evitar que el cierre de una etiqueta interna corte el formato de una etiqueta externa.

### 6. ¿Qué información puede perderse al convertir HTML a Markdown?
Se pierden estilos CSS, distribución visual de la página, clases e identificadores (`class`, `id`), scripts, formularios, metadatos del encabezado y atributos que no tienen equivalente directo en Markdown.

### 7. ¿Por qué deben resolverse las URL relativas?
Porque en una página web muchos enlaces e imágenes usan rutas relativas (por ejemplo `imagenes/logo.png` o `/docs/guia.html`). Si el archivo Markdown se guarda de forma local o en otra ruta, esas rutas dejarían de apuntar al recurso original si no se resuelven contra la URL base.

### 8. ¿Qué ventajas ofrece separar descarga, análisis y conversión en clases?
Cada clase tiene una responsabilidad clara: es más fácil probar cada parte por separado, localizar dónde ocurre un error (red, *parser* o formato Markdown), leer el código y modificar una etapa sin romper las demás.

### 9. ¿Qué limitaciones presenta `ParserDelegator` con HTML moderno?
Está basado en especificaciones antiguas de HTML (como HTML 3.2), por lo que no reconoce de forma nativa etiquetas modernas de HTML5 (`<article>`, `<section>`, `<nav>`, `<main>`, `<header>`, `<footer>`, etc.) y puede no manejar con precisión ciertas estructuras actuales.

### 10. ¿Qué modificaciones serían necesarias para convertir tablas HTML a Markdown?
Habría que:
1. Detectar `<table>`, `<thead>`, `<tbody>`, `<tr>`, `<th>` y `<td>`.
2. Guardar las celdas por fila en una estructura temporal antes de escribirlas.
3. Imprimir la fila de encabezado con `|`, agregar la fila separadora `| --- | --- |` y limpiar saltos de línea dentro de las celdas para no romper la tabla.