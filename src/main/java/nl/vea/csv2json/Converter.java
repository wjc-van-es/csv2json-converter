package nl.vea.csv2json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Converter {
    public static void convert(Path input, Path output, String separator) throws IOException {
        convert(Files.newBufferedReader(input), Files.newBufferedWriter(output), separator);
    }

    public static void convert(BufferedReader input, BufferedWriter output, String separator) throws IOException {
        output.write("[\n");
        final AtomicBoolean first = new AtomicBoolean(true);
        final AtomicReference<String> firstLine = new AtomicReference<>();
        input.lines().peek(System.out::println)
                .peek(line -> {
                    if (first.get()) {
                        firstLine.set(line);

                        first.set(false);
                    }
                })
                .forEach(line -> {
                    try {
                        output.write(writeLineAsJsonObjectString(separator, line, firstLine));
                        output.write(",\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        output.write("]");
    }

    private static String writeLineAsJsonObjectString(String separator, String line, AtomicReference<String> firstLine) {
        StringBuilder sb = new StringBuilder("{\n");
        String[] headers = firstLine.get().split(separator);
        String[] values = line.split(separator);
        if (headers.length != values.length) {
            System.out.println("This line is skipped, because of different number of fields:");
            System.out.println(line);
            System.out.println("Headers size %s should be the same as values size %s.".formatted(headers.length, values.length));
            return "";
        } else {
            for (int i = 0; i < headers.length; i++) {
                sb.append("\t\"")
                        .append(headers[i])
                        .append("\" : \"")
                        .append(values[i])
                        .append("\"");
                if (i < headers.length - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n}");
                }
            }
            return sb.toString();
        }
    }

}