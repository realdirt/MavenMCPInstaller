package de.dirty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {
  /** Entry point of this program just calls the main class. */
  public static void main(final String[] args) {
    new Main(args);
  }

  /** In this constructor handles everything. */
  public Main(final String[] args) {
    String path = "";
    String name = "";
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-help")) {
        sendHelp();
      }
      if (args[i].equals("-name")) {
        name = args[i + 1];
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
      }
      if (args[i].equals("-path")) {
        path = args[i + 1].toLowerCase();
      }
    }
    if (name.equals("")) {
      System.err.println("Please enter a name");
      sendHelp();
      System.exit(-1);
    }
    if (path.equals("")) {
      System.err.println("Please enter a path");
      sendHelp();
      System.exit(-2);
    }

    File tmpDir = new File("tmp");
    System.out.println("Creating tmp dir");
    createFolder(tmpDir);

    File zipFile = new File(tmpDir, "mcp.zip");
    System.out.println("Download mcp");
    downloadFile("http://www.modcoderpack.com/files/mcp918.zip", zipFile);

    System.out.println("unzip mcp");
    unzip(zipFile, tmpDir);

    System.out.println("Decompiling mcp");
    try {
      Runtime.getRuntime()
          .exec("cmd /C start /d \"" + tmpDir.getAbsolutePath() + "\" /wait decompile.bat");
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Error while decompiling mcp");
      System.exit(1);
    }
    System.out.println("Copying src");
    File srcDir = new File("src");

    System.out.println("creating src dir structure");

    createFolder(new File(srcDir, "test/java"));

    File mainDir = new File(srcDir, "main");
    createFolder(mainDir);

    File javaDir = new File(mainDir, "java");
    createFolder(javaDir);

    File pathDir = new File(javaDir, path.replace(".", "/"));
    createFolder(pathDir);

    System.out.println("creating resources dir");
    File resourcesDir = new File(mainDir, "resources");
    createFolder(resourcesDir);

    File tmpSrcDir = new File(tmpDir, "src");
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

    System.out.println("Creating Main class");
    File mainClass = new File(pathDir, name + ".java");
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(mainClass));
      bw.write("package " + path + ";");
      bw.newLine();
      bw.newLine();
      bw.write("public class " + name + " {");
      bw.newLine();
      bw.write("}");
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Download pom.xml");
    File pom = new File("pom.xml");
    downloadFile("https://raw.githubusercontent.com/DasDirt/MCPRepository/master/pom.xml", pom);

    System.out.println("Creating .iml file");
    File iml = new File(name + ".iml");
    try {
      if (!iml.createNewFile()) {
        System.err.println("Cannot create iml file");
        System.exit(8);
      }
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(iml));
      bufferedWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      bufferedWriter.newLine();
      bufferedWriter.write("<module type=\"JAVA_MODULE\" version=\"4\" />");
      bufferedWriter.newLine();
      bufferedWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Replacing name and path");
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(pom));
      List<String> lines = new ArrayList<>();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        lines.add(line);
      }
      bufferedReader.close();

      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(pom));
      for (String s : lines) {
        bufferedWriter.write(s.replace("de.dirty", path).replace("MavenMCP", name));
        bufferedWriter.newLine();
      }
      bufferedWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("deleting tmp dir");
    deleteFolder(tmpDir);

    System.out.println("copy minecraft to java folder");
    File tmpMinecraftDir = new File(javaDir, "/minecraft/");
    try {
      copyFolder(tmpMinecraftDir, javaDir);
    } catch (IOException e) {
      System.err.println("Error while copy minecraft to java folder");
      e.printStackTrace();
    }

    System.out.println("Delete tmp minecraft dir");
    deleteFolder(tmpMinecraftDir);
  }

  private void createFolder(File folder) {
    if (!folder.mkdirs()) {
      System.err.println("Cannot create folder: " + folder.getName());
      System.exit(9);
    }
  }

  private void sendHelp() {
    System.out.println("---Usage---");
    System.out.println("-help shows this output");
    System.out.println("-name the name of your client");
    System.out.println("-path your path like de.dirty");
    System.out.println("-----------");
  }

  // https://stackabuse.com/how-to-download-a-file-from-a-url-in-java/
  private void downloadFile(String url, File dest) {
    try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
        FileOutputStream fileOS = new FileOutputStream(dest)) {
      byte[] data = new byte[4096];
      int byteContent;
      while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
        fileOS.write(data, 0, byteContent);
      }
    } catch (IOException e) {
      System.err.println("Cannot download file");
      e.printStackTrace();
      System.exit(7);
    }
  }

  // https://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java
  private void unzip(File zipFilePath, File destDir) {
    if (!destDir.exists()) {
      createFolder(destDir);
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
          createFolder(dir);
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
      zipIn.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // https://howtodoinjava.com/java/io/how-to-copy-directories-in-java/
  private void copyFolder(final File sourceFolder, final File destinationFolder)
      throws IOException {
    if (sourceFolder.isDirectory()) {
      if (!destinationFolder.exists()) {
        createFolder(destinationFolder);
        System.out.println("Directory created :: " + destinationFolder);
      }
      String[] files = sourceFolder.list();
      if (files != null) {
        for (String file : files) {
          File srcFile = new File(sourceFolder, file);
          File destFile = new File(destinationFolder, file);
          copyFolder(srcFile, destFile);
        }
      }
    } else {
      Files.copy(
          sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
      System.out.println("File copied :: " + destinationFolder);
    }
  }

  // https://softwarecave.org/2018/03/24/delete-directory-with-contents-in-java/
  private void deleteFolder(final File folder) {
    File[] files = folder.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          deleteFolder(f);
        } else {
          System.out.println("Deleting file: " + f.getName());
          if (!f.delete()) {
            System.err.println("Cannot delete file: " + f.getName());
          }
        }
      }
    }
    System.out.println("Deleting folder: " + folder.getName());
    if (!folder.delete()) {
      System.err.println("Cannot delete folder: " + folder.getName());
    }
  }
}
