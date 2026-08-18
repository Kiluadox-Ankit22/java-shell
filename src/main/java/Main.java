import java.io.File;
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

                File directory = new File(target);

                if (directory.isAbsolute() && directory.isDirectory()) {
                    currentDirectory = directory;
                } else {
                    System.out.println(
                        "cd: " + target + ": No such file or directory"
                    );
                }

            } else if (command.equals("pwd")) {
                System.out.println(currentDirectory.getAbsolutePath());

            } else if (command.startsWith("echo")) {
                System.out.println(command.substring(5));

            } else if (command.startsWith("type")) {
                String typeArg = command.substring(5);
                System.out.println(type(typeArg));

            } else {
                String[] parts = command.trim().split("\\s+");
                String programName = parts[0];

                String executablePath = findExecutable(programName);

                if (executablePath != null) {
                    ProcessBuilder processBuilder = new ProcessBuilder(parts);

                    processBuilder.directory(currentDirectory);
                    processBuilder.inheritIO();

                    Process process = processBuilder.start();
                    process.waitFor();

                } else {
                    System.out.println(command + ": command not found");
                }
            }
        }

        sc.close();
    }

    public static String type(String command) {
        String[] commands = {"exit", "echo", "type", "pwd", "cd"};

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