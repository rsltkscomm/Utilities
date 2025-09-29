package data;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import net.lingala.zip4j.ZipFile;
import reporting.TestLogManager;
import seleniumUtils.ScreenshotUtil;
import reporting.ExtentManager;

/**
 * Utility class for file operations with ExtentManager logging
 */
public class FileUtils
{

	// ---------------------- File Existence ----------------------
	public static boolean fileExists(String filePath)
	{
		try
		{
			File file = new File(filePath);
			boolean exists = file.exists();
			TestLogManager.dataInfo("File exists check", filePath + " -> " + exists);
			ExtentManager.infoTest("File exists check: " + filePath + " -> " + exists);
			return exists;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to check if file exists: " + filePath, e);
			ExtentManager.failTest("Failed to check if file exists: " + filePath + " -> " + e.getMessage());
			return false;
		}
	}

	// ---------------------- File Download ----------------------
	public static String isFileDownloaded(String downloadPath, String fileName)
	{
		try
		{
			File folder = new File(downloadPath);
			File[] files = folder.listFiles();

			if (files != null)
			{
				for (File file : files)
				{
					if (file.getName().contains(fileName))
					{
						TestLogManager.success("File downloaded: " + file.getName());
						ExtentManager.passTest("File downloaded: " + file.getName());
						return file.getName();
					}
				}
			}

			TestLogManager.warning("File not found: " + fileName);
			ExtentManager.infoTest("File not found: " + fileName);
			return null;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to check if file is downloaded: " + fileName, e);
			ExtentManager.failTest("Failed to check if file is downloaded: " + fileName + " -> " + e.getMessage());
			return null;
		}
	}

	// ---------------------- Zip File Extractor ----------------------
	public static String zipFileExtractor(String formName, String password)
	{
		try
		{
			String zipPath = System.getProperty("user.dir") + "\\src\\main\\resources\\Zip\\" + formName + ".zip";
			String extractPath = System.getProperty("user.dir") + "\\src\\main\\resources\\Zip\\";

			ZipFile zipFile = new ZipFile(zipPath);

			if (zipFile.isEncrypted())
				zipFile.setPassword(password.toCharArray());

			zipFile.extractAll(extractPath);

			TestLogManager.success("Zip file extracted: " + formName);
			ExtentManager.passTest("Zip file extracted: " + formName);

			return extractPath;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to extract zip file: " + formName, e);
			ExtentManager.failTest("Failed to extract zip file: " + formName + " -> " + e.getMessage());
			throw new RuntimeException("Failed to extract zip file", e);
		}
	}

	// ---------------------- CSV Reading ----------------------
	public static List<Map<String, String>> fileHandler()
	{
		List<Map<String, String>> result = new ArrayList<>();

		File path = new File(System.getProperty("user.dir") + "/src/main/resources/data/downloadedFile/");
		File[] files = path.listFiles();

		if (files == null || files.length == 0)
		{
			ExtentManager.infoTest("No files found in directory: " + path);
			return result;
		}

		for (File file : files)
		{
			String fileName = file.getName();
			if (!file.exists())
			{
				ExtentManager.infoTest("File not found: " + fileName);
				continue;
			}

			try
			{
				if (fileName.endsWith(".txt"))
				{
					// ✅ Handle TXT files (each line = one row)
					try (BufferedReader reader = new BufferedReader(new FileReader(file)))
					{
						String line;
						int lineNumber = 1;
						while ((line = reader.readLine()) != null)
						{
							Map<String, String> row = new HashMap<>();
							row.put("lineNumber", String.valueOf(lineNumber));
							row.put("line", line);
							result.add(row);
							lineNumber++;
						}
					}

				} else if (fileName.endsWith(".csv"))
				{
					// ✅ Handle CSV files (structured rows)
					List<String> headers = new ArrayList<>();

					try (BufferedReader br = new BufferedReader(new FileReader(file)))
					{
						String line;
						boolean isFirstLine = true;

						while ((line = br.readLine()) != null)
						{
							if (isFirstLine)
							{
								headers = parseCSVLine(line);
								isFirstLine = false;
							} else
							{
								List<String> values = parseCSVLine(line);
								Map<String, String> row = new HashMap<>();
								for (int i = 0; i < Math.min(headers.size(), values.size()); i++)
								{
									row.put(headers.get(i), values.get(i));
								}
								result.add(row);
							}
						}
					}
				}
			} catch (IOException e)
			{
				TestLogManager.error("Exception occurred while reading file: " + fileName, e);
			} finally
			{
				file.delete();
				ExtentManager.infoTest("The downloaded file has been deleted: " + fileName);
			}
		}
		return result;
	}

	public static List<String> parseCSVLine(String line)
	{
		List<String> values = new ArrayList<>();
		if (line == null || line.isEmpty())
		{
			return values;
		}

		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);

			if (c == '"')
			{
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"')
				{
					// Escaped double quote ("")
					current.append('"');
					i++; // Skip next quote
				} else
				{
					// Toggle inQuotes state
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes)
			{
				// Field separator
				values.add(current.toString().trim());
				current.setLength(0);
			} else
			{
				current.append(c);
			}
		}

		// Add last value
		values.add(current.toString().trim());
		return values;
	}

	public static List<String> getCSVHeaders(String csvPath)
	{
		try
		{
			List<String> headers = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(csvPath)))
			{
				String firstLine = br.readLine();
				if (firstLine != null)
					headers = parseCSVLine(firstLine);
			}
			TestLogManager.dataInfo("CSV headers", headers.toString());
			ExtentManager.infoTest("CSV headers: " + headers.toString());
			return headers;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to get CSV headers: " + csvPath, e);
			ExtentManager.failTest("Failed to get CSV headers: " + csvPath + " -> " + e.getMessage());
			throw new RuntimeException("Failed to get CSV headers", e);
		}
	}

	public static ArrayList<String> getFirstColumnDataFromCSV(String csvPath)
	{
		try
		{
			ArrayList<String> firstColumnData = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(csvPath)))
			{
				String line;
				boolean isFirstLine = true;
				while ((line = br.readLine()) != null)
				{
					if (!isFirstLine)
					{
						List<String> values = parseCSVLine(line);
						if (!values.isEmpty())
							firstColumnData.add(values.get(0));
					} else
						isFirstLine = false;
				}
			}
			TestLogManager.dataInfo("First column data", "Count: " + firstColumnData.size());
			ExtentManager.infoTest("First column data count: " + firstColumnData.size());
			return firstColumnData;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to get first column data from CSV: " + csvPath, e);
			ExtentManager.failTest("Failed to get first column data: " + csvPath + " -> " + e.getMessage());
			throw new RuntimeException("Failed to get first column data", e);
		}
	}

	// ---------------------- File Write / Read ----------------------
	public static void writeToFile(String filePath, String content)
	{
		try (FileWriter writer = new FileWriter(filePath))
		{
			writer.write(content);
			TestLogManager.success("Data written to file: " + filePath);
			ExtentManager.passTest("Data written to file: " + filePath);
		} catch (Exception e)
		{
			TestLogManager.error("Failed to write to file: " + filePath, e);
			ExtentManager.failTest("Failed to write to file: " + filePath + " -> " + e.getMessage());
			throw new RuntimeException("Failed to write to file", e);
		}
	}

	public static String readFileAsString(String filePath)
	{
		try
		{
			String content = new String(Files.readAllBytes(Paths.get(filePath)));
			TestLogManager.dataInfo("File content read", "Length: " + content.length());
			ExtentManager.infoTest("File content read: " + filePath + " -> Length: " + content.length());
			return content;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to read file as string: " + filePath, e);
			ExtentManager.failTest("Failed to read file: " + filePath + " -> " + e.getMessage());
			throw new RuntimeException("Failed to read file", e);
		}
	}

	// ---------------------- Delete / Directory ----------------------
	public static boolean deleteFile(String filePath)
	{
		try
		{
			File file = new File(filePath);
			boolean deleted = file.delete();
			if (deleted)
			{
				TestLogManager.success("File deleted: " + filePath);
				ExtentManager.passTest("File deleted: " + filePath);
			} else
			{
				TestLogManager.warning("Failed to delete file: " + filePath);
				ExtentManager.infoTest("Failed to delete file: " + filePath);
			}
			return deleted;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to delete file: " + filePath, e);
			ExtentManager.failTest("Failed to delete file: " + filePath + " -> " + e.getMessage());
			return false;
		}
	}

	public static void createDirectoryIfNotExists(String directoryPath)
	{
		try
		{
			File directory = new File(directoryPath);
			if (!directory.exists())
			{
				boolean created = directory.mkdirs();
				if (created)
				{
					TestLogManager.success("Directory created: " + directoryPath);
					ExtentManager.passTest("Directory created: " + directoryPath);
				} else
				{
					TestLogManager.warning("Failed to create directory: " + directoryPath);
					ExtentManager.infoTest("Failed to create directory: " + directoryPath);
				}
			} else
			{
				TestLogManager.info("Directory already exists: " + directoryPath);
				ExtentManager.infoTest("Directory already exists: " + directoryPath);
			}
		} catch (Exception e)
		{
			TestLogManager.error("Failed to create directory: " + directoryPath, e);
			ExtentManager.failTest("Failed to create directory: " + directoryPath + " -> " + e.getMessage());
			throw new RuntimeException("Failed to create directory", e);
		}
	}

	// ---------------------- File Utilities ----------------------
	public static long getFileSize(String filePath)
	{
		try
		{
			File file = new File(filePath);
			long size = file.length();
			TestLogManager.dataInfo("File size", filePath + " -> " + size + " bytes");
			ExtentManager.infoTest("File size: " + filePath + " -> " + size + " bytes");
			return size;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to get file size: " + filePath, e);
			ExtentManager.failTest("Failed to get file size: " + filePath + " -> " + e.getMessage());
			return -1;
		}
	}

	public static String getFileExtension(String fileName)
	{
		try
		{
			int lastDotIndex = fileName.lastIndexOf('.');
			if (lastDotIndex > 0)
			{
				String extension = fileName.substring(lastDotIndex + 1);
				TestLogManager.dataInfo("File extension", fileName + " -> " + extension);
				ExtentManager.infoTest("File extension: " + fileName + " -> " + extension);
				return extension;
			}
			return "";
		} catch (Exception e)
		{
			TestLogManager.error("Failed to get file extension: " + fileName, e);
			ExtentManager.failTest("Failed to get file extension: " + fileName + " -> " + e.getMessage());
			return "";
		}
	}

	public static boolean validateFileFormat(String fileName, String expectedExtension)
	{
		try
		{
			String actualExtension = getFileExtension(fileName);
			boolean isValid = expectedExtension.equalsIgnoreCase(actualExtension);
			TestLogManager.dataInfo("File format validation", fileName + " -> " + (isValid ? "Valid" : "Invalid"));
			ExtentManager.infoTest("File format validation: " + fileName + " -> " + (isValid ? "Valid" : "Invalid"));
			return isValid;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to validate file format: " + fileName, e);
			ExtentManager.failTest("Failed to validate file format: " + fileName + " -> " + e.getMessage());
			return false;
		}
	}

	public static String isFileNameExist(String fileNameContains)
	{
		try
		{
			String downloadPath = System.getProperty("user.dir") + "\\src\\main\\resources\\data\\downloadedFile\\";
			File folder = new File(downloadPath);
			File[] files = folder.listFiles();

			if (files != null)
			{
				for (File file : files)
				{
					if (file.getName().contains(fileNameContains))
					{
						TestLogManager.success("File found: " + file.getName());
						ExtentManager.passTest("File found: " + file.getName());
						return file.getName();
					}
				}
			}

			TestLogManager.warning("File not found containing: " + fileNameContains);
			ExtentManager.infoTest("File not found containing: " + fileNameContains);
			return null;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to check if file name exists: " + fileNameContains, e);
			ExtentManager.failTest("Failed to check file name existence: " + fileNameContains + " -> " + e.getMessage());
			return null;
		}
	}

	public static List<String> listFilesInDirectory(String directoryPath)
	{
		try
		{
			List<String> fileNames = new ArrayList<>();
			File directory = new File(directoryPath);
			File[] files = directory.listFiles();

			if (files != null)
			{
				for (File file : files)
				{
					if (file.isFile())
						fileNames.add(file.getName());
				}
			}

			TestLogManager.dataInfo("Files in directory", directoryPath + " -> " + fileNames.size() + " files");
			ExtentManager.infoTest("Files in directory: " + directoryPath + " -> " + fileNames.size() + " files");
			return fileNames;
		} catch (Exception e)
		{
			TestLogManager.error("Failed to list files in directory: " + directoryPath, e);
			ExtentManager.failTest("Failed to list files in directory: " + directoryPath + " -> " + e.getMessage());
			throw new RuntimeException("Failed to list files", e);
		}
	}

	// pdf File
	public void checkIsPdfExist()
	{
		File downloadDir = new File(System.getProperty("user.dir") + "/src/main/resources/data/DetailReports/");
		File[] pdfFiles = downloadDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

		if (pdfFiles == null || pdfFiles.length == 0)
		{
			ExtentManager.infoTest("No PDF files found in download folder.");
			return;
		}

		// Sort all PDF files by last modified time (descending)
		Arrays.sort(pdfFiles, Comparator.comparingLong(File::lastModified).reversed());

		// Pick the latest one
		File latestPdf = pdfFiles[0];

		if (latestPdf.exists())
		{
			String fileName = latestPdf.getName();
			String filePath = latestPdf.getAbsolutePath();
			String fileLink = "file:///" + filePath.replace("\\", "/");

			ExtentManager.infoTest("Latest downloaded PDF: " + fileName);
			try
			{
				// Clickable file link in Extent Report
				ExtentManager.infoTest("<a href='" + fileLink + "' target='_blank'>Open PDF File</a>");

				// Open the file
				Desktop.getDesktop().open(latestPdf);
				ExtentManager.infoTest(fileName + " opened successfully.");
				ScreenshotUtil.takeScreenshot();
				// Use Robot to close the file (Alt + F4)
				try
				{
					Robot robot = new Robot();
					robot.keyPress(KeyEvent.VK_ALT);
					robot.keyPress(KeyEvent.VK_F4);
					robot.keyRelease(KeyEvent.VK_F4);
					robot.keyRelease(KeyEvent.VK_ALT);
					ExtentManager.infoTest("PDF closed via Robot.");
				} catch (AWTException e)
				{
					ExtentManager.warningTest("Error using Robot to close PDF: " + e.getMessage());
				}

			} catch (IOException e)
			{
				ExtentManager.infoTest("Unable to open the PDF file: " + fileName);
			}
			ScreenshotUtil.takeScreenshot();
		} else
		{
			ExtentManager.warningTest("File does not exist: " + latestPdf.getName());
		}
	}
	
	public List<Map<String, String>> dataFileReader(String path)
	{
		List<Map<String, String>> data = new ArrayList<>();
		File csvFile = new File(path);
		if (csvFile.exists())
		{
			int rowCount = 0;
			try
			{
				BufferedReader reader = new BufferedReader(new FileReader(csvFile));
				String line;

				String[] headers = reader.readLine().split(",");
				while ((line = reader.readLine()) != null)
				{
					Map<String, String> map = new LinkedHashMap<>();
					String[] cell = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					for (int i = 0; i < headers.length; i++)
					{
						map.put(headers[i], cell[i]);
					}
					data.add(rowCount, map);
					rowCount++;
				}
				reader.close();
			} catch (IOException e)
			{
//					TestLogManager.error("Exception Occured while reading File " + e, e);
			}
		} else
		{
			ExtentManager.infoTest("File not found");
		}
		ExtentManager.infoTest("Data: " + data);
		return data;
	}
}
