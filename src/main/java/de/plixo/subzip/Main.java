package de.plixo.subzip;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Main {
    public static final Predicate<String> folderPredicate =
            name -> !name.contains("out") && name.endsWith("submission");

    public static void walkFileTree(File file, Predicate<File> action) {
        if (!file.isDirectory() || action.test(file)) {
            return;
        }
        final File[] files = file.listFiles();
        if (files != null) {
            for (final File subFile : files) {
                walkFileTree(subFile, action);
            }
        }
    }

    public static void main(String[] args) {
        try {
            //that's intentional
            File location = new File(new File("").getAbsolutePath());
            if (!location.exists()) {
                System.out.println("start path problem");
                return;
            }
            List<Runnable> delayedFunctions = new ArrayList<>();
            final AtomicInteger foldersZipped = new AtomicInteger();
            walkFileTree(location, file -> {
                if (folderPredicate.test(file.getAbsolutePath())) {

                    final File parentFile = file.getParentFile();
                    final File outFile = new File(parentFile.getAbsolutePath() + "/" + parentFile.getName() + ".zip");
                    if (!outFile.exists() || outFile.delete()) {
                        System.out.println("cleaning " + outFile.getName());
                        delayedFunctions.add(() -> {
                            System.out.println("zipping submission in " + parentFile.getName());
                            try {
                                final Path outPath = outFile.toPath();
                                Files.createFile(outPath);
                                pack(file.toPath(), outPath);
                            } catch (final IOException exception) {
                                exception.printStackTrace();
                                System.err.println("a error has occurred while zipping the file");
                            }
                        });
                    }
                    foldersZipped.getAndIncrement();
                    return true;
                }
                return false;
            });
            Thread.sleep(3000);
            delayedFunctions.forEach(Runnable::run);
            System.out.println("done zipping " + foldersZipped.intValue() + " folder" + (foldersZipped.intValue() > 1 ? "s" : ""));
        } catch (final Exception exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    exception.getMessage(),
                    exception.getClass().getName() + " has occurred", JOptionPane.ERROR_MESSAGE);

        }
    }

    /**
     * modified, see https://stackoverflow.com/a/32052016/14902251
     *
     * @param inPath source folder
     * @param outPath destination zip folder
     * @throws IOException for access and file name exceptions
     */
    public static void pack(Path inPath, Path outPath) throws IOException {
        //auto resource release
        try (final ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(outPath))) {
            //default treewalker smh
            Files.walk(inPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        final ZipEntry zipEntry = new ZipEntry(inPath.relativize(path).toString());
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
