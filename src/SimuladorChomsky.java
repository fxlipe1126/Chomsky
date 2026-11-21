import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Ventana principal del proyecto "Simulador Interactivo de la Jerarquía de Chomsky".
 *
 * Usa un CardLayout (como un mazo de cartas): la primera carta es el panel principal con
 * el resumen de la jerarquía, y las otras cuatro son los módulos de cada nivel.
 * Para ejecutar el proyecto se corre el método main de esta clase.
 */
public class SimuladorChomsky extends JFrame {

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenedor = new JPanel(tarjetas);

    // Datos de cada nivel: {clave de la tarjeta, título, descripción corta}
    private static final String[][] NIVELES = {
            {"n3", "Nivel 3 · Regulares", "Autómata finito determinista. Sin memoria auxiliar."},
            {"n2", "Nivel 2 · Libres de contexto", "Autómata de pila. Memoria LIFO."},
            {"n1", "Nivel 1 · Sensibles al contexto", "Autómata linealmente acotado. Cinta del tamaño de la entrada."},
            {"n0", "Nivel 0 · Recursivamente enumerables", "Máquina de Turing. Cinta infinita."}
    };
    // Colores de los anillos del diagrama (del Tipo 0, el más externo, al Tipo 3)
    private static final Color[] COLORES = {
            new Color(253, 235, 208), new Color(232, 218, 239), new Color(212, 230, 241), new Color(212, 239, 223)
    };

    public SimuladorChomsky() {
        super("Simulador Interactivo de la Jerarquía de Chomsky");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Runnable volver = () -> tarjetas.show(contenedor, "inicio");
        contenedor.add(crearPanelPrincipal(), "inicio");
        contenedor.add(new PanelAFD(volver), "n3");
        contenedor.add(new PanelAP(volver), "n2");
        contenedor.add(new PanelALA(volver), "n1");
        contenedor.add(new PanelMT(volver), "n0");
        setContentPane(contenedor);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1150, 720));
        setLocationRelativeTo(null);
    }

    /** Muestra el módulo indicado ("n3", "n2", "n1" o "n0"). */
    void abrir(String clave) {
        tarjetas.show(contenedor, clave);
    }

    // =====================================================================
    //  Panel principal
    // =====================================================================
    private JPanel crearPanelPrincipal() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(14, 18, 14, 18));

        // ----- Encabezado -----
        JLabel titulo = new JLabel("<html><div style='text-align:center'>"
                + "<span style='font-size:22pt;color:#1F4E79'><b>Simulador Interactivo de la Jerarquía de Chomsky</b></span><br>"
                + "<span style='font-size:12pt'>Autómatas, Gramáticas y Lenguajes · Politécnico Grancolombiano</span><br>"
                + "<span style='font-size:11pt;color:#555555'>La jerarquía clasifica las gramáticas formales en cuatro niveles. "
                + "Cada nivel contiene a los que están dentro de él, y a cada uno le corresponde una máquina que reconoce sus lenguajes.<br>"
                + "Seleccione un módulo con los botones o haciendo clic sobre el nivel en el diagrama.</span></div></html>",
                SwingConstants.CENTER);
        panel.add(titulo, BorderLayout.NORTH);

        // ----- Diagrama de conjuntos anidados -----
        JPanel diagrama = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                String[] nombres = {"Tipo 0 · Recursivamente enumerables · Máquina de Turing",
                        "Tipo 1 · Sensibles al contexto · Autómata linealmente acotado",
                        "Tipo 2 · Libres de contexto · Autómata de pila",
                        "Tipo 3 · Regulares · Autómata finito"};
                String[] ejemplos = {"ej.: suma unaria, palíndromos con MT",
                        "ej.: aⁿbⁿcⁿ,  w#w",
                        "ej.: aⁿbⁿ, paréntesis",
                        "ej.: (a|b)*ab"};
                for (int i = 0; i < 4; i++) {
                    RoundRectangle2D e = anillo(i, getWidth(), getHeight());
                    g.setColor(COLORES[i]);
                    g.fill(e);
                    g.setColor(new Color(31, 78, 121));
                    g.setStroke(new BasicStroke(2f));
                    g.draw(e);
                    g.setFont(new Font("SansSerif", Font.BOLD, i == 3 ? 13 : 14));
                    FontMetrics fm = g.getFontMetrics();
                    float y = (float) e.getY() + 26;
                    g.drawString(nombres[i], (float) (e.getCenterX() - fm.stringWidth(nombres[i]) / 2.0), y);
                    g.setFont(new Font("SansSerif", Font.ITALIC, 12));
                    fm = g.getFontMetrics();
                    g.setColor(new Color(70, 70, 70));
                    g.drawString(ejemplos[i], (float) (e.getCenterX() - fm.stringWidth(ejemplos[i]) / 2.0), y + 17);
                }
                // Dentro del Tipo 3 se resume qué memoria agrega cada nivel
                RoundRectangle2D t3 = anillo(3, getWidth(), getHeight());
                String[] resumen = {"¿Qué agrega cada nivel?",
                        "Tipo 3: solo estados (memoria finita)",
                        "Tipo 2: + una pila (LIFO)",
                        "Tipo 1: + cinta de lectura/escritura del tamaño de la entrada",
                        "Tipo 0: + cinta infinita (todo lo computable)",
                        "Cada clase de lenguajes está contenida en la del anillo que la rodea"};
                for (int k = 0; k < resumen.length; k++) {
                    g.setFont(new Font("SansSerif", k == 0 ? Font.BOLD : Font.PLAIN, k == 0 ? 15 : 13));
                    g.setColor(k == 0 ? new Color(31, 78, 121) : new Color(50, 50, 50));
                    FontMetrics fm = g.getFontMetrics();
                    float yy = (float) (t3.getCenterY() - 40 + k * 24);
                    g.drawString(resumen[k], (float) (t3.getCenterX() - fm.stringWidth(resumen[k]) / 2.0), yy);
                }
            }
        };
        diagrama.setBackground(Color.WHITE);
        diagrama.setPreferredSize(new Dimension(700, 400));
        diagrama.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        diagrama.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                // Se revisa del anillo más interno al más externo
                for (int i = 3; i >= 0; i--) {
                    if (anillo(i, diagrama.getWidth(), diagrama.getHeight()).contains(ev.getPoint())) {
                        abrir("n" + i);
                        return;
                    }
                }
            }
        });

        // ----- Botones de los módulos -----
        JPanel botones = new JPanel(new GridLayout(4, 1, 0, 10));
        botones.setOpaque(false);
        botones.setPreferredSize(new Dimension(360, 400));
        for (int i = 0; i < NIVELES.length; i++) {
            String[] nv = NIVELES[i];
            JButton b = new JButton("<html><span style='font-size:13pt'><b>" + nv[1] + "</b></span><br>"
                    + "<span style='font-size:10pt'>" + nv[2] + "</span></html>");
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setBackground(COLORES[3 - i]);
            b.addActionListener(e -> abrir(nv[0]));
            botones.add(b);
        }
        JPanel centro = new JPanel(new BorderLayout(14, 0));
        centro.setOpaque(false);
        centro.add(diagrama, BorderLayout.CENTER);
        centro.add(botones, BorderLayout.EAST);
        panel.add(centro, BorderLayout.CENTER);

        // ----- Tabla resumen -----
        String[] columnas = {"Nivel", "Gramática", "Forma de las reglas", "Máquina reconocedora", "Memoria", "Ejemplo"};
        String[][] filas = {
                {"Tipo 3", "Regular", "A → aB  |  A → a", "Autómata finito (AFD / AFN)", "Ninguna, solo estados", "(a|b)*ab"},
                {"Tipo 2", "Libre de contexto", "A → γ", "Autómata de pila (AP)", "Pila (LIFO)", "aⁿbⁿ"},
                {"Tipo 1", "Sensible al contexto", "αAβ → αγβ,  γ ≠ λ", "Autómata linealmente acotado", "Cinta de tamaño |w|", "aⁿbⁿcⁿ"},
                {"Tipo 0", "Sin restricciones", "α → β", "Máquina de Turing", "Cinta infinita", "Funciones computables"}};
        JTable tabla = new JTable(filas, columnas) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla.setRowHeight(26);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c = 0; c < columnas.length; c++) tabla.getColumnModel().getColumn(c).setCellRenderer(centrado);
        JScrollPane sp = new JScrollPane(tabla);
        sp.setPreferredSize(new Dimension(100, 26 * 4 + 32));
        panel.add(sp, BorderLayout.SOUTH);
        return panel;
    }

    /** Rectángulo redondeado del anillo i (0 = Tipo 0, el más grande; 3 = Tipo 3, el más pequeño). */
    private static RoundRectangle2D anillo(int i, int w, int h) {
        double x = 10 + i * 45, y = 10 + i * 62;
        return new RoundRectangle2D.Double(x, y, w - 2 * x, h - y - 10 - i * 14, 70, 70);
    }

    // =====================================================================
    //  Punto de entrada del programa
    // =====================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                    if ("Nimbus".equals(info.getName())) UIManager.setLookAndFeel(info.getClassName());
            } catch (Exception ignorado) {
                // si Nimbus no está disponible se usa el aspecto por defecto
            }
            new SimuladorChomsky().setVisible(true);
        });
    }
}
