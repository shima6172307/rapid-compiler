package eu.project.rapid.compiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This is the main class, which gets as a command line argument the file containing the Remoteable
 * class and produces the modified class.
 */
public class RemoteableCodeGenerator {
    // Analyze the classes of the given project
    private CodeGenerator generator;
    // Data structure to be populated with app and method details for creating the rapid xml file
    private Map<String, List<MethodData>> classesMap;

    private RemoteableCodeGenerator(String projectFolderOrJavaFile) {
        System.out.println("Processing: " + projectFolderOrJavaFile);
        Path projectFolderOrJavaFilePath = Paths.get(projectFolderOrJavaFile);

        generator = new CodeGenerator();
        classesMap = new HashMap<>();

        if (Files.exists(projectFolderOrJavaFilePath)) {
            if (!Files.isDirectory(projectFolderOrJavaFilePath)) {
                if (projectFolderOrJavaFile.endsWith(".java")) {
                    processFile(projectFolderOrJavaFilePath);
                } else {
                    System.err.println("Given file is not a Java file: " + projectFolderOrJavaFile);
                }
            } else {
                processFolder(projectFolderOrJavaFilePath);
            }
        } else {
            System.err.println("File or folder does not exist: " + projectFolderOrJavaFile);
        }
    }

    private void processFile(Path javaFile) {
        String newCode = generator.generateRemoteCode(javaFile, classesMap);
        if (newCode != null) {
            backupAndModifyFile(javaFile, newCode);
        }
    }

    private void processFolder(Path projectFolder) {
        // Create a backup folder where to keep a copy of the given project
        String userHomeFolder = System.getProperty("user.home");
        Path rapidBackupFolder = Paths.get(userHomeFolder, File.separator, ".rapid-compiler-backups");
        try {
            Files.createDirectories(rapidBackupFolder);
        } catch (IOException e) {
            System.err.println("Could not create rapid backup folder: " + e);
        }

        // FIXME Create a backup of the given project. Current backup only copies the empty folder.
        try {
            Files.copy(projectFolder,
                    projectFolder.resolveSibling(rapidBackupFolder + File.separator + projectFolder.getFileName()));
        } catch (IOException e) {
            System.err.println("Could not backup the project: " + e);
        }

        try (Stream<Path> stream = Files.walk(projectFolder)) {
            // Parse the file and create the new code with offloading possibilities
            stream.filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::processFile);

            printXmlFile(classesMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void backupAndModifyFile(Path originalFile, String newCode) {
        try {
            Files.move(originalFile, originalFile.resolveSibling(originalFile + ".old"));
            FileWriter newFile = new FileWriter(originalFile.toString());
            try (BufferedWriter out = new BufferedWriter(newFile)) {
                out.write(newCode);
            }
        } catch (IOException e) {
            System.err.println("Could not create a backup for the class, " +
                    "maybe you already have a previous version of this file: " + e);
        }
    }

    private void printXmlFile(Map<String, List<MethodData>> classesMap) {

        System.out.println("<application>");
        System.out.println("\t<name>" + "TODO" + "</name>");
        for (String className : classesMap.keySet()) {
            System.out.println("\t\t<class>");
            System.out.println("\t\t\t<name>" + className + "</name>");

            for (MethodData md : classesMap.get(className)) {
                System.out.println("\t\t\t<method>");
                System.out.println("\t\t\t\t<name>" + md.methodName + "</name>");


                System.out.println("\t\t\t\t\t<Remote>");
                if (md.remoteAnnotationElements != null && md.remoteAnnotationValues != null &&
                        md.remoteAnnotationElements.size() == md.remoteAnnotationValues.size()) {
                    for (int i = 0; i < md.remoteAnnotationElements.size(); i++) {
                        String element = md.remoteAnnotationElements.get(i);
                        String value = md.remoteAnnotationValues.get(i);
                        System.out.println("\t\t\t\t\t\t<" + element + ">" + value + "</" + element + ">");
                    }
                }
                System.out.println("\t\t\t\t\t</Remote>");

                System.out.println("\t\t\t\t\t<QoS>");
                if (md.qosAnnotationTerms != null && md.qosAnnotationOperators != null &&
                        md.qosAnnotationThresholds != null &&
                        md.qosAnnotationTerms.size() == md.qosAnnotationOperators.size() &&
                        md.qosAnnotationTerms.size() == md.qosAnnotationThresholds.size()) {

                    for (int i = 0; i < md.qosAnnotationTerms.size(); i++) {
                        System.out.println("\t\t\t\t\t\t<term>" + md.qosAnnotationTerms.get(i) + "</term>");
                        System.out.println("\t\t\t\t\t\t<operator>" + md.qosAnnotationOperators.get(i) + "</operator>");
                        System.out.println("\t\t\t\t\t\t<threshold>" + md.qosAnnotationThresholds.get(i) + "</threshold>");
                    }
                }
                System.out.println("\t\t\t\t\t</QoS>");

                System.out.println("\t\t\t</method>");
            }

            System.out.println("\t\t</class>");
        }
        System.out.println("</application>");
    }

    /**
     * @param args the command line arguments -
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
        } else {
            new RemoteableCodeGenerator(args[0]);
        }
    }

    private static void usage() {
        System.out.println("usage: java -jar rapid-acceleration-compiler.jar <path_of_your_project>");
    }
}
