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
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

class EquipoAgricola {
    final String identificador;
    final String tipo;
    int horasRestantes;
    int horasTotalesTrabajadas;
    final int umbralMantenimiento;

    EquipoAgricola(String identificador, String tipo, int horasRestantes, int umbralMantenimiento) {
        this.identificador = identificador;
        this.tipo = tipo;
        this.horasRestantes = horasRestantes;
        this.umbralMantenimiento = umbralMantenimiento;
    }
}

public class GestorMaquinariaAgricola {
    private final Queue<EquipoAgricola> colaPrioridadAlta = new LinkedList<>();
    private final Queue<EquipoAgricola> colaPrioridadBaja = new LinkedList<>();
    private final List<EquipoAgricola> equiposAgricolas = new ArrayList<>();

    private final DefaultTableModel modeloMaquinaria = crearModeloNoEditable(
            "ID", "Tipo", "Prioridad", "Horas restantes", "Horas trabajadas", "Mantenimiento");
    private final DefaultTableModel modeloEstadisticas = crearModeloNoEditable(
            "ID", "Combustible (L)", "Horas trabajadas", "Tiempo muerto (h)");
    private final JTextArea historialArea = new JTextArea();
    private final JLabel estadoLabel = new JLabel("Todo listo para organizar el trabajo.");
    private JFrame ventanaPrincipal;

    public void inicializarSistema() {
        EquipoAgricola tractor = new EquipoAgricola("TR-01", "Tractor", 30, 100);
        EquipoAgricola cosechadora = new EquipoAgricola("COS-02", "Cosechadora", 45, 250);

        equiposAgricolas.add(tractor);
        equiposAgricolas.add(cosechadora);
        colaPrioridadAlta.add(tractor);
        colaPrioridadBaja.add(cosechadora);
    }

    private void planificarTrabajoPorTurnos(int horasPorTurno) {
        if (colaPrioridadAlta.isEmpty() && colaPrioridadBaja.isEmpty()) {
            agregarAlHistorial("No hay tareas pendientes. Agrega un equipo para empezar.");
            estadoLabel.setText("No hay tareas pendientes.");
            return;
        }

        agregarAlHistorial("--- Empezamos: cada equipo trabajará hasta " + horasPorTurno + " h por turno. ---");
        // Primero atendemos los equipos urgentes; si no terminan, pasan a prioridad baja.
        while (!colaPrioridadAlta.isEmpty() || !colaPrioridadBaja.isEmpty()) {
            while (!colaPrioridadAlta.isEmpty()) {
                procesarTurnoDeEquipo(colaPrioridadAlta.poll(), horasPorTurno);
            }
            while (!colaPrioridadBaja.isEmpty()) {
                procesarTurnoDeEquipo(colaPrioridadBaja.poll(), horasPorTurno);
            }
        }
        agregarAlHistorial("Listo: ya atendimos todas las tareas pendientes.");
        actualizarTablas();
        estadoLabel.setText("Trabajo planificado");
    }

    private void procesarTurnoDeEquipo(EquipoAgricola equipo, int horasPorTurno) {
        int horasTrabajadas = Math.min(horasPorTurno, equipo.horasRestantes);
        String evento = equipo.identificador + " trabajará durante " + horasTrabajadas + " h.";
        agregarAlHistorial(evento);

        equipo.horasRestantes -= horasTrabajadas;
        equipo.horasTotalesTrabajadas += horasTrabajadas;
        if (equipo.horasRestantes > 0) {
            colaPrioridadBaja.add(equipo);
            agregarAlHistorial("  A " + equipo.identificador
                    + " todavía le queda trabajo; continuará con prioridad baja.");
        } else {
            agregarAlHistorial("  ¡Buen trabajo! " + equipo.identificador + " terminó su tarea.");
        }
    }

    private DefaultTableModel crearModeloNoEditable(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    private void mostrarVentanaPrincipal() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            System.err.println("No se pudo aplicar el estilo del sistema: " + exception.getMessage());
        }

        ventanaPrincipal = new JFrame("Gestor de Maquinaria Agrícola");
        ventanaPrincipal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventanaPrincipal.setMinimumSize(new Dimension(850, 560));
        ventanaPrincipal.setSize(1050, 680);
        ventanaPrincipal.setLocationRelativeTo(null);
        ventanaPrincipal.setLayout(new BorderLayout(0, 12));
        ventanaPrincipal.getContentPane().setBackground(new Color(245, 248, 243));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(new Color(38, 91, 57));
        encabezado.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel titulo = new JLabel("Gestor de Maquinaria Agrícola");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subtitulo = new JLabel("Organiza tus equipos y planifica el trabajo del campo.");
        subtitulo.setForeground(new Color(220, 235, 221));
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel textosEncabezado = new JPanel(new GridLayout(2, 1, 0, 4));
        textosEncabezado.setOpaque(false);
        textosEncabezado.add(titulo);
        textosEncabezado.add(subtitulo);
        encabezado.add(textosEncabezado, BorderLayout.CENTER);
        ventanaPrincipal.add(encabezado, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        pestanas.addTab("Maquinaria", crearPanelEquipos());
        pestanas.addTab("Estadísticas", crearPanelEstadisticasUso());
        pestanas.addTab("Historial", crearPanelEventos());
        ventanaPrincipal.add(pestanas, BorderLayout.CENTER);

        JPanel piePagina = new JPanel(new BorderLayout());
        piePagina.setBackground(new Color(238, 243, 236));
        piePagina.setBorder(BorderFactory.createEmptyBorder(9, 24, 9, 24));
        estadoLabel.setForeground(new Color(70, 90, 73));
        piePagina.add(estadoLabel, BorderLayout.WEST);
        JLabel turnosConfiguradosLabel = new JLabel("Turnos disponibles: 3");
        turnosConfiguradosLabel.setForeground(new Color(70, 90, 73));
        piePagina.add(turnosConfiguradosLabel, BorderLayout.EAST);
        ventanaPrincipal.add(piePagina, BorderLayout.SOUTH);

        actualizarTablas();
        ventanaPrincipal.setVisible(true);
    }

    private JPanel crearPanelEquipos() {
        JPanel panel = crearPanelContenido();

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelAcciones.setOpaque(false);
        JButton botonAgregarEquipo = crearBoton("Agregar equipo", new Color(55, 117, 70));
        botonAgregarEquipo.addActionListener(event -> mostrarDialogoAgregarEquipo());
        panelAcciones.add(botonAgregarEquipo);

        panelAcciones.add(new JLabel("Horas de trabajo por turno:"));
        JSpinner controlHorasPorTurno = new JSpinner(new SpinnerNumberModel(15, 1, 500, 1));
        controlHorasPorTurno.setPreferredSize(new Dimension(78, 32));
        panelAcciones.add(controlHorasPorTurno);
        JButton botonPlanificar = crearBoton("Planificar trabajo", new Color(44, 95, 138));
        botonPlanificar.addActionListener(event ->
                planificarTrabajoPorTurnos((Integer) controlHorasPorTurno.getValue()));
        panelAcciones.add(botonPlanificar);
        panel.add(panelAcciones, BorderLayout.NORTH);

        JTable tabla = crearTabla(modeloMaquinaria);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JLabel nota = new JLabel("Si no termina en un turno, el equipo continuará después con prioridad baja.");
        nota.setForeground(new Color(92, 105, 92));
        panel.add(nota, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelEstadisticasUso() {
        JPanel panel = crearPanelContenido();
        panel.add(crearTituloPanel("Así va el trabajo de cada equipo"), BorderLayout.NORTH);
        panel.add(new JScrollPane(crearTabla(modeloEstadisticas)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelEventos() {
        JPanel panel = crearPanelContenido();
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(crearTituloPanel("Aquí puedes revisar el trabajo que se ha realizado."),
                BorderLayout.WEST);
        JButton botonBorrarHistorial = crearBoton("Borrar historial", new Color(116, 125, 115));
        botonBorrarHistorial.addActionListener(event -> historialArea.setText(""));
        cabecera.add(botonBorrarHistorial, BorderLayout.EAST);
        panel.add(cabecera, BorderLayout.NORTH);

        historialArea.setEditable(false);
        historialArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        historialArea.setBackground(Color.WHITE);
        historialArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(historialArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelContenido() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(new Color(245, 248, 243));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 6, 12, 6));
        return panel;
    }

    private JLabel crearTituloPanel(String texto) {
        JLabel titulo = new JLabel(texto);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        return titulo;
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
        JTextField campoIdentificador = new JTextField(12);
        JComboBox<String> selectorTipoEquipo = new JComboBox<>(new String[]{"Tractor", "Cosechadora", "Aspersor"});
        JSpinner controlHorasTrabajo = new JSpinner(new SpinnerNumberModel(20, 1, 10000, 1));
        JComboBox<String> selectorPrioridadInicial = new JComboBox<>(new String[]{"Alta", "Baja"});
        JComboBox<Integer> selectorUmbralMantenimiento = new JComboBox<>(
                new Integer[]{100, 250, 500});
        JPanel formulario = new JPanel(new GridLayout(5, 2, 8, 8));
        formulario.add(new JLabel("Código del equipo:"));
        formulario.add(campoIdentificador);
        formulario.add(new JLabel("Tipo:"));
        formulario.add(selectorTipoEquipo);
        formulario.add(new JLabel("Horas de trabajo pendientes:"));
        formulario.add(controlHorasTrabajo);
        formulario.add(new JLabel("Prioridad para comenzar:"));
        formulario.add(selectorPrioridadInicial);
        formulario.add(new JLabel("Recordatorio de mantenimiento cada (h):"));
        formulario.add(selectorUmbralMantenimiento);

        int resultado = JOptionPane.showConfirmDialog(ventanaPrincipal, formulario,
                "Agregar un equipo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }
        String identificador = campoIdentificador.getText().trim().toUpperCase();
        if (identificador.isEmpty()) {
            JOptionPane.showMessageDialog(ventanaPrincipal, "Escribe un código para identificar el equipo.",
                    "Falta el código", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (EquipoAgricola equipo : equiposAgricolas) {
            if (equipo.identificador.equalsIgnoreCase(identificador)) {
                JOptionPane.showMessageDialog(ventanaPrincipal, "Ese código ya está en uso. Elige otro, por favor.",
                        "Código repetido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        EquipoAgricola nuevoEquipo = new EquipoAgricola(identificador, (String) selectorTipoEquipo.getSelectedItem(),
                (Integer) controlHorasTrabajo.getValue(), (Integer) selectorUmbralMantenimiento.getSelectedItem());
        equiposAgricolas.add(nuevoEquipo);
        if ("Alta".equals(selectorPrioridadInicial.getSelectedItem())) {
            colaPrioridadAlta.add(nuevoEquipo);
        } else {
            colaPrioridadBaja.add(nuevoEquipo);
        }
        agregarAlHistorial("Agregamos el equipo " + identificador + " con prioridad "
                + selectorPrioridadInicial.getSelectedItem().toString().toLowerCase() + ".");
        actualizarTablas();
        estadoLabel.setText("Equipo " + identificador + " agregado.");
    }

    private void actualizarTablas() {
        modeloMaquinaria.setRowCount(0);
        modeloEstadisticas.setRowCount(0);
        for (int i = 0; i < equiposAgricolas.size(); i++) {
            EquipoAgricola equipo = equiposAgricolas.get(i);
            boolean requiereMantenimiento = equipo.horasTotalesTrabajadas >= equipo.umbralMantenimiento;
            String prioridad = equipo.horasRestantes == 0 ? "Finalizada"
                    : (colaPrioridadAlta.contains(equipo) ? "Alta" : "Baja");
            modeloMaquinaria.addRow(new Object[]{
                equipo.identificador, equipo.tipo, prioridad, equipo.horasRestantes, equipo.horasTotalesTrabajadas,
                requiereMantenimiento ? "Requerido" : "Al día"
            });
            modeloEstadisticas.addRow(new Object[]{
                equipo.identificador, "—", equipo.horasTotalesTrabajadas, "—"
            });
        }
    }

    private void agregarAlHistorial(String mensaje) {
        historialArea.append(mensaje + System.lineSeparator());
        historialArea.setCaretPosition(historialArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        GestorMaquinariaAgricola gestor = new GestorMaquinariaAgricola();
        gestor.inicializarSistema();
        SwingUtilities.invokeLater(gestor::mostrarVentanaPrincipal);
    }
}
