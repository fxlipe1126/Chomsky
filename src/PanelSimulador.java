import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.*;

/**
 * Clase base (abstracta) de los cuatro módulos del simulador.
 *
 * Aquí está todo lo que los cuatro niveles tienen en común:
 *  - Los controles (selección de máquina, ejemplos, cadena, botones y velocidad).
 *  - El temporizador para la ejecución automática.
 *  - La lista con la función de transición (δ) y la bitácora de la ejecución.
 *  - Métodos de dibujo reutilizables (estados, flechas, cinta, cajas de texto).
 *
 * Cada nivel (AFD, AP, ALA y MT) hereda de esta clase y solo implementa
 * su propia lógica: cómo se inicia, qué hace en un paso y cómo se dibuja.
 */
public abstract class PanelSimulador extends JPanel {

    // ---------------- Colores compartidos ----------------
    static final Color AZUL = new Color(31, 78, 121);
    static final Color AZUL_CLARO = new Color(214, 228, 240);
    static final Color AMARILLO = new Color(255, 192, 0);
    static final Color VERDE = new Color(39, 174, 96);
    static final Color VERDE_CLARO = new Color(200, 240, 210);
    static final Color ROJO = new Color(192, 57, 43);
    static final Color GRIS = new Color(190, 190, 190);
    static final Color GRIS_CLARO = new Color(230, 230, 230);
    static final Color TEXTO = new Color(45, 45, 45);
    static final int R = 24; // radio de los círculos que representan estados
    /** Si es true, las etiquetas de los bucles se escriben al lado del bucle (útil cuando son largas). */
    protected boolean etiquetaBucleAlLado = false;

    // -------- Estado general de la simulación (lo modifican las subclases) --------
    protected String cadena = "";          // cadena de entrada actual
    protected boolean terminado = false;   // la máquina ya se detuvo
    protected boolean aceptada = false;    // resultado final
    protected String mensajeFinal = null;  // texto especial para el resultado (opcional)
    protected int transicionActual = -1;   // posición de la última transición usada en la lista δ

    // ---------------- Componentes de la interfaz ----------------
    private final JComboBox<String> comboMaquina;
    private final JComboBox<String> comboEjemplos = new JComboBox<>();
    private final JTextField campoCadena = new JTextField(14);
    private final JButton btnEjecutar = new JButton("Ejecutar");
    private final JSlider velocidad = new JSlider(1, 10, 5);
    private final Timer temporizador = new Timer(600, e -> darPaso()); // ejecución automática
    private final JLabel lblDescripcion = new JLabel();
    private final JLabel lblResultado = new JLabel(" ", SwingConstants.CENTER);
    private final JTextArea bitacora = new JTextArea();
    private final JList<String> listaTransiciones = new JList<>();
    private final JPanel filaSeleccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    private final JPanel lienzo;
    private Timer temporizadorAnimacion;
    private long inicioAnimacion = 0;
    private int pasos = 0;
    private boolean cambiando = false;

    protected PanelSimulador(String titulo, String[] maquinas, Runnable volver) {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 10, 8, 10));
        setBackground(Color.WHITE);

        // ----- Fila del título -----
        JButton btnVolver = new JButton("< Panel principal");
        btnVolver.addActionListener(e -> { detener(); volver.run(); });
        JButton btnTeoria = new JButton("Teoría del nivel");
        btnTeoria.addActionListener(e -> mostrarTeoria());
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitulo.setForeground(AZUL);
        lblTitulo.setBorder(new EmptyBorder(0, 15, 0, 0));
        JPanel filaTitulo = new JPanel(new BorderLayout());
        filaTitulo.setOpaque(false);
        filaTitulo.add(btnVolver, BorderLayout.WEST);
        filaTitulo.add(lblTitulo, BorderLayout.CENTER);
        filaTitulo.add(btnTeoria, BorderLayout.EAST);

        // ----- Fila de selección: máquina y ejemplos -----
        comboMaquina = new JComboBox<>(maquinas);
        comboMaquina.addActionListener(e -> {
            if (!cambiando) cambiarMaquina(comboMaquina.getSelectedIndex());
        });
        comboEjemplos.addActionListener(e -> {
            if (cambiando || comboEjemplos.getSelectedItem() == null) return;
            campoCadena.setText(cadenaDeEjemplo((String) comboEjemplos.getSelectedItem()));
            reiniciar();
        });
        filaSeleccion.setOpaque(false);
        filaSeleccion.add(etiqueta("Máquina / lenguaje:"));
        filaSeleccion.add(comboMaquina);
        filaSeleccion.add(etiqueta("   Cadenas de prueba:"));
        filaSeleccion.add(comboEjemplos);

        // ----- Fila de control de la simulación -----
        campoCadena.setFont(new Font("Monospaced", Font.BOLD, 16));
        campoCadena.addActionListener(e -> reiniciar()); // al presionar Enter
        JButton btnReiniciar = new JButton("Cargar / Reiniciar");
        btnReiniciar.addActionListener(e -> reiniciar());
        JButton btnPaso = new JButton("Paso a paso  >");
        btnPaso.addActionListener(e -> { detener(); darPaso(); });
        btnEjecutar.addActionListener(e -> {
            if (temporizador.isRunning()) detener(); else ejecutar();
        });
        velocidad.setPreferredSize(new Dimension(110, 24));
        velocidad.addChangeListener(e -> temporizador.setDelay(retardo()));
        JPanel filaControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filaControles.setOpaque(false);
        filaControles.add(etiqueta("Cadena de entrada:"));
        filaControles.add(campoCadena);
        filaControles.add(btnReiniciar);
        filaControles.add(btnPaso);
        filaControles.add(btnEjecutar);
        filaControles.add(etiqueta("   Lento"));
        filaControles.add(velocidad);
        filaControles.add(etiqueta("Rápido"));

        lblDescripcion.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel filas = new JPanel(new GridLayout(2, 1));
        filas.setOpaque(false);
        filas.add(filaSeleccion);
        filas.add(filaControles);
        JPanel norte = new JPanel(new BorderLayout(4, 4));
        norte.setOpaque(false);
        norte.add(filaTitulo, BorderLayout.NORTH);
        norte.add(lblDescripcion, BorderLayout.CENTER);
        norte.add(filas, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        // ----- Lienzo central donde cada nivel dibuja su máquina -----
        lienzo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                dibujar(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        lienzo.setBackground(Color.WHITE);
        lienzo.setBorder(BorderFactory.createLineBorder(GRIS));
        add(lienzo, BorderLayout.CENTER);

        // ----- Panel derecho: función de transición y bitácora -----
        listaTransiciones.setFont(new Font("Monospaced", Font.PLAIN, 13));
        listaTransiciones.setSelectionBackground(AMARILLO);
        listaTransiciones.setSelectionForeground(Color.BLACK);
        listaTransiciones.setFocusable(false);
        JScrollPane spT = new JScrollPane(listaTransiciones);
        spT.setBorder(new TitledBorder("Función de transición δ"));
        bitacora.setEditable(false);
        bitacora.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bitacora.setLineWrap(true);
        bitacora.setWrapStyleWord(true);
        JScrollPane spB = new JScrollPane(bitacora);
        spB.setBorder(new TitledBorder("Bitácora de la ejecución"));
        JPanel este = new JPanel(new GridLayout(2, 1, 0, 6));
        este.setOpaque(false);
        este.setPreferredSize(new Dimension(340, 100));
        este.add(spT);
        este.add(spB);
        add(este, BorderLayout.EAST);

        // ----- Resultado -----
        lblResultado.setOpaque(true);
        lblResultado.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblResultado.setBorder(new EmptyBorder(6, 6, 6, 6));
        add(lblResultado, BorderLayout.SOUTH);

        temporizador.setDelay(retardo());
    }

    // =====================================================================
    //  Métodos que cada nivel debe implementar
    // =====================================================================
    /** Carga la definición formal de la máquina número i. */
    protected abstract void seleccionarMaquina(int i);
    /** Texto corto (HTML) con el lenguaje que reconoce la máquina. */
    protected abstract String descripcion();
    /** Explicación teórica del nivel (HTML) para la ventana "Teoría". */
    protected abstract String teoria();
    /** Símbolos permitidos en la cadena de entrada (alfabeto Σ). */
    protected abstract String alfabeto();
    /** Cadenas de prueba en formato "cadena  →  comentario". */
    protected abstract String[] ejemplos();
    /** La función de transición δ escrita de forma legible. */
    protected abstract String[] transiciones();
    /** Deja la máquina en su configuración inicial con la cadena w. */
    protected abstract void iniciar(String w);
    /** Ejecuta UN movimiento de la máquina. */
    protected abstract void paso();
    /** Dibuja la máquina en el lienzo. */
    protected abstract void dibujar(Graphics2D g, int w, int h);

    // =====================================================================
    //  Control de la simulación
    // =====================================================================
    /** Cambia la máquina activa y recarga ejemplos, transiciones y descripción. */
    protected void cambiarMaquina(int i) {
        detener();
        seleccionarMaquina(i);
        cambiando = true;
        comboMaquina.setSelectedIndex(i);
        comboEjemplos.removeAllItems();
        for (String e : ejemplos()) comboEjemplos.addItem(e);
        cambiando = false;
        listaTransiciones.setListData(transiciones());
        lblDescripcion.setText("<html><div style='padding:3px'>" + descripcion() + "</div></html>");
        campoCadena.setText(cadenaDeEjemplo(ejemplos()[0]));
        reiniciar();
    }

    /** Lee la cadena del campo de texto y deja la máquina lista para empezar. */
    protected void reiniciar() {
        detener();
        cadena = campoCadena.getText().trim();
        terminado = false;
        aceptada = false;
        mensajeFinal = null;
        transicionActual = -1;
        pasos = 0;
        inicioAnimacion = 0;
        bitacora.setText("");
        log("Cadena de entrada: " + (cadena.isEmpty() ? "λ (cadena vacía)" : cadena));
        iniciar(cadena);
        // Validación: todos los símbolos deben pertenecer al alfabeto de entrada
        for (char c : cadena.toCharArray()) {
            if (alfabeto().indexOf(c) < 0) {
                terminado = true;
                aceptada = false;
                mensajeFinal = "CADENA RECHAZADA: '" + c + "' no pertenece al alfabeto Σ";
                log("ERROR: el símbolo '" + c + "' no pertenece a Σ = " + conjunto(alfabeto()));
                break;
            }
        }
        actualizar();
    }

    /** Un paso de la simulación (lo usan el botón "Paso a paso" y el temporizador). */
    private void darPaso() {
        if (!campoCadena.getText().trim().equals(cadena)) reiniciar(); // la cadena cambió
        if (terminado) {
            detener();
            actualizar();
            return;
        }
        pasos++;
        paso();
        if (!terminado && pasos >= 5000) { // protección contra ciclos infinitos
            terminado = true;
            aceptada = false;
            mensajeFinal = "Se detuvo la simulación: más de 5000 pasos";
            log("La máquina superó 5000 pasos; puede que nunca se detenga.");
        }
        animar();
        actualizar();
        if (terminado) detener();
    }

    private void ejecutar() {
        if (terminado || !campoCadena.getText().trim().equals(cadena)) reiniciar();
        temporizador.start();
        btnEjecutar.setText("Pausar");
    }

    private void detener() {
        if (temporizador != null) temporizador.stop();
        btnEjecutar.setText("Ejecutar");
    }

    private int retardo() {
        return 1100 - velocidad.getValue() * 100; // de 1000 ms (lento) a 100 ms (rápido)
    }

    /** Refresca la lista δ, el resultado y el dibujo. */
    private void actualizar() {
        if (transicionActual >= 0) {
            listaTransiciones.setSelectedIndex(transicionActual);
            listaTransiciones.ensureIndexIsVisible(transicionActual);
        } else {
            listaTransiciones.clearSelection();
        }
        if (!terminado) {
            lblResultado.setText("Simulación en curso  ·  pasos realizados: " + pasos);
            lblResultado.setBackground(AZUL_CLARO);
            lblResultado.setForeground(AZUL);
        } else {
            String t = mensajeFinal != null ? mensajeFinal : (aceptada ? "CADENA ACEPTADA" : "CADENA RECHAZADA");
            lblResultado.setText(t + "   (" + pasos + " pasos)");
            lblResultado.setBackground(aceptada ? VERDE : ROJO);
            lblResultado.setForeground(Color.WHITE);
        }
        lienzo.repaint();
    }

    /** Inicia una pequeña animación (la usa la pila del nivel 2, por ejemplo). */
    private void animar() {
        inicioAnimacion = System.currentTimeMillis();
        if (temporizadorAnimacion == null) {
            temporizadorAnimacion = new Timer(25, e -> {
                lienzo.repaint();
                if (progreso() >= 1) ((Timer) e.getSource()).stop();
            });
        }
        temporizadorAnimacion.restart();
    }

    /** Avance de la animación entre 0 (recién empieza) y 1 (terminada). */
    protected double progreso() {
        return Math.min(1.0, (System.currentTimeMillis() - inicioAnimacion) / 400.0);
    }

    /** Escribe una línea en la bitácora. */
    protected void log(String texto) {
        bitacora.append(texto + "\n");
        bitacora.setCaretPosition(bitacora.getDocument().getLength());
    }

    /** Permite a un nivel agregar un botón propio a la barra de selección. */
    protected void agregarBoton(JButton b) {
        filaSeleccion.add(b);
    }

    private void mostrarTeoria() {
        JEditorPane panel = new JEditorPane("text/html",
                "<html><body style='font-family:sans-serif;font-size:12pt;padding:6px'>" + teoria() + "</body></html>");
        panel.setEditable(false);
        panel.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(panel);
        sp.setPreferredSize(new Dimension(720, 520));
        JOptionPane.showMessageDialog(this, sp, "Teoría del nivel", JOptionPane.PLAIN_MESSAGE);
    }

    // =====================================================================
    //  Utilidades
    // =====================================================================
    /** De "aabb  →  válida" obtiene "aabb" (la letra λ representa la cadena vacía). */
    private static String cadenaDeEjemplo(String ejemplo) {
        String s = ejemplo.split("  ")[0].trim();
        return s.equals("λ") ? "" : s;
    }

    /** Convierte "abc" en "{a, b, c}". */
    protected static String conjunto(String simbolos) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < simbolos.length(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(simbolos.charAt(i));
        }
        return sb.append("}").toString();
    }

    private static JLabel etiqueta(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        return l;
    }

    // =====================================================================
    //  Métodos de dibujo reutilizables
    // =====================================================================
    /** Dibuja un estado: doble círculo si es de aceptación, flecha si es inicial, amarillo si está activo. */
    protected void dibujarEstado(Graphics2D g, double x, double y, String nombre,
                                 boolean inicial, boolean esFinal, boolean activo) {
        Ellipse2D c = new Ellipse2D.Double(x - R, y - R, 2 * R, 2 * R);
        g.setColor(activo ? AMARILLO : Color.WHITE);
        g.fill(c);
        g.setColor(AZUL);
        g.setStroke(new BasicStroke(activo ? 3f : 2f));
        g.draw(c);
        if (esFinal) g.draw(new Ellipse2D.Double(x - R + 4, y - R + 4, 2 * R - 8, 2 * R - 8));
        if (inicial) {
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(x - R - 30, y, x - R - 2, y));
            punta(g, x - R - 2, y, 0);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(TEXTO);
        g.drawString(nombre, (float) (x - fm.stringWidth(nombre) / 2.0), (float) (y + fm.getAscent() / 2.0 - 2));
    }

    /** Punta de flecha en (x, y) apuntando con el ángulo dado. */
    protected void punta(Graphics2D g, double x, double y, double ang) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(x - 12 * Math.cos(ang - 0.38), y - 12 * Math.sin(ang - 0.38));
        p.lineTo(x - 12 * Math.cos(ang + 0.38), y - 12 * Math.sin(ang + 0.38));
        p.closePath();
        g.fill(p);
    }

    /**
     * Dibuja una transición entre dos estados. Si origen y destino son iguales dibuja un bucle
     * hacia afuera del punto "centro". La etiqueta puede tener varias líneas separadas por \n.
     */
    protected void dibujarTransicion(Graphics2D g, Point2D a, Point2D b, String etiqueta,
                                     boolean resaltada, double curva, Point2D centro) {
        Color color = resaltada ? ROJO : new Color(100, 100, 100);
        g.setColor(color);
        g.setStroke(new BasicStroke(resaltada ? 3f : 1.6f));
        double lx, ly, dirx, diry; // posición y dirección hacia donde se ubica la etiqueta
        if (a.equals(b)) {
            double dx = a.getX() - centro.getX(), dy = a.getY() - centro.getY();
            double len = Math.hypot(dx, dy);
            if (len < 1) { dx = 0; dy = -1; len = 1; }
            dx /= len; dy /= len;
            double ang = Math.atan2(dy, dx);
            double sx = a.getX() + R * Math.cos(ang - 0.55), sy = a.getY() + R * Math.sin(ang - 0.55);
            double ex = a.getX() + R * Math.cos(ang + 0.55), ey = a.getY() + R * Math.sin(ang + 0.55);
            double c1x = a.getX() + 3.0 * R * Math.cos(ang - 0.75), c1y = a.getY() + 3.0 * R * Math.sin(ang - 0.75);
            double c2x = a.getX() + 3.0 * R * Math.cos(ang + 0.75), c2y = a.getY() + 3.0 * R * Math.sin(ang + 0.75);
            g.draw(new CubicCurve2D.Double(sx, sy, c1x, c1y, c2x, c2y, ex, ey));
            punta(g, ex, ey, Math.atan2(ey - c2y, ex - c2x));
            lx = a.getX() + 2.5 * R * dx;
            ly = a.getY() + 2.5 * R * dy;
            dirx = dx;
            diry = dy;
            if (etiquetaBucleAlLado && Math.abs(dy) > 0.5) { // etiqueta a la derecha del bucle
                lx = a.getX() + 1.4 * R;
                ly = a.getY() + 2.1 * R * dy;
                dirx = 1;
                diry = 0;
            }
        } else {
            double mx = (a.getX() + b.getX()) / 2, my = (a.getY() + b.getY()) / 2;
            double dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), len = Math.hypot(dx, dy);
            double nx = -dy / len, ny = dx / len;
            double cx = mx + nx * curva, cy = my + ny * curva;
            double as = Math.atan2(cy - a.getY(), cx - a.getX()), be = Math.atan2(cy - b.getY(), cx - b.getX());
            double sx = a.getX() + R * Math.cos(as), sy = a.getY() + R * Math.sin(as);
            double ex = b.getX() + R * Math.cos(be), ey = b.getY() + R * Math.sin(be);
            g.draw(new QuadCurve2D.Double(sx, sy, cx, cy, ex, ey));
            punta(g, ex, ey, Math.atan2(ey - cy, ex - cx));
            lx = 0.25 * sx + 0.5 * cx + 0.25 * ex;
            ly = 0.25 * sy + 0.5 * cy + 0.25 * ey;
            if (curva == 0 && ny > 0) { nx = -nx; ny = -ny; } // en líneas rectas la etiqueta va arriba
            if (curva < 0) { nx = -nx; ny = -ny; }
            lx += nx * 6;
            ly += ny * 6;
            dirx = nx;
            diry = ny;
        }
        // Etiqueta con fondo blanco para que se lea bien
        String[] lineas = etiqueta.split("\n");
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int ancho = 0;
        for (String l : lineas) ancho = Math.max(ancho, fm.stringWidth(l));
        int alto = lineas.length * 15;
        double bx = dirx > 0.5 ? lx : (dirx < -0.5 ? lx - ancho : lx - ancho / 2.0);
        double by = diry < -0.5 ? ly - alto : (diry > 0.5 ? ly : ly - alto / 2.0);
        g.setColor(new Color(255, 255, 255, 220));
        g.fill(new RoundRectangle2D.Double(bx - 3, by - 1, ancho + 6, alto + 2, 6, 6));
        g.setColor(resaltada ? ROJO : AZUL);
        for (int i = 0; i < lineas.length; i++) g.drawString(lineas[i], (float) bx, (float) (by + 12 + i * 15));
    }

    /**
     * Dibuja un diagrama de estados con los estados repartidos sobre una elipse.
     * @param etiquetas  clave "origen destino" -> texto de la flecha (varias líneas separadas por \n)
     * @param resaltado  clave "origen destino" de la última transición usada (se pinta en rojo)
     */
    protected void dibujarDiagrama(Graphics2D g, java.util.List<String> estados, java.util.Map<String, String> etiquetas,
                                   String inicial, java.util.Collection<String> finales, String activo, String resaltado,
                                   double cx, double cy, double rx, double ry) {
        int n = estados.size();
        java.util.Map<String, Point2D> pos = new java.util.HashMap<>();
        for (int i = 0; i < n; i++) {
            double ang = Math.PI + 2 * Math.PI * i / n; // el primer estado queda a la izquierda
            pos.put(estados.get(i), new Point2D.Double(cx + rx * Math.cos(ang), cy + ry * Math.sin(ang)));
        }
        for (java.util.Map.Entry<String, String> e : etiquetas.entrySet()) {
            String[] par = e.getKey().split(" ");
            Point2D a = pos.get(par[0]);
            boolean idaYVuelta = !par[0].equals(par[1]) && etiquetas.containsKey(par[1] + " " + par[0]);
            // Los bucles se dibujan hacia arriba, salvo en los estados de la mitad de abajo
            Point2D centro = new Point2D.Double(a.getX(), a.getY() > cy + 5 ? a.getY() - 100 : a.getY() + 100);
            dibujarTransicion(g, a, pos.get(par[1]), e.getValue(), e.getKey().equals(resaltado), idaYVuelta ? 30 : 0, centro);
        }
        for (String q : estados) {
            Point2D p = pos.get(q);
            dibujarEstado(g, p.getX(), p.getY(), q, q.equals(inicial), finales.contains(q), q.equals(activo));
        }
    }

    /**
     * Dibuja una cinta centrada horizontalmente.
     * @param cabezal posición del cabezal (puede ser igual a la longitud: "fin de la cadena")
     * @param leidos  celdas que ya fueron consumidas (se pintan en gris); -1 si no aplica
     */
    protected void dibujarCinta(Graphics2D g, int w, int y, String[] celdas, int cabezal, int leidos, String rotulo) {
        int n = Math.max(1, celdas.length);
        int t = Math.max(18, Math.min(42, (w - 60) / (n + 1)));
        int x0 = (w - n * t) / 2;
        for (int i = 0; i < celdas.length; i++) {
            Rectangle2D r = new Rectangle2D.Double(x0 + i * t, y, t, t);
            boolean frontera = celdas[i].equals("<") || celdas[i].equals(">");
            Color fondo = Color.WHITE;
            if (i < leidos) fondo = GRIS_CLARO;
            if (frontera) fondo = new Color(60, 60, 60);
            if (i == cabezal) fondo = AMARILLO;
            g.setColor(fondo);
            g.fill(r);
            g.setColor(AZUL);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(r);
            g.setFont(new Font("Monospaced", Font.BOLD, t > 28 ? 20 : 14));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(frontera && i != cabezal ? Color.WHITE : (i < leidos && i != cabezal ? Color.GRAY : TEXTO));
            g.drawString(celdas[i], (float) (x0 + i * t + (t - fm.stringWidth(celdas[i])) / 2.0), (float) (y + t / 2.0 + fm.getAscent() / 2.0 - 3));
        }
        if (cabezal >= 0) {
            double cx = x0 + cabezal * t + t / 2.0;
            Path2D p = new Path2D.Double();
            p.moveTo(cx, y + t + 4);
            p.lineTo(cx - 10, y + t + 20);
            p.lineTo(cx + 10, y + t + 20);
            p.closePath();
            g.setColor(ROJO);
            g.fill(p);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(rotulo, (float) (cx - fm.stringWidth(rotulo) / 2.0), y + t + 34);
        }
    }

    /** Posición x de la celda i de una cinta dibujada con dibujarCinta (sirve para flechas). */
    protected double centroCelda(int w, int n, int i) {
        n = Math.max(1, n);
        int t = Math.max(18, Math.min(42, (w - 60) / (n + 1)));
        int x0 = (w - n * t) / 2;
        return x0 + i * t + t / 2.0;
    }

    /** Caja con varias líneas de información. La primera línea va en negrita. */
    protected void dibujarCaja(Graphics2D g, int x, int y, String[] lineas) {
        Font normal = new Font("SansSerif", Font.PLAIN, 14), negrita = new Font("SansSerif", Font.BOLD, 14);
        g.setFont(negrita);
        int ancho = 0;
        for (String l : lineas) ancho = Math.max(ancho, g.getFontMetrics().stringWidth(l));
        g.setColor(AZUL_CLARO);
        g.fill(new RoundRectangle2D.Double(x, y, ancho + 24, lineas.length * 20 + 14, 12, 12));
        g.setColor(AZUL);
        g.draw(new RoundRectangle2D.Double(x, y, ancho + 24, lineas.length * 20 + 14, 12, 12));
        for (int i = 0; i < lineas.length; i++) {
            g.setFont(i == 0 ? negrita : normal);
            g.setColor(i == 0 ? AZUL : TEXTO);
            g.drawString(lineas[i], x + 12, y + 24 + i * 20);
        }
    }

    /** Texto simple. */
    protected void texto(Graphics2D g, String s, double x, double y, int tam, boolean negrita, Color c) {
        g.setFont(new Font("SansSerif", negrita ? Font.BOLD : Font.PLAIN, tam));
        g.setColor(c);
        g.drawString(s, (float) x, (float) y);
    }
}
