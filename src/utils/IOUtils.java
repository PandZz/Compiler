package utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.StringJoiner;

public class IOUtils {
    private static final String INPUT_FILE = "testfile.txt";
    private static final String OUTPUT_FILE = "output.txt";
    private static final String DEBUG_FILE = "debug.txt";
    private static final String ERROR_FILE = "error.txt";
    private static StringJoiner buffer = new StringJoiner("\n");
    public static String readFile(String path) throws IOException {
        InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(path)));
        Scanner scanner = new Scanner(in);
        StringJoiner stringJoiner = new StringJoiner("\n");
        while (scanner.hasNextLine()) {
            stringJoiner.add(scanner.nextLine());
        }
        scanner.close();
        in.close();
        return stringJoiner.toString();
    }

    public static void writeFile(String path, String content) throws IOException {
        File file = new File(path);
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(content);
        }
    }

    public static void appendBuffer(String content) {
        buffer.add(content);
    }

    public static void clearBuffer() {
        buffer = new StringJoiner("\n");
    }

    public static void writeBuffer2OutPut() throws IOException {
        writeFile(OUTPUT_FILE, buffer.toString());
    }

    public static void writeBuffer2Debug() throws IOException {
        writeFile(DEBUG_FILE, buffer.toString());
    }

    public static void writeBuffer2Error() throws IOException {
        writeFile(ERROR_FILE, buffer.toString());
    }

    public static String readInput() throws IOException {
        return readFile(INPUT_FILE);
    }
}
