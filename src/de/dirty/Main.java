package de.dirty;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {

    private String name = "";

    public static void main(String[] args) {
//        unzip(new File("mcp.zip"), new File("tmp"));
        new Main(args);
    }

    public Main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-name")) {
                name = args[i + 1];
            }
        }
        if (name.equals("")) {
            System.err.println("Please enter a name");
            System.exit(-1);
        }

        File tmpDir = new File("tmp");
        System.out.println("Creating tmp dir");
        tmpDir.mkdirs();

        File zipFile = new File(tmpDir, "mcp.zip");
        System.out.println("Download mcp");
        Thread downloadThread = new Thread(() -> {
            downloadFile("http://www.modcoderpack.com/files/mcp918.zip", zipFile);
        });
        downloadThread.setName("downloadThread");
        downloadThread.start();
        try {
            downloadThread.join();
        } catch (InterruptedException e) {
            System.out.println("error while joining download thread");
            e.printStackTrace();
            System.exit(4);
        }

        System.out.println("unzip mcp");
        Thread unzipThread = new Thread(() -> {
            unzip(zipFile, tmpDir);
        });
        unzipThread.setName("unzipThread");
        unzipThread.start();
        try {
            unzipThread.join();
        } catch (InterruptedException e) {
            System.out.println("error while joining unzip thread");
            e.printStackTrace();
            System.exit(4);
        }

        System.out.println("Decompiling mcp");
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("cmd /C start /d \"" + tmpDir.getAbsolutePath() + "\" /wait decompile.bat");
            int exitVal = p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Error while decompiling mcp");
            System.exit(1);
        }
        System.out.println("Copying src");
        File tmpSrcDir = new File(tmpDir, "src");
        File srcDir = new File("src");

        new File(srcDir, "test/java").mkdirs();

        System.out.println("creating src dir structure");
        File mainDir = new File(srcDir, "main");
        mainDir.mkdirs();

        File javaDir = new File(mainDir, "java");
        javaDir.mkdirs();

        File resourcesDir = new File(mainDir, "resources");
        resourcesDir.mkdirs();

        if (tmpSrcDir.exists() && tmpSrcDir.isDirectory()) {
            try {
                copyFolder(tmpSrcDir, javaDir);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error while copying directory");
                System.exit(3);
            }
        } else {
            System.err.println("Error cannot find src dir");
            System.exit(2);
        }
        System.out.println("Download pom.xml");
        downloadFile("https://raw.githubusercontent.com/DasDirt/MCPRepository/master/pom.xml", new File("pom.xml"));

        System.out.println("Creating .iml file");
        File iml = new File(name + ".iml");
        try {
            iml.createNewFile();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(iml));
            bufferedWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            bufferedWriter.newLine();
            bufferedWriter.write("<module type=\"JAVA_MODULE\" version=\"4\" />");
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //https://stackabuse.com/how-to-download-a-file-from-a-url-in-java/
    private void downloadFile(String url, File dest) {
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOS = new FileOutputStream(dest)) {
            byte data[] = new byte[4096];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
        } catch (IOException ignored) {
        }
    }

    //https://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java
    static void unzip(File zipFilePath, File destDir) {
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        try {
            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] bytesIn = new byte[4096];
                    int read;
                    while ((read = zipIn.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }
                    bos.close();
                } else {
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //https://howtodoinjava.com/java/io/how-to-copy-directories-in-java/
    private static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
        if (sourceFolder.isDirectory()) {
            if (!destinationFolder.exists()) {
                destinationFolder.mkdir();
                System.out.println("Directory created :: " + destinationFolder);
            }
            String files[] = sourceFolder.list();
            for (String file : files) {
                File srcFile = new File(sourceFolder, file);
                File destFile = new File(destinationFolder, file);
                copyFolder(srcFile, destFile);
            }
        } else {
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied :: " + destinationFolder);
        }
    }
}
