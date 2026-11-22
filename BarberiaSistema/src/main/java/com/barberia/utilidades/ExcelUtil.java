

package com.barberia.utilidades;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtil {

    public static void exportarCitas(
            List<Object[]> citas,
            String ruta) throws IOException {

        String[] encabezados = {
            "ID", "Cliente", "Barbero",
            "Servicio", "Fecha y Hora", "Estado"
        };

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet hoja = workbook.createSheet("Citas");

            // Estilo de encabezados
            CellStyle estilo = workbook.createCellStyle();

            Font fuente = workbook.createFont();
            fuente.setBold(true);
            estilo.setFont(fuente);

            Row filaTitulo = hoja.createRow(0);

            for (int i = 0; i < encabezados.length; i++) {
                Cell celda = filaTitulo.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estilo);
            }

            // Registrar las citas
            int numeroFila = 1;

            for (Object[] cita : citas) {

                Row fila = hoja.createRow(numeroFila++);

                for (int i = 0; i < cita.length; i++) {

                    Cell celda = fila.createCell(i);

                    if (cita[i] == null) {
                        celda.setCellValue("");
                    } else {
                        celda.setCellValue(cita[i].toString());
                    }
                }
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < encabezados.length; i++) {
                hoja.autoSizeColumn(i);
            }

            // Guardar archivo
            try (FileOutputStream archivo =
                    new FileOutputStream(ruta)) {

                workbook.write(archivo);
            }
        }
    }
}
