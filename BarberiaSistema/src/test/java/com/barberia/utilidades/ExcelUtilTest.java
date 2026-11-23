
package com.barberia.utilidades;

import com.barberia.utilidades.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelUtilTest {

    @TempDir
    Path carpetaTemporal;

    @Test
    void debeGenerarArchivoExcelConCitas() throws Exception {

        Path archivo = carpetaTemporal.resolve("citas.xlsx");

       List<Object[]> citas = List.<Object[]>of(
    new Object[]{
        1,
        "Carlos",
        "Juan",
        "Corte clásico",
        "2026-11-21 10:00:00",
        "CONFIRMADA"
    }
);

        ExcelUtil.exportarCitas(
                citas,
                archivo.toString()
        );

        assertTrue(Files.exists(archivo));

        try (Workbook workbook =
                     WorkbookFactory.create(archivo.toFile())) {

            assertEquals(1, workbook.getNumberOfSheets());

            assertEquals(
                "Citas",
                workbook.getSheetAt(0).getSheetName()
            );

            assertEquals(
                "Carlos",
                workbook.getSheetAt(0)
                        .getRow(1)
                        .getCell(1)
                        .getStringCellValue()
            );
        }
    }
}
