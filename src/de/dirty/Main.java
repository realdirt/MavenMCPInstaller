/*
 * Developed by DasDirt on 1/29/20, 3:36 PM.
 * Copyright (c) 2020, for MavenMCPInstaller by DasDirt
 * All rights reserved.
 */

package de.dirty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {

  public static final Charset CHARSET = StandardCharsets.UTF_8;
  public static final int BUFFER_SIZE = 4096;

  /** Entry point of this program just calls the main class. */
  public static void main(final String[] args) {
    new Main(args);
  }

  /** In this constructor handles everything. */
  public Main(final String[] args) {
    System.setProperty(
        "http.agent",
        "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2)"
            + " Gecko/20100316 Firefox/3.6.2");
    // There are other ways to handle the args
    String path = "";
    String name = "";
    boolean addOptifine = false;
    for (int i = 0; i < args.length; i++) {
      switch (args[i].toLowerCase()) {
        case "-name":
          name = args[i + 1];
          name = name.substring(0, 1).toUpperCase() + name.substring(1);
          break;
        case "-path":
          path = args[i + 1].toLowerCase();
          break;
        case "-optifine":
          addOptifine = true;
          break;
        case "-help":
          sendHelp();
          break;
        default:
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

    System.out.println("Remove pause from decompile.bat");
    replaceStringInFile(new File(tmpDir, "decompile.bat"), "pause", "echo pause will be skipped");

    System.out.println("Decompiling mcp");
    try {
      Runtime.getRuntime()
          .exec("cmd /C start /d \"" + tmpDir.getAbsolutePath() + "\" /wait decompile.bat")
          .waitFor();
    } catch (IOException | InterruptedException e) {
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

    File tmpSrcDir = new File(tmpDir, "src/");
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
    replaceStringInFile(pom, "de.dirty", path);
    replaceStringInFile(pom, "MavenMCP", name);

    System.out.println("Creating workspace directory");
    File workspace = new File("workspace");
    createFolder(workspace);

    System.out.println("Copying saves folder");
    File tmpSaves = new File(tmpDir, "jars/saves");
    File saves = new File(workspace, "saves");
    try {
      copyFolder(tmpSaves, saves);
    } catch (IOException e) {
      System.err.println("Error while copying saves to workspace folder");
      e.printStackTrace();
    }

    System.out.println("Copying natives folder");
    File tmpNatives = new File(tmpDir, "jars/versions/1.8.8/1.8.8-natives");
    File natives = new File(workspace, "natives");
    try {
      copyFolder(tmpNatives, natives);
    } catch (IOException e) {
      System.err.println("Error while copying natives to workspace folder");
      e.printStackTrace();
      System.err.println("Without the natives the client will not run!");
    }

    System.out.println("deleting tmp dir");
    delete(tmpDir);

    System.out.println("copy minecraft to java folder");
    File tmpMinecraftDir = new File(javaDir, "/minecraft/");
    try {
      copyFolder(tmpMinecraftDir, javaDir);
    } catch (IOException e) {
      System.err.println("Error while copying minecraft to java folder");
      e.printStackTrace();
    }

    System.out.println("Delete tmp minecraft dir");
    delete(tmpMinecraftDir);

    System.out.println("Replace the name in start file");
    replaceStringInFile(new File(javaDir, "Start.java"), "mcp", name);

    if (addOptifine) {
      System.out.println("Adding optifine");
      createFolder(tmpDir);
      File optifine = new File(tmpDir, "optifine.zip");
      downloadFile(
          "https://github.com/DasDirt/MCPRepository/raw/master/optifine_1.8.8_hd_u_h8.zip",
          optifine);
      unzip(optifine, tmpDir);
      try {
        System.out.println("Replacing wrong imports");
        handleDir(tmpDir);
        System.out.println("Delete tmp zip file");
        delete(optifine);
        System.out.println("Copy optifine src");
        copyFolder(tmpDir, javaDir);
        System.out.println("Delete tmp dir");
        delete(tmpDir);
      } catch (IOException e) {
        System.err.println("Error while copying optifine files");
        e.printStackTrace();
        System.out.println(
            "Please copy the file from: "
                + tmpDir.getAbsolutePath()
                + " to "
                + javaDir.getAbsolutePath());
      }

      System.out.println("Create intellij runs");
      File ideaFolder = new File(".idea/runConfigurations");
      createFolder(ideaFolder);
      downloadFile(
          "https://raw.githubusercontent.com/DasDirt/MCPRepository/master/StartMC.xml",
          new File(ideaFolder, "StartMC.xml"));
    }

    System.out.println(
        "---------------------------------------------------------------------------------------");
    System.out.println("Introductions for IntelliJ IDEA (idk how this works in eclipse):");
    System.out.println("1. Open IntelliJ IDEA");
    System.out.println("2. Click on Open (File -> open if you are in a project)");
    System.out.println("3. Select the folder and press ok");
    System.out.println("4. Wait a few seconds");
    System.out.println("5. A Message should be appeared in the right bottom corner");
    System.out.println("6. Mit dem Titel \"Non-managed pom.xml file found:\"");
    System.out.println("7. As soon as you see it you have to click on \"Add as Maven Project\"");
    System.out.println("8. Click on File -> Project Structure");
    System.out.println("9. Goto the Project tab and Select a Project SDK");
    System.out.println("10. Click on Ok");
    System.out.println("11. Run -> Edit Configurations");
    System.out.println("12. Select the StartMC");
    System.out.println(
        "13. Check if you have the right module and working directory(workspace folder) selected");
    System.out.println("14. Click on Ok");
    System.out.println("You're done now you should be able to start the client");
    System.out.println(
        "---------------------------------------------------------------------------------------");
  }

  /*
   * This method replaces the import "javax.vecmath.Matrix4f"
   * to "org.lwjgl.util.vector.Matrix4f".
   */
  private void handleDir(File file) {
    File[] files = file.listFiles();
    if (files != null) {
      for (File file1 : files) {
        if (file1.isDirectory()) {
          handleDir(file1);
        } else {
          System.out.println("Replacing imports in:" + file1.getName());
          replaceStringInFile(file1, "javax.vecmath.Matrix4f", "org.lwjgl.util.vector.Matrix4f");
        }
      }
    } else {
      System.err.println("No optifine content found");
    }
  }

  private void createFolder(File folder) {
    if (!folder.mkdirs()) {
      System.err.println("Cannot create the folder: " + folder.getName());
      System.exit(9);
    }
  }

  private void sendHelp() {
    System.out.println("---Usage---");
    System.out.println("-help shows this output");
    System.out.println("-name the name of your client");
    System.out.println("-path your path like de.dirty");
    System.out.println("-optifine adds optifine to your project");
    System.out.println("-----------");
  }

  private void replaceStringInFile(File file, String regex, String replacement) {
    Path path = file.toPath();
    try {
      String content = new String(Files.readAllBytes(path), CHARSET);
      content = content.replaceAll(regex, replacement);
      Files.write(path, content.getBytes(CHARSET));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // https://stackabuse.com/how-to-download-a-file-from-a-url-in-java/
  private void downloadFile(String url, File dest) {
    try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
        FileOutputStream fileOS = new FileOutputStream(dest)) {
      byte[] data = new byte[BUFFER_SIZE];
      int byteContent;
      while ((byteContent = inputStream.read(data, 0, BUFFER_SIZE)) != -1) {
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
          byte[] bytesIn = new byte[BUFFER_SIZE];
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
  private void delete(final File folder) {
    File[] files = folder.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          delete(f);
        } else {
          System.out.println("Deleting file: " + f.getName());
          if (!f.delete()) {
            System.err.println("Cannot delete the file: " + f.getName());
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
