package data;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import base.BaseTest;
import reporting.TestLogManager;

public class XLSReader
{

	public String path;
	public FileInputStream fis = null;
	public FileOutputStream fos = null;
	private XSSFWorkbook workbook = null;
	private XSSFSheet sheet = null;
	private XSSFRow row = null;
	private XSSFCell cell = null;

	public XLSReader(String path) {
		this.path = path;
		try
		{
			fis = new FileInputStream(path);
			TestLogManager.info("Path is :" + path);
			workbook = new XSSFWorkbook(fis);
			sheet = workbook.getSheetAt(0);
			fis.close();
		} catch (Exception e)
		{
			TestLogManager.error("Exception occurred", e);
		}
	}

	// returns the row count in sheet
	public int getRowCount(String sheetName)
	{
		int index = workbook.getSheetIndex(sheetName);
		if (index == -1)
		{
			return 0;
		} else
		{
			sheet = workbook.getSheetAt(index);
			int number = sheet.getLastRowNum() + 1;
			return number;
		}
	}

	public String getCellData(String sheetName, String colname, int rowNum)
	{

		try
		{
			if (rowNum <= 0)
			{
				return "";
			}
			int index = workbook.getSheetIndex(sheetName);
			int col_num = -1;
			if (index == -1)
			{
				return "";
			}

			sheet = workbook.getSheetAt(index);
			row = sheet.getRow(0);
			for (int i = 0; i < row.getLastCellNum(); i++)
			{
				if (row.getCell(i).getStringCellValue().trim().equals(colname.trim()))
				{
					col_num = i;
				}
			}
			if (col_num == -1)
			{
				return "";
			}
			sheet = workbook.getSheetAt(index);
			row = sheet.getRow(rowNum - 1);
			if (row == null)
			{
				return "";
			}
			cell = row.getCell(col_num);
			if (cell == null)
			{
				return "";
			}

			if (cell.getCellType() != CellType.BLANK)
			{
				return cell.getStringCellValue();
			} else if (cell.getCellType() == CellType.BLANK)
			{
				return "";
			} else
			{
				return String.valueOf(cell.getBooleanCellValue());
			}
		} catch (Exception e)
		{
			TestLogManager.error("Exception occurred", e);
			return "row " + rowNum + " or column " + colname + " does not exist in xlsx";
		}
	}
}
