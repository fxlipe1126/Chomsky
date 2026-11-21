import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.util.*;
import java.util.List;

/**
 * NIVEL 1 de la jerarquía de Chomsky (Tipo 1): LENGUAJES SENSIBLES AL CONTEXTO.
 * Máquina: Autómata Linealmente Acotado (ALA).
 *
 * Es una máquina de Turing cuya cinta está limitada al tamaño de la entrada.
 * La cadena se escribe entre dos delimitadores de frontera:  < w >
 * El cabezal puede leer, escribir y moverse a la izquierda (L) o a la derecha (R),
 * pero NUNCA puede pasar por encima de < ni de >, ni borrarlos.
 * Por eso la memoria está acotada linealmente por la longitud de la entrada.
 */
public class PanelALA extends PanelSimulador {

    private static final String[] MAQUINAS = {
            "aⁿbⁿcⁿ  (n ≥ 1)",
            "Lenguaje copia  w#w  (w sobre {a,b})"
    };

    // Cada regla es {estado, símbolo leído, nuevo estado, símbolo escrito, movimiento (L, R o S)}
    private String[][] reglas;
    private final List<String> estados = new ArrayList<>();
    private String sigma, gamma, lenguaje, gramatica, idea;
    private String[] ejemplos;

    // ---- Configuración ----
    private final ArrayList<Character> cinta = new ArrayList<>();
    private int cabezal, cabezalAnterior;
    private String estado;
    private String accion = "";

    public PanelALA(Runnable volver) {
        super("Nivel 1 · Lenguajes sensibles al contexto · Autómata Linealmente Acotado (ALA)", MAQUINAS, volver);
        etiquetaBucleAlLado = true;
        cambiarMaquina(0);
    }

    // ------------------------------------------------------------------
    //  Definición de las máquinas
    // ------------------------------------------------------------------
    @Override
    protected void seleccionarMaquina(int i) {
        if (i == 0) {
            lenguaje = "L = { aⁿbⁿcⁿ | n ≥ 1 }";
            gramatica = "S → aSBC | aBC,  CB → BC,  aB → ab,  bB → bb,  bC → bc,  cC → cc";
            sigma = "abc";
            gamma = "{a, b, c, X, Y, Z, <, >}";
            idea = "En cada pasada marca una a con X, una b con Y y una c con Z, y vuelve a la izquierda. "
                    + "Cuando ya no quedan a, revisa que solo queden Y y Z.";
            reglas = new String[][]{
                    {"q0", "a", "q1", "X", "R"},   // marca una a
                    {"q0", "Y", "q4", "Y", "R"},   // ya no hay a: a verificar
                    {"q1", "a", "q1", "a", "R"},
                    {"q1", "Y", "q1", "Y", "R"},
                    {"q1", "b", "q2", "Y", "R"},   // marca una b
                    {"q2", "b", "q2", "b", "R"},
                    {"q2", "Z", "q2", "Z", "R"},
                    {"q2", "c", "q3", "Z", "L"},   // marca una c y regresa
                    {"q3", "a", "q3", "a", "L"},
                    {"q3", "b", "q3", "b", "L"},
                    {"q3", "Y", "q3", "Y", "L"},
                    {"q3", "Z", "q3", "Z", "L"},
                    {"q3", "X", "q0", "X", "R"},   // llegó a la última a marcada
                    {"q4", "Y", "q4", "Y", "R"},
                    {"q4", "Z", "q5", "Z", "R"},
                    {"q5", "Z", "q5", "Z", "R"},
                    {"q5", ">", "qa", ">", "S"}};  // llegó a la frontera derecha: acepta
            ejemplos = new String[]{"abc  →  válida", "aabbcc  →  válida", "aaabbbccc  →  válida",
                    "aabbc  →  inválida (falta una c)", "abcabc  →  inválida", "aabcc  →  inválida", "acb  →  inválida"};
        } else {
            lenguaje = "L = { w#w | w formada con a y b }";
            gramatica = "existe una gramática sensible al contexto equivalente (el lenguaje no es libre de contexto)";
            sigma = "ab#";
            gamma = "{a, b, #, X, <, >}";
            idea = "Toma el primer símbolo sin marcar de la izquierda, lo marca con X y lo \"lleva en el estado\" "
                    + "(q1 = recuerda a, q2 = recuerda b) hasta el primer símbolo sin marcar después de #.";
            reglas = new String[][]{
                    {"q0", "a", "q1", "X", "R"},   // recuerda a
                    {"q0", "b", "q2", "X", "R"},   // recuerda b
                    {"q0", "#", "q7", "#", "R"},   // lado izquierdo terminado
                    {"q1", "a", "q1", "a", "R"},
                    {"q1", "b", "q1", "b", "R"},
                    {"q1", "#", "q3", "#", "R"},
                    {"q2", "a", "q2", "a", "R"},
                    {"q2", "b", "q2", "b", "R"},
                    {"q2", "#", "q4", "#", "R"},
                    {"q3", "X", "q3", "X", "R"},
                    {"q3", "a", "q5", "X", "L"},   // coincide la a
                    {"q4", "X", "q4", "X", "R"},
                    {"q4", "b", "q5", "X", "L"},   // coincide la b
                    {"q5", "X", "q5", "X", "L"},
                    {"q5", "#", "q6", "#", "L"},
                    {"q6", "a", "q6", "a", "L"},
                    {"q6", "b", "q6", "b", "L"},
                    {"q6", "X", "q0", "X", "R"},
                    {"q7", "X", "q7", "X", "R"},
                    {"q7", ">", "qa", ">", "S"}};
            ejemplos = new String[]{"ab#ab  →  válida", "abb#abb  →  válida", "#  →  válida (w vacía)", "baab#baab  →  válida",
                    "ab#ba  →  inválida", "ab#abb  →  inválida", "abab  →  inválida (sin #)"};
        }
        estados.clear();
        for (String[] r : reglas) {
            if (!estados.contains(r[0])) estados.add(r[0]);
            if (!estados.contains(r[2])) estados.add(r[2]);
        }
        Collections.sort(estados); // q0, q1, ..., qa / qf
    }

    // ------------------------------------------------------------------
    //  Simulación
    // ------------------------------------------------------------------
    @Override
    protected void iniciar(String w) {
        cinta.clear();
        cinta.add('<');                                  // delimitador izquierdo
        for (char c : w.toCharArray()) cinta.add(c);
        cinta.add('>');                                  // delimitador derecho
        cabezal = 1;                                     // primer símbolo de la entrada
        cabezalAnterior = 1;
        estado = "q0";
        accion = "—";
        log("Cinta acotada de " + cinta.size() + " celdas (|w| + 2). Configuración: " + configuracion());
    }

    @Override
    protected void paso() {
        char leido = cinta.get(cabezal);
        int r = buscar(estado, leido);
        if (r < 0) {
            terminado = true;
            aceptada = false;
            log("δ(" + estado + ", " + leido + ") no está definida. Se detiene en un estado que no es de aceptación → RECHAZADA");
            return;
        }
        String[] t = reglas[r];
        transicionActual = r;
        char escrito = t[3].charAt(0);
        cinta.set(cabezal, escrito);
        estado = t[2];
        cabezalAnterior = cabezal;
        if (t[4].equals("R")) cabezal++;
        else if (t[4].equals("L")) cabezal--;
        accion = "lee " + leido + ", escribe " + escrito + ", "
                + (t[4].equals("R") ? "mueve a la derecha" : t[4].equals("L") ? "mueve a la izquierda" : "no se mueve");
        log("δ(" + t[0] + ", " + t[1] + ") = (" + t[2] + ", " + t[3] + ", " + t[4] + ")   " + configuracion());

        // Restricción del ALA: el cabezal no puede salir de los delimitadores
        if (cabezal < 0 || cabezal >= cinta.size()) {
            cabezal = Math.max(0, Math.min(cinta.size() - 1, cabezal));
            terminado = true;
            aceptada = false;
            mensajeFinal = "RECHAZADA: el cabezal intentó salir de la cinta acotada";
            log("Error: el cabezal intentó salir de los delimitadores < >");
            return;
        }
        if (estado.equals("qa")) {
            terminado = true;
            aceptada = true;
            log("Llegó al estado de aceptación qa → ACEPTADA");
        }
    }

    private int buscar(String q, char s) {
        for (int i = 0; i < reglas.length; i++)
            if (reglas[i][0].equals(q) && reglas[i][1].charAt(0) == s) return i;
        return -1;
    }

    /** Descripción instantánea: la cinta con el estado escrito justo antes del símbolo que se está leyendo. */
    private String configuracion() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cinta.size(); i++) {
            if (i == cabezal) sb.append('[').append(estado).append(']');
            sb.append(cinta.get(i));
        }
        return sb.toString();
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
            l[i] = "δ(" + t[0] + ", " + t[1] + ") = (" + t[2] + ", " + t[3] + ", " + t[4] + ")";
        }
        return l;
    }

    @Override
    protected String descripcion() {
        return "<b>Lenguaje:</b> " + lenguaje + " &nbsp;&nbsp; <b>Idea:</b> " + idea
                + "<br><b>Σ</b> = " + conjunto(sigma) + " &nbsp; <b>Γ</b> = " + gamma
                + " &nbsp; <b>Aceptación:</b> qa &nbsp;&nbsp; <i>Memoria: la cinta, limitada a |w| + 2 celdas.</i>";
    }

    @Override
    protected String teoria() {
        return "<h2 style='color:#1F4E79'>Nivel 1 (Tipo 1): Lenguajes sensibles al contexto</h2>"
                + "<p>Se generan con <b>gramáticas sensibles al contexto</b>, con reglas de la forma <b>αAβ → αγβ</b> con γ no vacía: "
                + "la variable A solo se puede reemplazar cuando está rodeada por el contexto α y β. Una consecuencia importante es que "
                + "las reglas <b>nunca acortan</b> la cadena (|lado izquierdo| ≤ |lado derecho|), y por eso basta con una memoria "
                + "del mismo tamaño que la entrada para reconocerlas.</p>"
                + "<p>Gramática para aⁿbⁿcⁿ (forma no contráctil equivalente): <b>S → aSBC | aBC, CB → BC, aB → ab, bB → bb, bC → bc, cC → cc</b>. "
                + "La regla CB → BC es la que \"ordena\" las variables y depende del contexto.</p>"
                + "<h3>Autómata Linealmente Acotado</h3>"
                + "<p>Es una máquina de Turing no determinista con una restricción: solo puede usar las celdas donde está la entrada. "
                + "Formalmente M = (Q, Σ, Γ, δ, q0, &lt;, &gt;, F), con δ: Q × Γ → Q × Γ × {L, R}. Los símbolos &lt; y &gt; son los "
                + "<b>delimitadores de frontera</b>: el cabezal no puede moverse a la izquierda de &lt; ni a la derecha de &gt;, "
                + "y tampoco puede reemplazarlos. (Los ejemplos del simulador son deterministas, que es un caso particular.)</p>"
                + "<h3>¿Qué gana frente al autómata de pila?</h3>"
                + "<p>Puede <b>leer y escribir</b> en cualquier celda y moverse en <b>ambas direcciones</b>. Así puede marcar símbolos "
                + "(X, Y, Z) y volver atrás tantas veces como quiera. En aⁿbⁿcⁿ hace pasadas: marca una a, una b y una c, regresa "
                + "a la izquierda y repite. Con una pila esto no es posible porque al comparar las a con las b se pierde la cuenta para las c.</p>"
                + "<h3>Memoria acotada</h3>"
                + "<p>La cinta mide exactamente |w| + 2 celdas y no crece. Esa es la diferencia con la máquina de Turing del Nivel 0, "
                + "que tiene una cinta infinita.</p>"
                + "<h3>Notación de la bitácora</h3>"
                + "<p>Cada paso muestra la <b>descripción instantánea</b>: la cinta con el estado entre corchetes justo antes del símbolo "
                + "que está leyendo el cabezal. Ejemplo: &lt;X[q1]abbcc&gt;.</p>";
    }

    // ------------------------------------------------------------------
    //  Dibujo
    // ------------------------------------------------------------------
    @Override
    protected void dibujar(Graphics2D g, int w, int h) {
        // Estados como una fila de círculos; el activo se pinta de amarillo
        // Diagrama de estados; cada flecha dice: símbolo leído / símbolo escrito, movimiento
        Map<String, String> etiquetas = new LinkedHashMap<>();
        for (String[] t : reglas) {
            String par = t[0] + " " + t[2];
            String e = t[1] + "/" + t[3] + "," + t[4];
            etiquetas.put(par, etiquetas.containsKey(par) ? etiquetas.get(par) + "\n" + e : e);
        }
        String resaltado = transicionActual >= 0 ? reglas[transicionActual][0] + " " + reglas[transicionActual][2] : null;
        texto(g, "Diagrama de estados  (etiqueta: lee / escribe, movimiento)", 20, 24, 13, true, AZUL);
        dibujarDiagrama(g, estados, etiquetas, "q0", Collections.singleton("qa"), estado, resaltado,
                w / 2.0, h * 0.30, w * 0.36, h * 0.13);

        // Cinta acotada
        int yCinta = (int) (h * 0.65);
        String[] celdas = new String[cinta.size()];
        for (int i = 0; i < cinta.size(); i++) celdas[i] = String.valueOf(cinta.get(i));
        texto(g, "Cinta acotada: |w| + 2 = " + cinta.size() + " celdas. Los delimitadores < y > no se pueden cruzar ni borrar.",
                20, yCinta - 50, 13, true, AZUL);
        dibujarCinta(g, w, yCinta, celdas, cabezal, -1, "cabezal (" + estado + ")");

        // Flecha que muestra hacia dónde se movió el cabezal en el último paso (movimiento bidireccional)
        if (cabezalAnterior != cabezal) {
            double x1 = centroCelda(w, cinta.size(), cabezalAnterior), x2 = centroCelda(w, cinta.size(), cabezal);
            g.setColor(ROJO);
            g.setStroke(new BasicStroke(2.2f));
            g.draw(new QuadCurve2D.Double(x1, yCinta - 4, (x1 + x2) / 2, yCinta - 32, x2, yCinta - 6));
            punta(g, x2, yCinta - 6, Math.atan2(26, x2 - (x1 + x2) / 2));
            texto(g, x2 > x1 ? "R" : "L", (x1 + x2) / 2 - 4, yCinta - 24, 13, true, ROJO);
        }
        // Marcas de frontera debajo de los delimitadores
        g.setColor(TEXTO);
        double xi = centroCelda(w, cinta.size(), 0), xd = centroCelda(w, cinta.size(), cinta.size() - 1);
        texto(g, "frontera izq.", xi - 38, yCinta + 70, 11, false, TEXTO);
        texto(g, "frontera der.", xd - 32, yCinta + 70, 11, false, TEXTO);

        dibujarCaja(g, 20, (int) (h * 0.80), new String[]{
                "Estado actual: " + estado + "     Símbolo bajo el cabezal: " + cinta.get(cabezal),
                "Última acción: " + accion,
                "Descripción instantánea: " + configuracion(),
                "Σ = " + conjunto(sigma) + "     Γ = " + gamma});
    }
}
