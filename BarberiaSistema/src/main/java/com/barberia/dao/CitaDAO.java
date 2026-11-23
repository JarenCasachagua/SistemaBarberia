
package com.barberia.dao;

import com.barberia.config.ConexionBD;
import com.barberia.config.LoggerConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

public class CitaDAO {

    private static final Logger LOGGER =
            LoggerConfig.getLogger(CitaDAO.class);

    public List<Object[]> listarServicios() throws SQLException {
        List<Object[]> lista = new ArrayList<>();

        String sql = "SELECT id, nombre, precio, duracion_minutos "
                   + "FROM servicios WHERE activo = TRUE ORDER BY nombre";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getBigDecimal("precio"),
                    rs.getInt("duracion_minutos")
                });
            }

            LOGGER.info("Servicios consultados: {}", lista.size());

        } catch (SQLException e) {
            LOGGER.error("Error al listar servicios", e);
            throw e;
        }

        return lista;
    }

    public List<Object[]> listarBarberos() throws SQLException {
        List<Object[]> lista = new ArrayList<>();

        String sql = "SELECT b.id, u.nombre "
                   + "FROM barberos b "
                   + "JOIN usuarios u ON u.id = b.usuario_id "
                   + "WHERE b.activo = TRUE AND u.activo = TRUE "
                   + "ORDER BY u.nombre";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre")
                });
            }

            LOGGER.info("Barberos consultados: {}", lista.size());

        } catch (SQLException e) {
            LOGGER.error("Error al listar barberos", e);
            throw e;
        }

        return lista;
    }

    public void reservarCita(int clienteId, int barberoId,
                             int servicioId, LocalDateTime fechaHora)
            throws SQLException {

        if (fechaHora.isBefore(LocalDateTime.now().plusMinutes(5))) {
            throw new SQLException(
                    "Selecciona una fecha y hora futura, con al menos 5 minutos de anticipación.");
        }

        String comprobarServicio =
                "SELECT duracion_minutos FROM servicios "
              + "WHERE id = ? AND activo = TRUE";

        String comprobarBarbero =
                "SELECT id FROM barberos "
              + "WHERE id = ? AND activo = TRUE";

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try {
                int duracion;

                try (PreparedStatement ps =
                             con.prepareStatement(comprobarServicio)) {
                    ps.setInt(1, servicioId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "El servicio no está disponible.");
                        }
                        duracion = rs.getInt("duracion_minutos");
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(comprobarBarbero)) {
                    ps.setInt(1, barberoId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "El barbero no está disponible.");
                        }
                    }
                }

                String choque =
                        "SELECT COUNT(*) "
                      + "FROM citas c "
                      + "JOIN servicios s ON s.id = c.servicio_id "
                      + "WHERE c.barbero_id = ? "
                      + "AND c.estado <> 'CANCELADA' "
                      + "AND c.fecha_hora < DATE_ADD(?, INTERVAL ? MINUTE) "
                      + "AND DATE_ADD(c.fecha_hora, "
                      + "INTERVAL s.duracion_minutos MINUTE) > ?";

                try (PreparedStatement ps = con.prepareStatement(choque)) {
                    Timestamp inicio = Timestamp.valueOf(fechaHora);

                    ps.setInt(1, barberoId);
                    ps.setTimestamp(2, inicio);
                    ps.setInt(3, duracion);
                    ps.setTimestamp(4, inicio);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();

                        if (rs.getInt(1) > 0) {
                            throw new SQLException(
                                    "Ese barbero ya tiene una cita en ese horario.");
                        }
                    }
                }

                String insertar =
                        "INSERT INTO citas "
                      + "(cliente_id, barbero_id, servicio_id, fecha_hora) "
                      + "VALUES (?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(insertar)) {
                    ps.setInt(1, clienteId);
                    ps.setInt(2, barberoId);
                    ps.setInt(3, servicioId);
                    ps.setTimestamp(4, Timestamp.valueOf(fechaHora));
                    ps.executeUpdate();
                }

                con.commit();

                LOGGER.info(
                        "Cita reservada correctamente. Cliente: {}, Barbero: {}, Servicio: {}",
                        clienteId, barberoId, servicioId
                );

            } catch (SQLException ex) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    LOGGER.error("Error al revertir la transacción de reserva", rollbackError);
                    ex.addSuppressed(rollbackError);
                }

                LOGGER.error("Error al reservar cita", ex);
                throw ex;

            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.error("Error al restaurar AutoCommit", e);
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Error de conexión o transacción al reservar cita", e);
            throw e;
        }
    }

    public List<Object[]> listarCitasCliente(int clienteId)
            throws SQLException {
        return listarCitas("c.cliente_id = ?", clienteId);
    }

    public List<Object[]> listarCitasBarbero(int usuarioId)
            throws SQLException {
        String sql = "SELECT b.id FROM barberos b "
                   + "WHERE b.usuario_id = ? AND b.activo = TRUE";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, usuarioId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "Esta cuenta no está asociada a un barbero.");
                }

                return listarCitas("c.barbero_id = ?", rs.getInt("id"));
            }

        } catch (SQLException e) {
            LOGGER.error("Error al listar citas del barbero. Usuario: {}",
                    usuarioId, e);
            throw e;
        }
    }

    private List<Object[]> listarCitas(String condicion, int id)
            throws SQLException {
        List<Object[]> lista = new ArrayList<>();

        String sql =
                "SELECT c.id, u.nombre AS cliente, "
              + "ub.nombre AS barbero, s.nombre AS servicio, "
              + "c.fecha_hora, c.estado "
              + "FROM citas c "
              + "JOIN usuarios u ON u.id = c.cliente_id "
              + "JOIN barberos b ON b.id = c.barbero_id "
              + "JOIN usuarios ub ON ub.id = b.usuario_id "
              + "JOIN servicios s ON s.id = c.servicio_id "
              + "WHERE " + condicion
              + " ORDER BY c.fecha_hora DESC";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("cliente"),
                        rs.getString("barbero"),
                        rs.getString("servicio"),
                        rs.getTimestamp("fecha_hora"),
                        rs.getString("estado")
                    });
                }
            }

            LOGGER.info("Citas consultadas: {}", lista.size());

        } catch (SQLException e) {
            LOGGER.error("Error al consultar citas. ID: {}", id, e);
            throw e;
        }

        return lista;
    }

    public void actualizarEstado(int citaId, int usuarioId,
                                 String nuevoEstado) throws SQLException {

        if (!List.of("CONFIRMADA", "EN_ATENCION", "FINALIZADA",
                     "CANCELADA").contains(nuevoEstado)) {
            throw new SQLException("Estado no permitido.");
        }

        String sql =
                "UPDATE citas c "
              + "JOIN barberos b ON b.id = c.barbero_id "
              + "SET c.estado = ? "
              + "WHERE c.id = ? AND b.usuario_id = ? "
              + "AND c.estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_ATENCION')";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, citaId);
            ps.setInt(3, usuarioId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo actualizar la cita. Verifica su estado y asignación.");
            }

            LOGGER.info(
                    "Estado de cita actualizado. Cita: {}, Nuevo estado: {}",
                    citaId, nuevoEstado
            );

        } catch (SQLException e) {
            LOGGER.error("Error al actualizar estado de cita: {}", citaId, e);
            throw e;
        }
    }

    public void cancelarCitaCliente(int citaId, int clienteId)
            throws SQLException {

        String sql =
                "UPDATE citas SET estado = 'CANCELADA' "
              + "WHERE id = ? AND cliente_id = ? AND estado = 'PENDIENTE' "
              + "AND fecha_hora > NOW()";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, citaId);
            ps.setInt(2, clienteId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "Solo puedes cancelar tus citas pendientes futuras.");
            }

            LOGGER.info(
                    "Cita cancelada. Cita: {}, Cliente: {}",
                    citaId, clienteId
            );

        } catch (SQLException e) {
            LOGGER.error("Error al cancelar cita: {}", citaId, e);
            throw e;
        }
    }
}
