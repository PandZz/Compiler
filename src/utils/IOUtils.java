package utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.StringJoiner;

public class IOUtils {
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

    public static void writeOutput(String content) throws IOException {
        writeFile("output.txt", content);
    }
}
