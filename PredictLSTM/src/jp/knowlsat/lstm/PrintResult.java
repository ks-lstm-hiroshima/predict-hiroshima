package jp.knowlsat.lstm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//public class PrintResult {
//	FileWriter fileWriter;
//	String filePath;
//
//	public PrintResult(String filePath) {
//		File file = new File(filePath);
//		this.filePath = filePath;
//
//		try {
//			fileWriter = new FileWriter(file);
//		} catch (Exception e) {
//		}
//	}
//
//	public void w(String test) {
//		try {
//			fileWriter.write(test);
//			fileWriter.flush();
//		} catch (Exception e) {
//		}
//	}
//
//	public void wl(String test) {
//		try {
//			fileWriter.write(test);
//			fileWriter.write("\n");
//			fileWriter.flush();
//		} catch (Exception e) {
//		}
//	}
//
//	public void delete() {
//		File file = new File(filePath);
//
//		try {
//			fileWriter.close();
//			file.delete();
//		} catch (Exception e) {
//		}
//	}
//
//}

public class PrintResult {
	private static Path logDirPath = null;
	private static FileWriter errWriter = null;
	FileWriter fileWriter;
	Path filePath;

	public static void setNullClassFields() {
		PrintResult.logDirPath = null;
		PrintResult.errWriter = null;
	}

	public static void copyFile(Path src) throws IOException {
		if (PrintResult.logDirPath == null) {
			throw new IOException("The log directory has not been created yet.");
		}
		Files.copy(src, PrintResult.logDirPath.resolve(src.getFileName()));
	}

	public PrintResult(Path filePath) {

		LocalDateTime t_date = LocalDateTime.now();
		DateTimeFormatter t_df = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

		Path errLogDirPath = Path.of("err_log");
		try {
			Files.createDirectory(errLogDirPath);
		} catch (FileAlreadyExistsException e) {

		} catch (Exception e) {
			System.out.println("Failed to create error log directory. " + e.getMessage());
			System.exit(-1);
		}

		if (PrintResult.errWriter == null) {
			Path errLogFilePath = Path.of("err_log", t_df.format(t_date) + ".log");
			File errLogFile;
			try {
				errLogFile = Files.createFile(errLogFilePath).toFile();
				try {
					PrintResult.errWriter = new FileWriter(errLogFile);
				} catch (Exception ex) {
					System.out.println("Failed to create error log file writer. " + ex.getMessage());
					System.exit(-1);
				}
			} catch (Exception e) {
				System.out.println("Failed to create error log file. " + e.getMessage());
				System.exit(-1);
			}
		}
		if (PrintResult.logDirPath == null) {
			PrintResult.logDirPath = Path.of("log", t_df.format(t_date));
			try {
				Files.createDirectories(PrintResult.logDirPath);
			} catch (Exception e) {
				System.out.println("Failed to create log directory. " + e.getMessage());
				try {
					errWriter.write("Failed to create log directory. " + e.getMessage() + "\n");
				} catch (Exception ex) {
					System.out.println("Failed to write to error log file. " + ex.getMessage());
				}
				System.exit(-1);
			}
		}

		this.filePath = PrintResult.logDirPath.resolve(filePath);
		File file = this.filePath.toFile();

		try {
			fileWriter = new FileWriter(file);
		} catch (Exception e) {
			System.out.println("Failed to create file writer. " + e.getMessage());
			try {
				errWriter.write("Failed to create file writer. " + e.getMessage() + "\n");
			} catch (Exception ex) {
				System.out.println("Failed to write to error log file. " + ex.getMessage());
			}
		}
	}

	public void w(String test) {
		try {
			fileWriter.write(test);
			fileWriter.flush();
		} catch (Exception e) {
			System.out.println("Error occurred while writing a file. " + e.getMessage());
			try {
				errWriter.write("Failed to create file writer. " + e.getMessage() + "\n");
			} catch (Exception ex) {
				System.out.println("Failed to write to error log file. " + ex.getMessage());
			}
		}
	}

	public void wl(String test) {
		try {
			fileWriter.write(test);
			fileWriter.write("\n");
			fileWriter.flush();
		} catch (Exception e) {
			System.out.println("Error occurred while writing a file. " + e.getMessage());
			try {
				errWriter.write("Failed to create file writer. " + e.getMessage() + "\n");
			} catch (Exception ex) {
				System.out.println("Failed to write to error log file. " + ex.getMessage());
			}
		}
	}

	public void delete() {
		try {
			fileWriter.close();
		} catch (Exception e) {
			System.out.println("Failed to close file writer. " + e.getMessage());
			try {
				errWriter.write("Failed to create file writer. " + e.getMessage() + "\n");
			} catch (Exception ex) {
				System.out.println("Failed to write to error log file. " + ex.getMessage());
			}
		}
		try {
			Files.deleteIfExists(filePath);
		} catch (Exception e) {
			System.out.println("Failed to delete file. " + filePath.toString() + "  " + e.getMessage());
			try {
				errWriter.write("Failed to delete file. " + e.getMessage() + "\n");
			} catch (Exception ex) {
				System.out.println("Failed to write to error log file. " + ex.getMessage());
			}
		}
	}
	
	public void writeAll(CharSequence str) {
		try {
			Files.writeString(this.filePath, str, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println("Failed to write a batch of strings to a file.. " + filePath.toString() + e.getMessage());
			e.printStackTrace();
			try {
				errWriter.write("Failed to write a batch of strings to a file. " + filePath.toString() + e.getMessage() + "\n");
			} catch (Exception ex) {
				System.out.println("Failed to write to error log file. " + ex.getMessage());
			}
		}
	}
}

class PrintResultBuffer {
//	private StringBuilder buffer;
	private StringBuffer buffer;
	private Path filePath;
	
	// testRealSize * 300
	
	public PrintResultBuffer(Path filePath, int capacity) {
//		this.buffer = new StringBuilder(capacity);
		this.buffer = new StringBuffer(capacity);
		this.filePath = filePath;
	}

	public void w(String test) {
		buffer.append(test);
	}
	
	public void wl(String test) {
		buffer.append(test);
		buffer.append("\n");
	}
	
	public void writeToFile() {
		PrintResult newPr = new PrintResult(this.filePath);
		newPr.writeAll(this.buffer);
	}
}
