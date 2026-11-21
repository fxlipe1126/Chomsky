import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * NIVEL 0 de la jerarquía de Chomsky (Tipo 0): LENGUAJES RECURSIVAMENTE ENUMERABLES.
 * Máquina: Máquina de Turing  M = (Q, Σ, Γ, δ, q0, B, F).
 *
 * Tiene una cinta infinita en ambas direcciones (aquí crece cada vez que el cabezal
 * llega a un extremo), un cabezal que lee y escribe, y un control de estados.
 * Se usa de dos formas:
 *  - Para ACEPTAR cadenas (palíndromos): si llega a qa la cadena es aceptada.
 *  - Para CALCULAR una función (suma en unario, incremento binario): cuando llega a qf
 *    se detiene y el resultado es lo que quedó escrito en la cinta.
 */
public class PanelMT extends PanelSimulador {

    private static final String[] MAQUINAS = {
            "Función: suma en unario  (111+11 = 11111)",
            "Función: incremento binario  (n + 1)",
            "Aceptación: palíndromos sobre {a, b}"
    };
    private static final char BLANCO = 'B';

    // Cada regla es {estado, símbolo leído, nuevo estado, símbolo escrito, movimiento (L, R o S)}
    private String[][] reglas;
    private final List<String> estados = new ArrayList<>();
    private String sigma, gamma, lenguaje, idea, estadoFinal;
    private boolean esFuncion;
    private String[] ejemplos;

    // ---- Configuración: la cinta es una lista que crece por ambos lados ----
    private final ArrayList<Character> cinta = new ArrayList<>();
    private int cabezal;
    private int desplazamiento; // cuántas celdas se han agregado a la izquierda (para numerar las posiciones)
    private String estado;
    private String accion = "";

    public PanelMT(Runnable volver) {
        super("Nivel 0 · Lenguajes recursivamente enumerables · Máquina de Turing (MT)", MAQUINAS, volver);
        etiquetaBucleAlLado = true;
        cambiarMaquina(0);
    }

    // ------------------------------------------------------------------
    //  Definición de las máquinas
    // ------------------------------------------------------------------
    @Override
    protected void seleccionarMaquina(int i) {
        if (i == 0) {
            esFuncion = true;
            estadoFinal = "qf";
            lenguaje = "f(n, m) = n + m  con los números escritos en unario (n = 111 representa 3)";
            idea = "Cambia el + por 1, va hasta el final y borra el último 1.";
            sigma = "1+";
            gamma = "{1, +, B}";
            reglas = new String[][]{
                    {"q0", "1", "q0", "1", "R"},
                    {"q0", "+", "q1", "1", "R"},   // el + se vuelve 1
                    {"q1", "1", "q1", "1", "R"},
                    {"q1", "B", "q2", "B", "L"},   // llegó al final
                    {"q2", "1", "q3", "B", "L"},   // borra un 1 sobrante
                    {"q3", "1", "q3", "1", "L"},
                    {"q3", "B", "qf", "B", "R"}};  // vuelve al inicio y se detiene
            ejemplos = new String[]{"111+11  →  3 + 2 = 5", "1+1  →  1 + 1 = 2", "1111+111  →  4 + 3 = 7",
                    "+11  →  0 + 2 = 2", "11+  →  2 + 0 = 2", "111  →  inválida (no tiene +)"};
        } else if (i == 1) {
            esFuncion = true;
            estadoFinal = "qf";
            lenguaje = "f(n) = n + 1  con n escrito en binario";
            idea = "Va al último dígito y suma 1 con acarreo: los 1 se vuelven 0 hasta encontrar un 0 o un blanco.";
            sigma = "01";
            gamma = "{0, 1, B}";
            reglas = new String[][]{
                    {"q0", "0", "q0", "0", "R"},
                    {"q0", "1", "q0", "1", "R"},
                    {"q0", "B", "q1", "B", "L"},   // llegó al final
                    {"q1", "1", "q1", "0", "L"},   // 1 + 1 = 0 y se lleva 1
                    {"q1", "0", "q2", "1", "L"},   // 0 + 1 = 1, sin acarreo
                    {"q1", "B", "q2", "1", "L"},   // acarreo al inicio: la cinta crece a la izquierda
                    {"q2", "0", "q2", "0", "L"},
                    {"q2", "1", "q2", "1", "L"},
                    {"q2", "B", "qf", "B", "R"}};
            ejemplos = new String[]{"1011  →  11 + 1 = 12 (1100)", "111  →  7 + 1 = 8 (1000)", "1000  →  8 + 1 = 9 (1001)",
                    "0  →  0 + 1 = 1", "1  →  1 + 1 = 2 (10)", "11111  →  31 + 1 = 32"};
        } else {
            esFuncion = false;
            estadoFinal = "qa";
            lenguaje = "L = { w sobre {a, b} | w es palíndromo (se lee igual al derecho y al revés) }";
            idea = "Borra el primer símbolo, lo recuerda en el estado, va al final y compara con el último. Repite.";
            sigma = "ab";
            gamma = "{a, b, B}";
            reglas = new String[][]{
                    {"q0", "a", "q1", "B", "R"},   // recuerda a
                    {"q0", "b", "q2", "B", "R"},   // recuerda b
                    {"q0", "B", "qa", "B", "S"},   // no queda nada: acepta
                    {"q1", "a", "q1", "a", "R"},
                    {"q1", "b", "q1", "b", "R"},
                    {"q1", "B", "q3", "B", "L"},
                    {"q2", "a", "q2", "a", "R"},
                    {"q2", "b", "q2", "b", "R"},
                    {"q2", "B", "q4", "B", "L"},
                    {"q3", "a", "q5", "B", "L"},   // el último también es a
                    {"q3", "B", "qa", "B", "S"},
                    {"q4", "b", "q5", "B", "L"},   // el último también es b
                    {"q4", "B", "qa", "B", "S"},
                    {"q5", "a", "q5", "a", "L"},
                    {"q5", "b", "q5", "b", "L"},
                    {"q5", "B", "q0", "B", "R"}};  // regresa al inicio
            ejemplos = new String[]{"abba  →  válida", "aba  →  válida", "babbab  →  válida", "λ  →  válida (vacía)",
                    "ab  →  inválida", "abab  →  inválida", "aab  →  inválida"};
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
        for (char c : w.toCharArray()) cinta.add(c);
        if (cinta.isEmpty()) cinta.add(BLANCO);
        cabezal = 0;
        desplazamiento = 0;
        estado = "q0";
        accion = "—";
        log("Configuración inicial: " + configuracion() + "  (B = blanco)");
    }

    @Override
    protected void paso() {
        char leido = cinta.get(cabezal);
        int r = buscar(estado, leido);
        if (r < 0) {
            terminado = true;
            aceptada = false;
            log("δ(" + estado + ", " + leido + ") no está definida: la máquina se detiene en " + estado + ", que no es estado final.");
            mensajeFinal = esFuncion ? "LA MT SE DETUVO SIN TERMINAR: la entrada no tiene el formato esperado" : null;
            return;
        }
        String[] t = reglas[r];
        transicionActual = r;
        char escrito = t[3].charAt(0);
        cinta.set(cabezal, escrito);
        estado = t[2];
        if (t[4].equals("R")) cabezal++;
        else if (t[4].equals("L")) cabezal--;
        // Cinta infinita: si el cabezal sale por un extremo se agrega una celda en blanco
        String extension = "";
        if (cabezal < 0) {
            cinta.add(0, BLANCO);
            cabezal = 0;
            desplazamiento++;
            extension = "   (la cinta creció a la izquierda)";
        } else if (cabezal >= cinta.size()) {
            cinta.add(BLANCO);
            extension = "   (la cinta creció a la derecha)";
        }
        accion = "lee " + leido + ", escribe " + escrito + ", "
                + (t[4].equals("R") ? "mueve a la derecha" : t[4].equals("L") ? "mueve a la izquierda" : "no se mueve");
        log("δ(" + t[0] + ", " + t[1] + ") = (" + t[2] + ", " + t[3] + ", " + t[4] + ")   " + configuracion() + extension);

        if (estado.equals(estadoFinal)) {
            terminado = true;
            aceptada = true;
            if (esFuncion) {
                String res = resultado();
                mensajeFinal = "LA MT SE DETUVO EN qf · Resultado en la cinta: " + (res.isEmpty() ? "λ" : res) + "  " + interpretar(res);
                log("Se detuvo en qf. Resultado: " + (res.isEmpty() ? "λ" : res) + " " + interpretar(res));
            } else {
                log("Llegó al estado de aceptación qa → ACEPTADA");
            }
        }
    }

    private int buscar(String q, char s) {
        for (int i = 0; i < reglas.length; i++)
            if (reglas[i][0].equals(q) && reglas[i][1].charAt(0) == s) return i;
        return -1;
    }

    /** Contenido de la cinta sin los blancos de los extremos. */
    private String resultado() {
        StringBuilder sb = new StringBuilder();
        for (char c : cinta) sb.append(c);
        return sb.toString().replaceAll("^B+|B+$", "");
    }

    /** Traduce el resultado a decimal para que sea fácil comprobarlo. */
    private String interpretar(String res) {
        if (maquinaUnaria()) return "(= " + res.length() + " en decimal)";
        try {
            return "(= " + (res.isEmpty() ? 0 : Long.parseLong(res, 2)) + " en decimal)";
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private boolean maquinaUnaria() {
        return sigma.equals("1+");
    }

    /** Descripción instantánea: estado entre corchetes antes del símbolo leído. */
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
        return "<b>" + (esFuncion ? "Función" : "Lenguaje") + ":</b> " + lenguaje + " &nbsp;&nbsp; <b>Idea:</b> " + idea
                + "<br><b>Σ</b> = " + conjunto(sigma) + " &nbsp; <b>Γ</b> = " + gamma + " &nbsp; <b>B</b> = blanco &nbsp; <b>"
                + (esFuncion ? "Estado de parada:</b> qf" : "Estado de aceptación:</b> qa")
                + " &nbsp;&nbsp; <i>Memoria: cinta infinita (crece cuando se necesita).</i>";
    }

    @Override
    protected String teoria() {
        return "<h2 style='color:#1F4E79'>Nivel 0 (Tipo 0): Lenguajes recursivamente enumerables</h2>"
                + "<p>Se generan con <b>gramáticas sin restricciones</b>: reglas α → β donde α tiene al menos una variable y no hay "
                + "ningún límite sobre β (incluso puede ser más corta que α). Es el nivel más general de la jerarquía y contiene a los otros tres.</p>"
                + "<h3>Definición formal de la Máquina de Turing</h3>"
                + "<p>M = (Q, Σ, Γ, δ, q0, B, F): Q estados, Σ alfabeto de entrada, Γ alfabeto de la cinta (incluye a Σ y al blanco B), "
                + "δ: Q × Γ → Q × Γ × {L, R} la función de transición (aquí también se usa S = no moverse), q0 estado inicial, "
                + "B el símbolo blanco y F los estados finales.</p>"
                + "<p>La transición δ(q, a) = (p, b, L) se lee: en el estado q, si el cabezal ve a, escribe b, pasa al estado p "
                + "y se mueve una celda a la izquierda.</p>"
                + "<h3>Cinta infinita</h3>"
                + "<p>A diferencia del ALA, la cinta no tiene fronteras: si el cabezal llega a un extremo aparece una celda nueva en blanco. "
                + "En el incremento binario se ve claramente: 111 + 1 = 1000 necesita una celda más a la izquierda.</p>"
                + "<h3>Aceptar y calcular</h3>"
                + "<p>Una MT puede <b>reconocer un lenguaje</b> (palíndromos: acepta si llega a qa) o <b>calcular una función</b> "
                + "(suma unaria e incremento binario: cuando llega a qf el resultado es lo que quedó escrito en la cinta). "
                + "Según la tesis de Church-Turing, todo lo que se puede calcular con un algoritmo se puede calcular con una MT.</p>"
                + "<h3>Detención</h3>"
                + "<p>Una MT puede no detenerse nunca con algunas entradas. Por eso los lenguajes de este nivel se llaman "
                + "<b>recursivamente enumerables</b>: si la cadena pertenece, la máquina lo confirma en algún momento, pero si no "
                + "pertenece puede quedarse ejecutando para siempre. Decidir en general si una MT se detiene (problema de la parada) "
                + "es imposible. Por seguridad, el simulador corta la ejecución a los 5000 pasos.</p>";
    }

    // ------------------------------------------------------------------
    //  Dibujo
    // ------------------------------------------------------------------
    @Override
    protected void dibujar(Graphics2D g, int w, int h) {
        // Diagrama de estados; cada flecha dice: símbolo leído / símbolo escrito, movimiento
        Map<String, String> etiquetas = new LinkedHashMap<>();
        for (String[] t : reglas) {
            String par = t[0] + " " + t[2];
            String e = t[1] + "/" + t[3] + "," + t[4];
            etiquetas.put(par, etiquetas.containsKey(par) ? etiquetas.get(par) + "\n" + e : e);
        }
        String resaltado = transicionActual >= 0 ? reglas[transicionActual][0] + " " + reglas[transicionActual][2] : null;
        texto(g, "Diagrama de estados  (etiqueta: lee / escribe, movimiento)", 20, 24, 13, true, AZUL);
        dibujarDiagrama(g, estados, etiquetas, "q0", Collections.singleton(estadoFinal), estado, resaltado,
                w / 2.0, h * 0.30, w * 0.36, h * 0.13);

        // Ventana visible de la cinta infinita, centrada en el cabezal
        int visibles = Math.max(9, Math.min(19, (w - 120) / 42));
        if (visibles % 2 == 0) visibles--;
        int inicio = cabezal - visibles / 2;
        String[] celdas = new String[visibles];
        for (int i = 0; i < visibles; i++) {
            int k = inicio + i;
            celdas[i] = (k >= 0 && k < cinta.size()) ? String.valueOf(cinta.get(k)) : String.valueOf(BLANCO);
        }
        int yCinta = (int) (h * 0.65);
        texto(g, "Cinta infinita en ambas direcciones (se muestra la parte alrededor del cabezal; B = blanco):",
                20, yCinta - 30, 13, true, AZUL);
        dibujarCinta(g, w, yCinta, celdas, visibles / 2, -1, "cabezal L/E (" + estado + ")");
        // Puntos suspensivos para indicar que la cinta sigue
        double xi = centroCelda(w, visibles, 0) - 52, xd = centroCelda(w, visibles, visibles - 1) + 28;
        texto(g, "···", xi, yCinta + 26, 22, true, TEXTO);
        texto(g, "···", xd, yCinta + 26, 22, true, TEXTO);
        // Número de la posición de algunas celdas
        for (int i = 0; i < visibles; i++) {
            int k = inicio + i - desplazamiento;
            if (k % 5 == 0) texto(g, String.valueOf(k), centroCelda(w, visibles, i) - 4, yCinta - 6, 10, false, Color.GRAY);
        }

        dibujarCaja(g, 20, (int) (h * 0.80), new String[]{
                "Estado activo: " + estado + "     Símbolo bajo el cabezal: " + cinta.get(cabezal),
                "Última acción: " + accion,
                "Contenido de la cinta: " + (resultado().isEmpty() ? "(solo blancos)" : resultado()),
                "Σ (entrada) = " + conjunto(sigma) + "     Γ (cinta) = " + gamma});
    }
}
