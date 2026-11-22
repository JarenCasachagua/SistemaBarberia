
package com.barberia.dao;

import com.barberia.config.ConexionBD;
import com.barberia.config.LoggerConfig;
import com.barberia.modelos.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;

public class UsuarioDAO {

    private static final Logger LOGGER =
            LoggerConfig.getLogger(UsuarioDAO.class);

    public boolean registrar(String nombre, String correo, String clave)
            throws SQLException {

        String sql = "INSERT INTO usuarios (nombre, correo, password, rol) "
                   + "VALUES (?, ?, ?, 'CLIENTE')";

        String hash = BCrypt.hashpw(clave, BCrypt.gensalt(12));

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, correo);
            ps.setString(3, hash);

            boolean registrado = ps.executeUpdate() == 1;

            if (registrado) {
                LOGGER.info("Usuario registrado correctamente");
            }

            return registrado;

        } catch (SQLException e) {
            LOGGER.error("Error al registrar usuario", e);
            throw e;
        }
    }

    public Usuario iniciarSesion(String correo, String clave)
            throws SQLException {

        String sql = "SELECT id, nombre, correo, password, rol "
                   + "FROM usuarios "
                   + "WHERE correo = ? AND activo = TRUE";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, correo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password");

                    if (BCrypt.checkpw(clave, hash)) {

                        LOGGER.info("Inicio de sesión exitoso");

                        return new Usuario(
                                rs.getInt("id"),
                                rs.getString("nombre"),
                                rs.getString("correo"),
                                hash,
                                rs.getString("rol")
                        );
                    }
                }
            }

            LOGGER.info("Intento de inicio de sesión no válido");
            
        } catch (SQLException e) {
            LOGGER.error("Error al iniciar sesión", e);
            throw e;
        }

        return null;
    }
}