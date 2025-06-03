package org.utility;

import java.awt.Color;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class ExcelReportGenerator {

	public static void writeToExcel(String defaultPath, String sheetNames, String flags,
									String testCaseIds, String releases, String results,
									String accountHeader, String moduleName) {

		String[] sheetArr = sheetNames.split(",");
		String[] flagArr = flags.split(",");
		String[] tcIdArr = testCaseIds.split(",");
		String[] releaseArr = releases.split(",");
		String[] resultArr = results.split(",");

		String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

		for (int s = 0; s < sheetArr.length; s++) {
			if (!"yes".equalsIgnoreCase(flagArr[s])) continue;

			String sheetName = sheetArr[s];
			String filePath = defaultPath + sheetName + ".xlsx";
			File file = new File(filePath);

			try (Workbook workbook = file.exists()
					? new XSSFWorkbook(new FileInputStream(file))
					: new XSSFWorkbook()) {

				Sheet sheet = getOrCreateSheet(workbook, sheetName);
				Row headerRow = getOrCreateHeaderRow(sheet);

				CellStyle headerStyle = createHeaderCellStyle(workbook);
				createHeaderIfAbsent(headerRow, 0, "Module Name", headerStyle);
				createHeaderIfAbsent(headerRow, 1, "Test Case ID", headerStyle);

				int resultColIndex = getResultColumnIndex(headerRow, sheetName, today, releaseArr[0], accountHeader, headerStyle);

				CellStyle passStyle = createCellStyle(workbook, new XSSFColor(new Color(0, 97, 0), null), IndexedColors.WHITE.getIndex());
				CellStyle failStyle = createCellStyle(workbook, new XSSFColor(new Color(156, 0, 6), null), IndexedColors.WHITE.getIndex());

				for (int i = 0; i < tcIdArr.length; i++) {
					String tcId = tcIdArr[i];
					String res = resultArr[i];
					boolean updated = false;

					for (int r = 1; r <= sheet.getLastRowNum(); r++) {
						Row row = sheet.getRow(r);
						if (row != null && cellEquals(row.getCell(0), moduleName) && cellEquals(row.getCell(1), tcId)) {
							updateResultCell(row, resultColIndex, res, passStyle, failStyle);
							updated = true;
							break;
						}
					}

					if (!updated) {
						Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
						newRow.createCell(0).setCellValue(moduleName);
						newRow.createCell(1).setCellValue(tcId);
						updateResultCell(newRow, resultColIndex, res, passStyle, failStyle);
					}
				}

				try (FileOutputStream fos = new FileOutputStream(filePath)) {
					workbook.write(fos);
				}

				System.out.println("Workbook for " + sheetName + " updated successfully.");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void writeAutomationCreatedData(String createdDataname,String createdDate,String createdTime)
	{
		String sheetName = System.getProperty("AutomationDataSheetName");
		String filePath = System.getProperty("AutomationDataPath")+ sheetName + ".xlsx";
		Workbook workbook;
		File file = new File(filePath);
      try
      {
    	  if (file.exists())
  		  {
  			FileInputStream fis = new FileInputStream(file);
  			workbook = new XSSFWorkbook(fis);
  			fis.close();
  		  }
    	  else
  		  {
  			workbook = new XSSFWorkbook();
  			workbook.createSheet(sheetName);
  		  }
    	 Sheet sheet = workbook.getSheet(sheetName);
  		if (sheet == null)
  		{
  			sheet = workbook.createSheet(sheetName);
  		}
  		Row headerRow = sheet.getRow(0);
		if (headerRow == null)
		{
			headerRow = sheet.createRow(0);
		}
 
		CellStyle headerStyle = createHeaderCellStyle(workbook);
 
		// Set headers
		if (headerRow.getCell(0) == null)
		{
			Cell cell = headerRow.createCell(0);
			cell.setCellValue("Name");
			cell.setCellStyle(headerStyle);
		}
		if (headerRow.getCell(1) == null)
		{
			Cell cell = headerRow.createCell(1);
			cell.setCellValue("Date");
			cell.setCellStyle(headerStyle);
		}
		if (headerRow.getCell(2) == null)
		{
			Cell cell = headerRow.createCell(2);
			cell.setCellValue("Time");
			cell.setCellStyle(headerStyle);
		}

		int newRowNum = sheet.getLastRowNum()+1;
		Row newRow = sheet.createRow(newRowNum);
		for (Cell cell : headerRow)
		{
			String header=cell.getStringCellValue();
			int columnindex = cell.getColumnIndex();
			if (header.equalsIgnoreCase("Name"))
			{
	              Cell namevaluecell = newRow.createCell(columnindex);
	              namevaluecell.setCellValue(createdDataname);
			}
			else if (header.equalsIgnoreCase("Date"))
			{
				 Cell namevaluecell = newRow.createCell(columnindex);
	             namevaluecell.setCellValue(createdDate);
			}
			else if (header.equalsIgnoreCase("Time"))
			{
				 Cell namevaluecell = newRow.createCell(columnindex);
	             namevaluecell.setCellValue(createdTime);
			}
			else
			{
				System.out.println("Header now Fount");
			}

		}
		workbook.close();	  
	 } 
      catch (Exception e)
      {
		e.printStackTrace();
	  }

	}

	private static Sheet getOrCreateSheet(Workbook workbook, String name) {
		Sheet sheet = workbook.getSheet(name);
		return sheet != null ? sheet : workbook.createSheet(name);
	}

	private static Row getOrCreateHeaderRow(Sheet sheet) {
		Row row = sheet.getRow(0);
		return row != null ? row : sheet.createRow(0);
	}

	private static void createHeaderIfAbsent(Row row, int index, String value, CellStyle style) {
		if (row.getCell(index) == null) {
			Cell cell = row.createCell(index);
			cell.setCellValue(value);
			cell.setCellStyle(style);
		}
	}

	private static int getResultColumnIndex(Row headerRow, String sheetName, String today, String release, String accountHeader, CellStyle style) {
		String dynamicHeader = sheetName.equalsIgnoreCase("daily") ? today :
				sheetName.equalsIgnoreCase("release") ? "Release - " + release : accountHeader;

		for (int i = 2; i < headerRow.getLastCellNum(); i++) {
			Cell cell = headerRow.getCell(i);
			if (cell != null && dynamicHeader.equals(cell.getStringCellValue())) {
				return i;
			}
		}

		int newIndex = headerRow.getLastCellNum() == -1 ? 2 : headerRow.getLastCellNum();
		Cell cell = headerRow.createCell(newIndex);
		cell.setCellValue(dynamicHeader);
		cell.setCellStyle(style);
		return newIndex;
	}

	private static void updateResultCell(Row row, int colIndex, String result, CellStyle passStyle, CellStyle failStyle) {
		Cell cell = row.createCell(colIndex);
		cell.setCellValue(result);
		cell.setCellStyle(result.contains("PASS") ? passStyle : failStyle);
	}

	private static boolean cellEquals(Cell cell, String expected) {
		return cell != null && expected.equals(cell.getStringCellValue());
	}

	private static CellStyle createCellStyle(Workbook workbook, XSSFColor bgColor, short fontColor) {
		XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setColor(fontColor);
		style.setFont(font);
		style.setFillForegroundColor(bgColor);
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}

	private static CellStyle createHeaderCellStyle(Workbook workbook) {
		XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.BLACK.getIndex());
		style.setFont(font);
		style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}
}
