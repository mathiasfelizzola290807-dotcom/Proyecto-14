import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

// Representa un equipo agrícola con su estado operativo y de mantenimiento.
class EquipoAgricola {
    String identificador;
    String tipo;
    int horasRestantes;
    int horasTotalesTrabajadas;
    int umbralMantenimiento;

    public EquipoAgricola(String identificador, String tipo, int horasRestantes, int umbralMantenimiento) {
        this.identificador = identificador;
        this.tipo = tipo;
        this.horasRestantes = horasRestantes;
        this.horasTotalesTrabajadas = 0;
        this.umbralMantenimiento = umbralMantenimiento;
    }
}

public class GestorMaquinariaAgricola {
    private final Queue<EquipoAgricola> colaPrioridadAlta = new LinkedList<>();
    private final Queue<EquipoAgricola> colaPrioridadBaja = new LinkedList<>();
    private final Stack<String> pilaCambiosContexto = new Stack<>();
    private final int[] umbralesGlobales = {100, 250, 500};
    private final double[][] estadisticasRendimiento = new double[5][3];
    private final List<String> inventarioMaquinariaActiva = new LinkedList<>();
    private final LinkedList<String> listaPrioridadesTrabajo = new LinkedList<>();
    private final List<String> turnosConfigurados = new ArrayList<>();
    private final List<EquipoAgricola> maquinaria = new ArrayList<>();

    private final DefaultTableModel maquinariaModel = new DefaultTableModel(
            new Object[]{"ID", "Tipo", "Prioridad", "Horas restantes", "Horas trabajadas", "Mantenimiento"},
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel estadisticasModel = new DefaultTableModel(
            new Object[]{"ID", "Combustible (L)", "Horas trabajadas", "Tiempo muerto (h)"},
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTextArea historialArea = new JTextArea();
    private final JLabel estadoLabel = new JLabel("Sistema listo");
    private JFrame frame;

    public void inicializarSistema() {
        // Equipos de ejemplo con carga inicial típica del campo.
        EquipoAgricola tractor = new EquipoAgricola("TR-01", "Tractor", 30, 100);
        EquipoAgricola cosechadora = new EquipoAgricola("COS-02", "Cosechadora", 45, 250);

        maquinaria.add(tractor);
        maquinaria.add(cosechadora);
        colaPrioridadAlta.add(tractor);
        colaPrioridadBaja.add(cosechadora);
        inventarioMaquinariaActiva.add("TR-01 (Activo)");
        inventarioMaquinariaActiva.add("COS-02 (Activo)");
        listaPrioridadesTrabajo.add("Prioridad Alta: Cosecha urgente");
        listaPrioridadesTrabajo.add("Prioridad Baja: Labranza secundaria");
        turnosConfigurados.add("Turno Mañana (06:00 - 14:00)");
        turnosConfigurados.add("Turno Tarde (14:00 - 22:00)");
        turnosConfigurados.add("Turno Noche (22:00 - 06:00)");
    }

    private void ejecutarPlanificacionPorTurnos(int duracionTurno) {
        if (colaPrioridadAlta.isEmpty() && colaPrioridadBaja.isEmpty()) {
            registrarEnHistorial("No hay tareas pendientes para planificar.");
            return;
        }

        registrarEnHistorial("--- Planificación por turnos (duración: " + duracionTurno + " h) ---");
        while (!colaPrioridadAlta.isEmpty() || !colaPrioridadBaja.isEmpty()) {
            while (!colaPrioridadAlta.isEmpty()) {
                procesarEquipo(colaPrioridadAlta.poll(), duracionTurno, false);
            }
            while (!colaPrioridadBaja.isEmpty()) {
                procesarEquipo(colaPrioridadBaja.poll(), duracionTurno, true);
            }
        }
        registrarEnHistorial("Planificación completada.");
        actualizarTablas();
        estadoLabel.setText("Planificación completada");
    }

    private void procesarEquipo(EquipoAgricola equipoActual, int quantum, boolean prioridadBaja) {
        int tiempoEjecutado = Math.min(quantum, equipoActual.horasRestantes);
        String accion = prioridadBaja ? "Retomando " : "Ejecutando ";
        String evento = "Cambio de contexto: " + accion + equipoActual.identificador + " por "
                + tiempoEjecutado + " h.";
        pilaCambiosContexto.push(evento);
        registrarEnHistorial(evento);

        equipoActual.horasRestantes -= tiempoEjecutado;
        equipoActual.horasTotalesTrabajadas += tiempoEjecutado;
        if (equipoActual.horasRestantes > 0) {
            colaPrioridadBaja.add(equipoActual);
            registrarEnHistorial("  " + equipoActual.identificador + " consumió su quantum; pasa a prioridad baja.");
        } else {
            registrarEnHistorial("  La tarea de " + equipoActual.identificador + " finalizó correctamente.");
        }
    }

    private void registrarEstadistica(int indiceEquipo, double combustible, double horas, double tiempoMuerto) {
        estadisticasRendimiento[indiceEquipo][0] = combustible;
        estadisticasRendimiento[indiceEquipo][1] = horas;
        estadisticasRendimiento[indiceEquipo][2] = tiempoMuerto;
    }

    private void mostrarVentanaPrincipal() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            System.err.println("No se pudo aplicar el estilo del sistema: " + exception.getMessage());
        }

        frame = new JFrame("Gestor de Maquinaria Agrícola");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(850, 560));
        frame.setSize(1050, 680);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(0, 12));
        frame.getContentPane().setBackground(new Color(245, 248, 243));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(new Color(38, 91, 57));
        encabezado.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel titulo = new JLabel("Gestor de Maquinaria Agrícola");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subtitulo = new JLabel("Control de equipos y planificación de tareas");
        subtitulo.setForeground(new Color(220, 235, 221));
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel textosEncabezado = new JPanel(new GridLayout(2, 1, 0, 4));
        textosEncabezado.setOpaque(false);
        textosEncabezado.add(titulo);
        textosEncabezado.add(subtitulo);
        encabezado.add(textosEncabezado, BorderLayout.CENTER);
        frame.add(encabezado, BorderLayout.NORTH);

        JTabbedPane pestañas = new JTabbedPane();
        pestañas.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        pestañas.addTab("Maquinaria", crearPanelEquipos());
        pestañas.addTab("Estadísticas", crearPanelEstadisticasUso());
        pestañas.addTab("Historial", crearPanelEventos());
        frame.add(pestañas, BorderLayout.CENTER);

        JPanel piePagina = new JPanel(new BorderLayout());
        piePagina.setBackground(new Color(238, 243, 236));
        piePagina.setBorder(BorderFactory.createEmptyBorder(9, 24, 9, 24));
        estadoLabel.setForeground(new Color(70, 90, 73));
        piePagina.add(estadoLabel, BorderLayout.WEST);
        JLabel turnosLabel = new JLabel("Turnos configurados: 3");
        turnosLabel.setForeground(new Color(70, 90, 73));
        piePagina.add(turnosLabel, BorderLayout.EAST);
        frame.add(piePagina, BorderLayout.SOUTH);

        actualizarTablas();
        frame.setVisible(true);
    }

    private JPanel crearPanelEquipos() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(new Color(245, 248, 243));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 6, 12, 6));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        acciones.setOpaque(false);
        JButton agregarButton = crearBoton("Agregar maquinaria", new Color(55, 117, 70));
        agregarButton.addActionListener(event -> mostrarDialogoAgregarEquipo());
        acciones.add(agregarButton);

        acciones.add(new JLabel("Duración del turno (horas):"));
        JSpinner quantumSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 500, 1));
        quantumSpinner.setPreferredSize(new Dimension(78, 32));
        acciones.add(quantumSpinner);
        JButton planificarButton = crearBoton("Ejecutar planificación", new Color(44, 95, 138));
        planificarButton.addActionListener(event ->
                ejecutarPlanificacionPorTurnos((Integer) quantumSpinner.getValue()));
        acciones.add(planificarButton);
        panel.add(acciones, BorderLayout.NORTH);

        JTable tabla = crearTabla(maquinariaModel);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JLabel nota = new JLabel("El equipo baja de prioridad cuando requiere más de un turno de trabajo.");
        nota.setForeground(new Color(92, 105, 92));
        panel.add(nota, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelEstadisticasUso() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(new Color(245, 248, 243));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 6, 12, 6));
        JLabel descripcion = new JLabel("Resumen de uso registrado por maquinaria");
        descripcion.setFont(new Font("SansSerif", Font.BOLD, 15));
        panel.add(descripcion, BorderLayout.NORTH);
        panel.add(new JScrollPane(crearTabla(estadisticasModel)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelEventos() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(new Color(245, 248, 243));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 6, 12, 6));
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JLabel descripcion = new JLabel("Eventos de planificación y cambios de contexto");
        descripcion.setFont(new Font("SansSerif", Font.BOLD, 15));
        cabecera.add(descripcion, BorderLayout.WEST);
        JButton limpiarButton = crearBoton("Limpiar historial", new Color(116, 125, 115));
        limpiarButton.addActionListener(event -> historialArea.setText(""));
        cabecera.add(limpiarButton, BorderLayout.EAST);
        panel.add(cabecera, BorderLayout.NORTH);

        historialArea.setEditable(false);
        historialArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        historialArea.setBackground(Color.WHITE);
        historialArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(historialArea), BorderLayout.CENTER);
        return panel;
    }

    private JButton crearBoton(String texto, Color color) {
        JButton boton = new JButton(texto);
        boton.setForeground(Color.WHITE);
        boton.setBackground(color);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        return boton;
    }

    private JTable crearTabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(30);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        return tabla;
    }

    private void mostrarDialogoAgregarEquipo() {
        JTextArea idInput = new JTextArea(1, 12);
        JComboBox<String> tipoInput = new JComboBox<>(new String[]{"Tractor", "Cosechadora", "Aspersor"});
        JSpinner horasInput = new JSpinner(new SpinnerNumberModel(20, 1, 10000, 1));
        JComboBox<String> prioridadInput = new JComboBox<>(new String[]{"Alta", "Baja"});
        JComboBox<Integer> umbralInput = new JComboBox<>(new Integer[]{umbralesGlobales[0], umbralesGlobales[1], umbralesGlobales[2]});
        JPanel formulario = new JPanel(new GridLayout(5, 2, 8, 8));
        formulario.add(new JLabel("Identificador:"));
        formulario.add(idInput);
        formulario.add(new JLabel("Tipo:"));
        formulario.add(tipoInput);
        formulario.add(new JLabel("Horas de tarea:"));
        formulario.add(horasInput);
        formulario.add(new JLabel("Prioridad inicial:"));
        formulario.add(prioridadInput);
        formulario.add(new JLabel("Umbral de mantenimiento:"));
        formulario.add(umbralInput);

        int resultado = JOptionPane.showConfirmDialog(frame, formulario,
                "Agregar maquinaria", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }
        String identificador = idInput.getText().trim().toUpperCase();
        if (identificador.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Ingresa un identificador para la maquinaria.",
                    "Dato requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (EquipoAgricola equipo : maquinaria) {
            if (equipo.identificador.equalsIgnoreCase(identificador)) {
                JOptionPane.showMessageDialog(frame, "Ya existe una maquinaria con ese identificador.",
                        "Identificador duplicado", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        EquipoAgricola nuevoEquipo = new EquipoAgricola(identificador, (String) tipoInput.getSelectedItem(),
                (Integer) horasInput.getValue(), (Integer) umbralInput.getSelectedItem());
        maquinaria.add(nuevoEquipo);
        inventarioMaquinariaActiva.add(identificador + " (Activo)");
        if ("Alta".equals(prioridadInput.getSelectedItem())) {
            colaPrioridadAlta.add(nuevoEquipo);
        } else {
            colaPrioridadBaja.add(nuevoEquipo);
        }
        registrarEnHistorial("Maquinaria " + identificador + " agregada con prioridad "
                + prioridadInput.getSelectedItem().toString().toLowerCase() + ".");
        actualizarTablas();
        estadoLabel.setText("Maquinaria " + identificador + " agregada");
    }

    private void actualizarTablas() {
        maquinariaModel.setRowCount(0);
        estadisticasModel.setRowCount(0);
        for (int i = 0; i < maquinaria.size(); i++) {
            EquipoAgricola equipo = maquinaria.get(i);
            boolean requiereMantenimiento = equipo.horasTotalesTrabajadas >= equipo.umbralMantenimiento;
            String prioridad = equipo.horasRestantes == 0 ? "Finalizada"
                    : (colaPrioridadAlta.contains(equipo) ? "Alta" : "Baja");
            maquinariaModel.addRow(new Object[]{
                equipo.identificador, equipo.tipo, prioridad, equipo.horasRestantes, equipo.horasTotalesTrabajadas,
                requiereMantenimiento ? "Requerido" : "Al día"
            });
            if (i < estadisticasRendimiento.length) {
                registrarEstadistica(i, 0, equipo.horasTotalesTrabajadas, 0);
            }
            estadisticasModel.addRow(new Object[]{
                equipo.identificador, "—", equipo.horasTotalesTrabajadas, "—"
            });
        }
    }

    private void registrarEnHistorial(String mensaje) {
        historialArea.append(mensaje + System.lineSeparator());
        historialArea.setCaretPosition(historialArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        GestorMaquinariaAgricola gestor = new GestorMaquinariaAgricola();
        gestor.inicializarSistema();
        SwingUtilities.invokeLater(gestor::mostrarVentanaPrincipal);
    }
}
