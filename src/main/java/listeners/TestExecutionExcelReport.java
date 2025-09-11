package listeners;

import java.io.*;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestExecutionExcelReport {

    public static void writeResultsToExcel(String fileName, List<String[]> results, String baseSheetName) {
        Workbook workbook;
        Sheet sheet;
        File file = new File(fileName);

        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                fis.close();

                // Get or create the sheet
                sheet = workbook.getSheet(baseSheetName);
                if (sheet == null) {
                    sheet = workbook.createSheet(baseSheetName);
                    createHeaderRow(sheet, workbook);
                }
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet(baseSheetName);
                createHeaderRow(sheet, workbook);
            }

            // Define styles
            CellStyle passStyle = createStatusCellStyle(workbook, IndexedColors.GREEN);
            CellStyle failStyle = createStatusCellStyle(workbook, IndexedColors.RED);
            CellStyle skipStyle = createStatusCellStyle(workbook, IndexedColors.YELLOW);
            CellStyle defaultStyle = createBorderStyle(workbook);

            // Find the last row to append data
            int rowNum = sheet.getLastRowNum();
            if (rowNum == 0 && sheet.getRow(0) == null) {
                // If header wasn't written somehow
                createHeaderRow(sheet, workbook);
                rowNum = 0;
            }

            for (int i = 0; i < results.size(); i++) {
                Row row = sheet.createRow(++rowNum);

                // S.No
                Cell sno = row.createCell(0);
                sno.setCellValue(rowNum); // serial number
                sno.setCellStyle(defaultStyle);

                for (int j = 0; j < results.get(i).length; j++) {
                    Cell cell = row.createCell(j + 1);
                    cell.setCellValue(results.get(i)[j]);

                    if (j == 2) { // Status column
                        String status = results.get(i)[j].toUpperCase();
                        switch (status) {
                            case "PASS" -> cell.setCellStyle(passStyle);
                            case "FAIL" -> cell.setCellStyle(failStyle);
                            case "SKIP" -> cell.setCellStyle(skipStyle);
                            default -> cell.setCellStyle(defaultStyle);
                        }
                    } else {
                        cell.setCellStyle(defaultStyle);
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            // Save workbook
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();

            System.out.println("✅ Excel updated: " + baseSheetName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createHeaderRow(Sheet sheet, Workbook workbook) {
        String[] headers = { "S.No", "Test Script Name", "Test Description", "Status" };
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setAllBorders(style);
        return style;
    }

    private static CellStyle createStatusCellStyle(Workbook workbook, IndexedColors bgColor) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setAllBorders(style);
        return style;
    }

    private static CellStyle createBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setAllBorders(style);
        return style;
    }

    private static void setAllBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
