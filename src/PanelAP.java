import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * NIVEL 2 de la jerarquía de Chomsky (Tipo 2): LENGUAJES LIBRES DE CONTEXTO.
 * Máquina: Autómata de Pila  M = (Q, Σ, Γ, δ, q0, Z, F).
 *
 * Es un autómata finito al que se le agrega una PILA (memoria LIFO: el último en entrar
 * es el primero en salir). Cada transición depende del estado, del símbolo leído (o λ)
 * y del símbolo que está en el tope de la pila.
 * Se acepta por estado final: se leyó toda la cadena y la máquina quedó en un estado de F.
 */
public class PanelAP extends PanelSimulador {

    private static final String[] MAQUINAS = {
            "aⁿbⁿ  (n ≥ 1)",
            "Paréntesis balanceados",
            "Palíndromos con marca central  w c w^R"
    };

    // ---- Definición formal ----
    // Cada regla es {estado, símbolo de entrada (o λ), tope de la pila, nuevo estado, cadena que se apila}
    // La cadena que se apila se escribe con su primer símbolo como nuevo tope. "λ" = no apilar nada.
    private String[][] reglas;
    private final List<String> estados = new ArrayList<>();
    private final Set<String> finales = new HashSet<>();
    private String sigma, gamma, lenguaje, gramatica;
    private String[] ejemplos;

    // ---- Configuración durante la simulación ----
    private String estado;
    private int posicion;
    private final ArrayList<Character> pila = new ArrayList<>(); // el último elemento es el tope
    private String operacion = "";
    private int apilados = 0;           // símbolos nuevos en el tope (para la animación)
    private Character desapilado = null; // símbolo que salió (para la animación)

    public PanelAP(Runnable volver) {
        super("Nivel 2 · Lenguajes libres de contexto · Autómata de Pila (AP)", MAQUINAS, volver);
        cambiarMaquina(0);
    }

    // ------------------------------------------------------------------
    //  Definición de los autómatas de pila
    // ------------------------------------------------------------------
    @Override
    protected void seleccionarMaquina(int i) {
        finales.clear();
        if (i == 0) {
            lenguaje = "L = { aⁿbⁿ | n ≥ 1 }";
            gramatica = "S → aSb | ab";
            sigma = "ab";
            gamma = "{A, Z}";
            // Por cada 'a' se apila una A; por cada 'b' se desapila una A.
            reglas = new String[][]{
                    {"q0", "a", "Z", "q0", "AZ"},
                    {"q0", "a", "A", "q0", "AA"},
                    {"q0", "b", "A", "q1", "λ"},
                    {"q1", "b", "A", "q1", "λ"},
                    {"q1", "λ", "Z", "q2", "Z"}};
            finales.add("q2");
            ejemplos = new String[]{"aabb  →  válida", "ab  →  válida", "aaabbb  →  válida", "aaaabbbb  →  válida",
                    "aab  →  inválida (sobra una a)", "abb  →  inválida (sobra una b)", "abab  →  inválida", "λ  →  inválida (n ≥ 1)"};
        } else if (i == 1) {
            lenguaje = "L = { w sobre {(,)} | los paréntesis están balanceados }";
            gramatica = "S → (S)S | λ";
            sigma = "()";
            gamma = "{P, Z}";
            // q0 = no hay paréntesis abiertos; q1 = hay al menos uno abierto (una P por cada uno).
            reglas = new String[][]{
                    {"q0", "(", "Z", "q1", "PZ"},
                    {"q1", "(", "P", "q1", "PP"},
                    {"q1", ")", "P", "q1", "λ"},
                    {"q1", "λ", "Z", "q0", "Z"}};
            finales.add("q0");
            ejemplos = new String[]{"(())()  →  válida", "((()))  →  válida", "()()()  →  válida", "λ  →  válida (vacía)",
                    "(()  →  inválida (falta cerrar)", "())(  →  inválida", ")(  →  inválida"};
        } else {
            lenguaje = "L = { w c w<sup>R</sup> | w sobre {a,b} }  (w<sup>R</sup> = w al revés)";
            gramatica = "S → aSa | bSb | c";
            sigma = "abc";
            gamma = "{A, B, Z}";
            // Antes de la c se apila lo leído; después de la c se compara con el tope.
            reglas = new String[][]{
                    {"q0", "a", "Z", "q0", "AZ"},
                    {"q0", "a", "A", "q0", "AA"},
                    {"q0", "a", "B", "q0", "AB"},
                    {"q0", "b", "Z", "q0", "BZ"},
                    {"q0", "b", "A", "q0", "BA"},
                    {"q0", "b", "B", "q0", "BB"},
                    {"q0", "c", "Z", "q1", "Z"},
                    {"q0", "c", "A", "q1", "A"},
                    {"q0", "c", "B", "q1", "B"},
                    {"q1", "a", "A", "q1", "λ"},
                    {"q1", "b", "B", "q1", "λ"},
                    {"q1", "λ", "Z", "q2", "Z"}};
            finales.add("q2");
            ejemplos = new String[]{"abcba  →  válida", "abbcbba  →  válida", "c  →  válida (w vacía)", "baacaab  →  válida",
                    "abcab  →  inválida", "abba  →  inválida (sin c)", "acaa  →  inválida"};
        }
        estados.clear();
        for (String[] r : reglas) {
            if (!estados.contains(r[0])) estados.add(r[0]);
            if (!estados.contains(r[3])) estados.add(r[3]);
        }
    }

    // ------------------------------------------------------------------
    //  Simulación
    // ------------------------------------------------------------------
    @Override
    protected void iniciar(String w) {
        estado = "q0";
        posicion = 0;
        pila.clear();
        pila.add('Z'); // símbolo inicial de la pila (marca el fondo)
        operacion = "Pila inicial: solo Z (fondo)";
        apilados = 0;
        desapilado = null;
        log("Configuración inicial: (q0, " + (w.isEmpty() ? "λ" : w) + ", Z)");
        if (w.isEmpty() && finales.contains(estado)) {
            terminado = true;
            aceptada = true;
            log("La cadena vacía se acepta porque q0 es estado de aceptación.");
        }
    }

    @Override
    protected void paso() {
        apilados = 0;
        desapilado = null;
        char tope = pila.isEmpty() ? ' ' : pila.get(pila.size() - 1);
        String simbolo = posicion < cadena.length() ? String.valueOf(cadena.charAt(posicion)) : null;

        // 1) Se busca una transición que lea el símbolo actual; si no hay, una transición λ.
        int r = simbolo != null ? buscar(estado, simbolo, tope) : -1;
        boolean lee = r >= 0;
        if (r < 0) r = buscar(estado, "λ", tope);
        if (r < 0) {
            terminado = true;
            aceptada = false;
            log("No hay transición para (" + estado + ", " + (simbolo == null ? "λ" : simbolo) + ", " + tope + "). La máquina se detiene.");
            log(simbolo != null ? "Quedaron símbolos sin leer → RECHAZADA"
                    : "Se leyó toda la cadena pero " + estado + " no es de aceptación → RECHAZADA");
            return;
        }
        String[] t = reglas[r];
        transicionActual = r;

        // 2) Se saca el tope y se apila la cadena indicada (su primer símbolo queda como nuevo tope).
        pila.remove(pila.size() - 1);
        String ap = t[4].equals("λ") ? "" : t[4];
        for (int i = ap.length() - 1; i >= 0; i--) pila.add(ap.charAt(i));

        // 3) Texto y datos para la animación
        String topeTxt = String.valueOf(tope);
        if (ap.isEmpty()) {
            operacion = "POP: se desapila " + tope;
            desapilado = tope;
        } else if (ap.equals(topeTxt)) {
            operacion = "La pila no cambia";
        } else if (ap.endsWith(topeTxt)) {
            apilados = ap.length() - 1;
            operacion = "PUSH: se apila " + ap.substring(0, ap.length() - 1);
        } else {
            desapilado = tope;
            apilados = ap.length();
            operacion = "Se reemplaza " + tope + " por " + ap;
        }
        estado = t[3];
        if (lee) posicion++;
        log("δ(" + t[0] + ", " + t[1] + ", " + t[2] + ") = (" + t[3] + ", " + t[4] + ")  " + operacion
                + "  | pila: " + textoPila());

        // 4) Aceptación por estado final
        if (posicion == cadena.length() && finales.contains(estado)) {
            terminado = true;
            aceptada = true;
            log("Se leyó toda la cadena y " + estado + " es de aceptación → ACEPTADA");
        }
    }

    private int buscar(String q, String entrada, char tope) {
        for (int i = 0; i < reglas.length; i++)
            if (reglas[i][0].equals(q) && reglas[i][1].equals(entrada) && reglas[i][2].charAt(0) == tope) return i;
        return -1;
    }

    private String textoPila() {
        StringBuilder sb = new StringBuilder();
        for (int i = pila.size() - 1; i >= 0; i--) sb.append(pila.get(i));
        return sb.length() == 0 ? "vacía" : sb.toString();
    }

    // ------------------------------------------------------------------
    //  Textos
    // ------------------------------------------------------------------
    @Override protected String alfabeto() { return sigma; }
    @Override protected String[] ejemplos() { return ejemplos; }

    @Override
    protected String[] transiciones() {
        String[] l = new String[reglas.length];
        for (int i = 0; i < reglas.length; i++) {
            String[] t = reglas[i];
            l[i] = "δ(" + t[0] + ", " + t[1] + ", " + t[2] + ") = (" + t[3] + ", " + t[4] + ")";
        }
        return l;
    }

    @Override
    protected String descripcion() {
        return "<b>Lenguaje:</b> " + lenguaje + " &nbsp;&nbsp; <b>Gramática:</b> " + gramatica
                + "<br><b>Q</b> = " + estados + " &nbsp; <b>Σ</b> = " + conjunto(sigma) + " &nbsp; <b>Γ</b> = " + gamma
                + " &nbsp; <b>q0</b> = q0 &nbsp; <b>Z</b> = fondo &nbsp; <b>F</b> = " + finales
                + " &nbsp;&nbsp; <i>Memoria: una pila (LIFO).</i>";
    }

    @Override
    protected String teoria() {
        return "<h2 style='color:#1F4E79'>Nivel 2 (Tipo 2): Lenguajes libres de contexto</h2>"
                + "<p>Se generan con <b>gramáticas libres de contexto</b>: todas sus reglas tienen la forma <b>A → γ</b>, es decir, "
                + "a la izquierda hay una sola variable que se puede reemplazar sin importar lo que la rodea (el \"contexto\"). "
                + "Con ellas se describe la sintaxis de casi todos los lenguajes de programación (expresiones, bloques, paréntesis).</p>"
                + "<p>Gramática del lenguaje actual: <b>" + gramatica + "</b></p>"
                + "<h3>Definición formal del autómata de pila</h3>"
                + "<p>M = (Q, Σ, Γ, δ, q0, Z, F): Q estados, Σ alfabeto de entrada, Γ alfabeto de la pila, "
                + "δ: Q × (Σ o λ) × Γ → Q × Γ*, q0 estado inicial, Z símbolo inicial de la pila y F estados de aceptación.</p>"
                + "<p>Una transición δ(q, a, X) = (p, γ) significa: estando en q, leyendo a y con X en el tope, "
                + "pasar a p, sacar X y poner γ en la pila (el primer símbolo de γ queda arriba). Si γ = λ solo se desapila; "
                + "si γ = AX se apila A encima de X; si γ = X la pila no cambia.</p>"
                + "<p>Las transiciones con entrada <b>λ</b> (transiciones lambda) cambian de estado y modifican la pila <b>sin leer</b> "
                + "ningún símbolo de la cadena. En este simulador se usan al final para pasar al estado de aceptación cuando el tope es Z. "
                + "Primero se intenta leer un símbolo y, si no hay transición para él, se aplica la transición λ. "
                + "(En algunos libros λ se escribe ε.)</p>"
                + "<h3>Memoria LIFO</h3>"
                + "<p>La pila solo permite trabajar con el elemento de arriba (el tope): el último que entra es el primero que sale. "
                + "Esa memoria es suficiente para \"contar y descontar\": en aⁿbⁿ cada a apila una A y cada b desapila una A. "
                + "Si al final solo queda Z, había la misma cantidad de a y b. En los paréntesis pasa lo mismo con P, y en w c w<sup>R</sup> "
                + "la pila devuelve los símbolos al revés, que es justo lo que se necesita para comparar con w<sup>R</sup>.</p>"
                + "<h3>Aceptación</h3>"
                + "<p>Este simulador acepta por <b>estado final</b>: se consumió toda la cadena y la máquina está en un estado de F. "
                + "Los tres autómatas son deterministas (en cada configuración hay como máximo una transición posible).</p>"
                + "<h3>Límite del nivel</h3>"
                + "<p>Con una sola pila no se pueden comparar tres cantidades a la vez: { aⁿbⁿcⁿ } no es libre de contexto "
                + "(lema de bombeo para LLC). Al desapilar las A para comparar con las b se pierde la información para las c. "
                + "Para eso se necesita una cinta que se pueda leer y escribir en ambas direcciones: el Nivel 1.</p>";
    }

    // ------------------------------------------------------------------
    //  Dibujo
    // ------------------------------------------------------------------
    @Override
    protected void dibujar(Graphics2D g, int w, int h) {
        int anchoDiagrama = w - 240; // la parte derecha queda para la pila
        int n = estados.size();
        double sep = Math.min(240, (anchoDiagrama - 160) / (double) Math.max(1, n - 1));
        double y = h * 0.40;
        Map<String, Point2D> pos = new HashMap<>();
        for (int i = 0; i < n; i++) pos.put(estados.get(i), new Point2D.Double(90 + i * sep, y));

        // Etiquetas agrupadas por pareja de estados: "a, Z / AZ"
        Map<String, String> etiquetas = new LinkedHashMap<>();
        for (String[] t : reglas) {
            String par = t[0] + " " + t[3];
            String e = t[1] + "," + t[2] + "/" + t[4];
            etiquetas.put(par, etiquetas.containsKey(par) ? etiquetas.get(par) + "\n" + e : e);
        }
        String parResaltado = transicionActual >= 0 ? reglas[transicionActual][0] + " " + reglas[transicionActual][3] : null;
        for (Map.Entry<String, String> e : etiquetas.entrySet()) {
            String[] par = e.getKey().split(" ");
            Point2D a = pos.get(par[0]);
            boolean vuelta = !par[0].equals(par[1]) && etiquetas.containsKey(par[1] + " " + par[0]);
            dibujarTransicion(g, a, pos.get(par[1]), e.getValue(), e.getKey().equals(parResaltado),
                    vuelta ? 40 : 0, new Point2D.Double(a.getX(), a.getY() + 200));
        }
        for (String q : estados) {
            Point2D p = pos.get(q);
            dibujarEstado(g, p.getX(), p.getY(), q, q.equals("q0"), finales.contains(q), q.equals(estado));
        }
        texto(g, "Notación de las etiquetas:  entrada , tope / lo que se apila", 20, 25, 13, true, AZUL);

        // Cinta de entrada
        int yCinta = (int) (h * 0.62);
        String[] celdas = new String[Math.max(1, cadena.length())];
        if (cadena.isEmpty()) celdas[0] = "λ";
        for (int i = 0; i < cadena.length(); i++) celdas[i] = String.valueOf(cadena.charAt(i));
        texto(g, "Cinta de entrada:", 20, yCinta - 10, 13, true, AZUL);
        dibujarCinta(g, anchoDiagrama, yCinta, celdas, cadena.isEmpty() ? -1 : posicion, posicion, terminado ? "fin" : "cabezal");
        dibujarCaja(g, 20, h - 110, new String[]{
                "Estado: " + estado + "     Por leer: " + (posicion >= cadena.length() ? "λ" : cadena.substring(posicion)),
                "Operación: " + operacion,
                "Pila (tope a la izquierda): " + textoPila()});

        dibujarPila(g, w - 190, h);
    }

    /** Dibuja la pila como un recipiente. Los símbolos nuevos bajan y el que sale sube y se desvanece. */
    private void dibujarPila(Graphics2D g, int x, int h) {
        int ancho = 90, alto = 32, fondo = h - 50;
        double p = progreso();
        texto(g, "PILA (LIFO)", x + 2, 30, 16, true, AZUL);
        int maxVisibles = (fondo - 70) / alto;
        int inicio = Math.max(0, pila.size() - maxVisibles); // si no caben se muestran los de arriba
        for (int i = inicio; i < pila.size(); i++) {
            int nivel = i - inicio;
            double yy = fondo - (nivel + 1) * alto;
            boolean nuevo = i >= pila.size() - apilados;
            if (nuevo && p < 1) yy -= (1 - p) * 120; // cae desde arriba
            boolean esTope = i == pila.size() - 1;
            g.setColor(esTope ? AMARILLO : (nuevo ? VERDE_CLARO : AZUL_CLARO));
            g.fill(new Rectangle2D.Double(x + 6, yy + 2, ancho - 12, alto - 4));
            g.setColor(AZUL);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Rectangle2D.Double(x + 6, yy + 2, ancho - 12, alto - 4));
            String s = String.valueOf(pila.get(i));
            g.setFont(new Font("Monospaced", Font.BOLD, 20));
            g.setColor(TEXTO);
            g.drawString(s, x + ancho / 2 - 6, (float) yy + alto / 2 + 7);
            if (esTope) texto(g, "<- tope", x + ancho + 6, yy + alto / 2.0 + 5, 13, true, ROJO);
        }
        if (inicio > 0) texto(g, "(" + inicio + " más abajo)", x + 4, fondo + 18, 11, false, TEXTO);
        // El símbolo desapilado sube y se desvanece
        if (desapilado != null && p < 1) {
            double yy = fondo - (pila.size() - inicio + 1) * alto - p * 90;
            Composite antes = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - p)));
            g.setColor(new Color(240, 170, 160));
            g.fill(new Rectangle2D.Double(x + 6, yy + 2, ancho - 12, alto - 4));
            g.setColor(ROJO);
            g.setFont(new Font("Monospaced", Font.BOLD, 20));
            g.drawString(String.valueOf(desapilado), x + ancho / 2 - 6, (float) yy + alto / 2 + 7);
            g.setComposite(antes);
        }
        // Recipiente en forma de U
        g.setColor(TEXTO);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Line2D.Double(x, 50, x, fondo));
        g.draw(new Line2D.Double(x + ancho, 50, x + ancho, fondo));
        g.draw(new Line2D.Double(x, fondo, x + ancho, fondo));
        texto(g, "Z = fondo de la pila", x - 10, fondo + 34, 12, false, TEXTO);
        if (pila.isEmpty()) texto(g, "(vacía)", x + 20, fondo - 10, 13, false, Color.GRAY);
    }
}
