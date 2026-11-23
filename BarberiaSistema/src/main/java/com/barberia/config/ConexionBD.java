
package com.barberia.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL =
            "jdbc:mysql://localhost:3306/barberia_db"
              + "?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true";

    private static final String USUARIO = "root";

    private static final String CLAVE = "Amil2023";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
