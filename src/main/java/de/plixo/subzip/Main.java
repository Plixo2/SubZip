package de.plixo.subzip;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Main {
    public static final Predicate<String> folderPredicate =
            name -> !name.contains("out") && name.endsWith("submission");

    public static void main(String[] args) {
        try {
            final Path location = Path.of(new File("").getAbsolutePath());
            if (!Files.exists(location)) {
                return;
            }
            final AtomicInteger foldersZipped = new AtomicInteger();
            Files.walk(location).filter(Files::isDirectory).filter(path -> folderPredicate.test(path.toString()))
                    .forEach(path -> {
                        final Path outLocation = Paths.get(path.toString() + ".zip");
                        System.out.println("zipping " + path.toString());
                        if (!Files.exists(outLocation)) {
                            try {
                                Files.createFile(outLocation);
                            } catch (final IOException exception) {
                                exception.printStackTrace();
                                System.err.println("could not create a file");
                            }
                        }
                        try {
                            pack(path, outLocation);
                        } catch (final IOException exception) {
                            exception.printStackTrace();
                            System.err.println("a error has occurred while zipping the file");
                        }
                        foldersZipped.incrementAndGet();
                    });
            System.out.println("done zipping " + foldersZipped.intValue() + " folders");

        } catch (final Exception exception) {
            exception.printStackTrace();
            if (exception instanceof UncheckedIOException) {
                UncheckedIOException uncheckedIOException = (UncheckedIOException) exception;
                if (uncheckedIOException.getCause() instanceof AccessDeniedException) {
                    JOptionPane.showMessageDialog(null,
                            "AccessDeniedException has occurred, try moving this .jar " +
                                    "file to another location",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null,
                        exception.getClass().getName() + " has occurred",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    /**
     * modified, see https://stackoverflow.com/a/32052016/14902251
     *
     * @param inPath  source folder
     * @param outPath destination zip folder
     * @throws IOException for access and file name exceptions
     */
    public static void pack(Path inPath, Path outPath) throws IOException {
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(outPath))) {
            Files.walk(inPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(inPath.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
