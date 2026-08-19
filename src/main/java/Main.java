import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        File currentDirectory = new File(System.getProperty("user.dir"));

        while (true) {
            System.out.print("$ ");
            String command = sc.nextLine();

            if (command.equals("exit")) {
                break;

            } else if (command.startsWith("cd ")) {

                String target = command.substring(3).trim();

                File directory;

                if (target.equals("~")) {
                    String home = System.getenv("HOME");
                    directory = new File(home);

                } else if (target.startsWith("/")) {
                    directory = new File(target);

                } else {
                    directory = new File(currentDirectory, target);
                }

                if (directory.isDirectory()) {
                    currentDirectory = directory.getCanonicalFile();
                } else {
                    System.out.println(
                        "cd: " + target + ": No such file or directory"
                    );
                }

            } else if (command.equals("pwd")) {

                System.out.println(currentDirectory.getAbsolutePath());

            } else if (command.startsWith("echo")) {

                String[] parts = tokenize(command);

                for (int i = 1; i < parts.length; i++) {
                    if (i > 1) {
                        System.out.print(" ");
                    }
                    System.out.print(parts[i]);
                }

                System.out.println();

            } else if (command.startsWith("type")) {

                String[] parts = tokenize(command);

                if (parts.length > 1) {
                    System.out.println(type(parts[1]));
                }

            } else {

                String[] parts = tokenize(command);

                if (parts.length == 0) {
                    continue;
                }

                String programName = parts[0];

                String executablePath = findExecutable(programName);

                if (executablePath != null) {

                    ProcessBuilder processBuilder =
                        new ProcessBuilder(parts);

                    processBuilder.directory(currentDirectory);
                    processBuilder.inheritIO();

                    Process process = processBuilder.start();
                    process.waitFor();

                } else {
                    System.out.println(
                        command + ": command not found"
                    );
                }
            }
        }

        sc.close();
    }

    public static String[] tokenize(String command) {

        List<String> arguments = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        boolean insideSingleQuotes = false;

        for (int i = 0; i < command.length(); i++) {

            char ch = command.charAt(i);

            if (ch == '\'') {

                insideSingleQuotes = !insideSingleQuotes;

            } else if (Character.isWhitespace(ch)
                    && !insideSingleQuotes) {

                if (current.length() > 0) {
                    arguments.add(current.toString());
                    current.setLength(0);
                }

            } else {

                current.append(ch);
            }
        }

        if (current.length() > 0) {
            arguments.add(current.toString());
        }

        return arguments.toArray(new String[0]);
    }

    public static String type(String command) {

        String[] commands = {
            "exit",
            "echo",
            "type",
            "pwd",
            "cd"
        };

        String path = System.getenv("PATH");
        String[] pathDirs = path.split(":");

        for (int i = 0; i < commands.length; i++) {

            if (commands[i].equals(command)) {
                return command + " is a shell builtin";
            }
        }

        for (int i = 0; i < pathDirs.length; i++) {

            File file = new File(pathDirs[i], command);

            if (file.exists() && file.canExecute()) {
                return command + " is " + file.getAbsolutePath();
            }
        }

        return command + ": not found";
    }

    public static String findExecutable(String command) {

        String path = System.getenv("PATH");
        String[] pathDirs = path.split(":");

        for (int i = 0; i < pathDirs.length; i++) {

            File file = new File(pathDirs[i], command);

            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }
}