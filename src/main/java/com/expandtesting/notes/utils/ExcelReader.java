package com.expandtesting.notes.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelReader {

    /**
     * Reads any given Excel sheet from your TestData.xlsx workbook and outputs
     * a multi-dimensional Object array perfectly formatted for TestNG DataProviders.
     * * @param filePath  Absolute or relative system path to your TestData.xlsx file
     * @param sheetName The exact name of the tab (e.g., "LoginData", "NotesData", "E2EData")
     * @return Object[][] multi-dimensional array containing your test rows
     */
    public static Object[][] getSheetData(String filePath, String sheetName) {
        Object[][] data = null;

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            // 1. Fetch the requested sheet tab
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("Target sheet tab '" + sheetName + "' not found in Excel workbook!");
            }

            // 2. Determine matrix dimensions (skipping the first header row)
            int totalRows = sheet.getLastRowNum();
            int totalCols = sheet.getRow(0).getLastCellNum();

            data = new Object[totalRows][totalCols];

            // 3. DataFormatter smoothly turns dates, integers, or decimals into safe Java Strings
            DataFormatter formatter = new DataFormatter();

            // i = 1 to skip your spreadsheet header row (TestCaseID, ScenarioID, etc.)
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);

                for (int j = 0; j < totalCols; j++) {
                    if (row != null) {
                        Cell cell = row.getCell(j);
                        // Safely maps blank cells to empty strings to avoid NullPointerExceptions
                        if (cell != null) {
                            data[i - 1][j] = formatter.formatCellValue(cell).trim();
                        } else {
                            data[i - 1][j] = "";
                        }
                    } else {
                        data[i - 1][j] = "";
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Unable to load or read your TestData.xlsx file structure.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return data;
    }
}