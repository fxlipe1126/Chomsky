import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * NIVEL 3 de la jerarquía de Chomsky (Tipo 3): LENGUAJES REGULARES.
 * Máquina: Autómata Finito Determinista  M = (Q, Σ, δ, q0, F).
 *
 * El autómata no tiene memoria auxiliar: lo único que "recuerda" es el estado actual.
 * En cada paso lee un símbolo y cambia de estado según δ(q, a) = p.
 * Al terminar la cadena acepta si el estado en el que quedó pertenece a F.
 */
public class PanelAFD extends PanelSimulador {

    private static final String[] MAQUINAS = {
            "Cadenas sobre {a,b} que terminan en \"ab\"",
            "Números binarios divisibles entre 3",
            "Cadenas sobre {a,b} que contienen \"aba\"",
            "AFD propio (definido por el usuario)"
    };

    // ---- Definición formal M = (Q, Σ, δ, q0, F) ----
    private final List<String> estados = new ArrayList<>();             // Q
    private String sigma = "";                                          // Σ
    private final Map<String, String> delta = new LinkedHashMap<>();    // δ: clave "estado símbolo" -> destino
    private String inicial = "q0";                                      // q0
    private final Set<String> finales = new HashSet<>();                // F
    private String lenguaje = "", expresion = "";
    private String[] ejemplos;

    // ---- Configuración durante la simulación ----
    private String estadoActual;
    private int posicion;              // cuántos símbolos se han leído
    private String ultimaClave = null; // última transición usada

    // ---- AFD que el usuario puede escribir ----
    private String definicionPropia =
            "# AFD de ejemplo: cadenas binarias con un número IMPAR de ceros\n"
            + "inicial: q0\n"
            + "finales: q1\n"
            + "q0 0 q1\n"
            + "q0 1 q0\n"
            + "q1 0 q0\n"
            + "q1 1 q1\n";

    public PanelAFD(Runnable volver) {
        super("Nivel 3 · Lenguajes regulares · Autómata Finito Determinista (AFD)", MAQUINAS, volver);
        JButton propio = new JButton("Definir AFD propio...");
        propio.addActionListener(e -> definirPropio());
        agregarBoton(propio);
        cambiarMaquina(0);
    }

    // ------------------------------------------------------------------
    //  Definición de los autómatas
    // ------------------------------------------------------------------
    @Override
    protected void seleccionarMaquina(int i) {
        estados.clear();
        delta.clear();
        finales.clear();
        inicial = "q0";
        if (i == 0) {
            lenguaje = "L = { w sobre {a,b} | w termina en ab }";
            expresion = "(a|b)*ab";
            sigma = "ab";
            t("q0", 'a', "q1"); t("q0", 'b', "q0");
            t("q1", 'a', "q1"); t("q1", 'b', "q2");
            t("q2", 'a', "q1"); t("q2", 'b', "q0");
            finales.add("q2");
            ejemplos = new String[]{"ab  →  válida", "aab  →  válida", "babab  →  válida", "bbaab  →  válida",
                    "abba  →  inválida", "ba  →  inválida", "λ  →  inválida (cadena vacía)"};
        } else if (i == 1) {
            lenguaje = "L = { w sobre {0,1} | w es un número binario múltiplo de 3 }";
            expresion = "(0|1(01*0)*1)*";
            sigma = "01";
            // Cada estado es el residuo de dividir entre 3 lo que se ha leído: q0=0, q1=1, q2=2
            t("q0", '0', "q0"); t("q0", '1', "q1");
            t("q1", '0', "q2"); t("q1", '1', "q0");
            t("q2", '0', "q1"); t("q2", '1', "q2");
            finales.add("q0");
            ejemplos = new String[]{"11  →  válida (3)", "110  →  válida (6)", "1001  →  válida (9)", "1111  →  válida (15)",
                    "10  →  inválida (2)", "111  →  inválida (7)", "1010  →  inválida (10)"};
        } else if (i == 2) {
            lenguaje = "L = { w sobre {a,b} | w contiene la subcadena aba }";
            expresion = "(a|b)*aba(a|b)*";
            sigma = "ab";
            t("q0", 'a', "q1"); t("q0", 'b', "q0");
            t("q1", 'a', "q1"); t("q1", 'b', "q2");
            t("q2", 'a', "q3"); t("q2", 'b', "q0");
            t("q3", 'a', "q3"); t("q3", 'b', "q3");
            finales.add("q3");
            ejemplos = new String[]{"aba  →  válida", "bbabaa  →  válida", "aabab  →  válida",
                    "abba  →  inválida", "bbbb  →  inválida", "abbab  →  inválida"};
        } else {
            cargarPropio(definicionPropia); // siempre es válida porque ya se verificó
            lenguaje = "AFD definido por el usuario (lenguaje parametrizable)";
            expresion = "—";
            ejemplos = generarEjemplos();
        }
    }

    /** Agrega la transición δ(p, s) = q y registra los estados. */
    private void t(String p, char s, String q) {
        if (!estados.contains(p)) estados.add(p);
        if (!estados.contains(q)) estados.add(q);
        delta.put(p + " " + s, q);
    }

    // ------------------------------------------------------------------
    //  Simulación
    // ------------------------------------------------------------------
    @Override
    protected void iniciar(String w) {
        estadoActual = inicial;
        posicion = 0;
        ultimaClave = null;
        log("Configuración inicial: estado " + inicial + ", cabezal en el primer símbolo.");
        if (w.isEmpty()) finalizar();
    }

    @Override
    protected void paso() {
        char simbolo = cadena.charAt(posicion);
        String clave = estadoActual + " " + simbolo;
        String destino = delta.get(clave);
        if (destino == null) {
            // Transición no definida = estado de error implícito
            log("δ(" + estadoActual + ", " + simbolo + ") no está definida → la cadena se rechaza.");
            terminado = true;
            aceptada = false;
            return;
        }
        transicionActual = new ArrayList<>(delta.keySet()).indexOf(clave);
        ultimaClave = clave;
        log("Lee '" + simbolo + "':  δ(" + estadoActual + ", " + simbolo + ") = " + destino);
        estadoActual = destino;
        posicion++;
        if (posicion == cadena.length()) finalizar();
    }

    private void finalizar() {
        terminado = true;
        aceptada = finales.contains(estadoActual);
        log("Fin de la cadena. Estado final: " + estadoActual
                + (aceptada ? " pertenece a F → ACEPTADA" : " no pertenece a F → RECHAZADA"));
    }

    // ------------------------------------------------------------------
    //  AFD propio (parametrizable)
    // ------------------------------------------------------------------
    private void definirPropio() {
        JTextArea area = new JTextArea(definicionPropia, 14, 34);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JLabel ayuda = new JLabel("<html>Escriba el autómata con este formato:<br>"
                + "&nbsp;• <b>inicial:</b> nombre del estado inicial<br>"
                + "&nbsp;• <b>finales:</b> estados de aceptación separados por coma<br>"
                + "&nbsp;• Una transición por línea: <b>origen símbolo destino</b><br>"
                + "Las líneas que empiezan con # son comentarios. Si falta una transición,<br>"
                + "la cadena se rechaza (estado de error implícito). Máximo 8 estados.</html>");
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.add(ayuda, BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        int r = JOptionPane.showConfirmDialog(this, p, "Definir AFD propio", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            cargarPropio(area.getText());
            definicionPropia = area.getText();
            cambiarMaquina(3);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error en la definición del AFD", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Lee el texto del AFD propio. Lanza una excepción si el formato es incorrecto. */
    private void cargarPropio(String texto) {
        List<String> est = new ArrayList<>();
        Map<String, String> d = new LinkedHashMap<>();
        Set<String> fin = new HashSet<>();
        String ini = null;
        StringBuilder sig = new StringBuilder();
        for (String linea : texto.split("\n")) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) continue;
            String min = linea.toLowerCase();
            if (min.startsWith("inicial:")) { ini = linea.substring(8).trim(); continue; }
            if (min.startsWith("finales:")) {
                for (String f : linea.substring(8).split("[,\\s]+")) if (!f.isEmpty()) fin.add(f);
                continue;
            }
            String[] p = linea.split("\\s+");
            if (p.length != 3 || p[1].length() != 1)
                throw new IllegalArgumentException("Línea inválida: \"" + linea + "\"\nUse el formato: origen símbolo destino");
            String clave = p[0] + " " + p[1];
            if (d.containsKey(clave))
                throw new IllegalArgumentException("El autómata no es determinista: hay dos transiciones para δ(" + p[0] + ", " + p[1] + ")");
            d.put(clave, p[2]);
            if (!est.contains(p[0])) est.add(p[0]);
            if (!est.contains(p[2])) est.add(p[2]);
            if (sig.indexOf(p[1]) < 0) sig.append(p[1]);
        }
        if (ini == null || ini.isEmpty()) throw new IllegalArgumentException("Falta la línea \"inicial: ...\"");
        est.remove(ini);
        est.add(0, ini); // el inicial siempre va primero (queda a la izquierda en el dibujo)
        for (String f : fin) if (!est.contains(f)) est.add(f);
        if (est.size() > 8) throw new IllegalArgumentException("El AFD tiene " + est.size() + " estados; el máximo es 8.");
        if (d.isEmpty()) throw new IllegalArgumentException("No se escribió ninguna transición.");
        // Si todo está bien se copian los datos al autómata actual
        estados.clear(); estados.addAll(est);
        delta.clear(); delta.putAll(d);
        finales.clear(); finales.addAll(fin);
        inicial = ini;
        sigma = sig.toString();
    }

    /** Para el AFD propio busca automáticamente cadenas cortas aceptadas y rechazadas. */
    private String[] generarEjemplos() {
        List<String> si = new ArrayList<>(), no = new ArrayList<>();
        List<String> nivel = new ArrayList<>();
        nivel.add("");
        for (int largo = 0; largo <= 6 && (si.size() < 4 || no.size() < 3); largo++) {
            List<String> siguiente = new ArrayList<>();
            for (String w : nivel) {
                if (acepta(w)) { if (si.size() < 4) si.add(w); } else if (no.size() < 3) no.add(w);
                if (siguiente.size() < 2000) for (char c : sigma.toCharArray()) siguiente.add(w + c);
            }
            nivel = siguiente;
        }
        List<String> res = new ArrayList<>();
        for (String w : si) res.add((w.isEmpty() ? "λ" : w) + "  →  válida (generada)");
        for (String w : no) res.add((w.isEmpty() ? "λ" : w) + "  →  inválida (generada)");
        return res.toArray(new String[0]);
    }

    /** Ejecuta el AFD de una vez (sin animación). Solo se usa para generar ejemplos. */
    private boolean acepta(String w) {
        String q = inicial;
        for (char c : w.toCharArray()) {
            q = delta.get(q + " " + c);
            if (q == null) return false;
        }
        return finales.contains(q);
    }

    // ------------------------------------------------------------------
    //  Textos
    // ------------------------------------------------------------------
    @Override protected String alfabeto() { return sigma; }
    @Override protected String[] ejemplos() { return ejemplos; }

    @Override
    protected String[] transiciones() {
        List<String> l = new ArrayList<>();
        for (Map.Entry<String, String> e : delta.entrySet()) {
            String[] k = e.getKey().split(" ");
            l.add("δ(" + k[0] + ", " + k[1] + ") = " + e.getValue());
        }
        return l.toArray(new String[0]);
    }

    @Override
    protected String descripcion() {
        return "<b>Lenguaje:</b> " + lenguaje + " &nbsp;&nbsp; <b>Expresión regular:</b> " + expresion
                + "<br><b>Q</b> = " + estados + " &nbsp; <b>Σ</b> = " + conjunto(sigma) + " &nbsp; <b>q0</b> = " + inicial
                + " &nbsp; <b>F</b> = " + finales + " &nbsp;&nbsp; <i>Memoria: ninguna, solo el estado actual.</i>";
    }

    /** Gramática regular equivalente, obtenida directamente del AFD: si δ(A,a)=B entonces A → aB, y A → λ si A es final. */
    private String gramaticaEquivalente() {
        StringBuilder sb = new StringBuilder();
        for (String q : estados) {
            List<String> der = new ArrayList<>();
            for (Map.Entry<String, String> e : delta.entrySet()) {
                String[] k = e.getKey().split(" ");
                if (k[0].equals(q)) der.add(k[1] + e.getValue().toUpperCase());
            }
            if (finales.contains(q)) der.add("λ");
            if (!der.isEmpty()) sb.append(q.toUpperCase()).append(" → ").append(String.join(" | ", der)).append("<br>");
        }
        return sb.toString();
    }

    @Override
    protected String teoria() {
        return "<h2 style='color:#1F4E79'>Nivel 3 (Tipo 3): Lenguajes regulares</h2>"
                + "<p>Son los lenguajes más simples de la jerarquía. Se generan con <b>gramáticas regulares</b>, cuyas reglas "
                + "tienen la forma <b>A → aB</b> o <b>A → a</b> (una sola variable, siempre al final). También se describen con "
                + "<b>expresiones regulares</b> y se reconocen con <b>autómatas finitos</b>.</p>"
                + "<h3>Definición formal del AFD</h3>"
                + "<p>M = (Q, Σ, δ, q0, F), donde Q es el conjunto finito de estados, Σ el alfabeto de entrada, "
                + "δ: Q × Σ → Q la función de transición, q0 el estado inicial y F (subconjunto de Q) los estados de aceptación.</p>"
                + "<p>En este autómata: Q = " + estados + ", Σ = " + conjunto(sigma) + ", q0 = " + inicial + ", F = " + finales + ".</p>"
                + "<h3>¿Cómo funciona?</h3>"
                + "<p>El cabezal lee la cadena de izquierda a derecha, un símbolo por paso, y nunca retrocede. En cada paso se aplica "
                + "δ(q, a) = p. Cuando se acaba la cadena, se acepta si el estado actual está en F. Es <b>determinista</b> porque para "
                + "cada pareja (estado, símbolo) hay como máximo una transición. Si una transición no existe, se entiende que la "
                + "máquina pasa a un estado de error y la cadena se rechaza.</p>"
                + "<h3>Memoria</h3>"
                + "<p>El AFD no tiene memoria adicional: su única memoria es el estado en el que está. Por eso solo puede recordar "
                + "una cantidad finita de información. Por ejemplo, en el autómata de múltiplos de 3 cada estado guarda el residuo "
                + "de la división (0, 1 o 2), no el número completo.</p>"
                + "<h3>Gramática regular equivalente a este AFD</h3><p style='font-family:monospace'>" + gramaticaEquivalente() + "</p>"
                + "<h3>Límite del nivel</h3>"
                + "<p>Un AFD no puede contar sin límite. El lenguaje { a<sup>n</sup>b<sup>n</sup> } no es regular (se demuestra "
                + "con el lema de bombeo): para comparar cuántas a y cuántas b hay se necesita memoria, y eso lo da la pila del Nivel 2.</p>";
    }

    // ------------------------------------------------------------------
    //  Dibujo
    // ------------------------------------------------------------------
    @Override
    protected void dibujar(Graphics2D g, int w, int h) {
        int n = estados.size();
        // Se agrupan los símbolos que van del mismo origen al mismo destino: "a,b"
        Map<String, String> etiquetas = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : delta.entrySet()) {
            String[] k = e.getKey().split(" ");
            String par = k[0] + " " + e.getValue();
            etiquetas.put(par, etiquetas.containsKey(par) ? etiquetas.get(par) + "," + k[1] : k[1]);
        }
        String resaltado = ultimaClave == null ? null : ultimaClave.split(" ")[0] + " " + delta.get(ultimaClave);
        dibujarDiagrama(g, estados, etiquetas, inicial, finales, estadoActual, resaltado,
                w / 2.0, h * 0.36, Math.min(w * 0.32, 120 + n * 50), Math.min(h * 0.22, 70 + n * 30));

        // Cinta de entrada (solo lectura, el cabezal avanza a la derecha)
        int yCinta = (int) (h * 0.74);
        texto(g, "Cinta de entrada (solo lectura, el cabezal siempre avanza a la derecha):", 20, yCinta - 14, 13, true, AZUL);
        String[] celdas = new String[Math.max(1, cadena.length())];
        if (cadena.isEmpty()) celdas[0] = "λ";
        for (int i = 0; i < cadena.length(); i++) celdas[i] = String.valueOf(cadena.charAt(i));
        dibujarCinta(g, w, yCinta, celdas, cadena.isEmpty() ? -1 : posicion, posicion, terminado ? "fin" : "cabezal");

        String ultima = ultimaClave == null ? "—"
                : "δ(" + ultimaClave.replace(" ", ", ") + ") = " + delta.get(ultimaClave);
        dibujarCaja(g, 20, 20, new String[]{
                "Estado actual: " + estadoActual,
                "Leído: " + (posicion == 0 ? "λ" : cadena.substring(0, posicion)),
                "Por leer: " + (posicion >= cadena.length() ? "λ" : cadena.substring(posicion)),
                "Última transición: " + ultima});
    }
}
