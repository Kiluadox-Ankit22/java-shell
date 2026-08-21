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
            }

            // -----------------------------------------
            // Handle output redirection
            // Supports:
            // >
            // 1>
            // -----------------------------------------
            String[] redirectParts = parseRedirection(command);

            String commandPart = redirectParts[0];
            String outputFile = redirectParts[1];

            // -----------------------------------------
            // cd
            // -----------------------------------------
            if (commandPart.startsWith("cd ")) {

                String target = commandPart.substring(3).trim();

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

            // -----------------------------------------
            // pwd
            // -----------------------------------------
            } else if (commandPart.equals("pwd")) {

                if (outputFile != null) {

                    File file = new File(currentDirectory, outputFile);

                    try (java.io.PrintStream out =
                             new java.io.PrintStream(file)) {

                        out.println(currentDirectory.getAbsolutePath());
                    }

                } else {
                    System.out.println(
                        currentDirectory.getAbsolutePath()
                    );
                }

            // -----------------------------------------
            // echo
            // -----------------------------------------
            } else if (commandPart.startsWith("echo")) {

                String[] parts = tokenize(commandPart);

                if (outputFile != null) {

                    File file = new File(currentDirectory, outputFile);

                    try (java.io.PrintStream out =
                             new java.io.PrintStream(file)) {

                        for (int i = 1; i < parts.length; i++) {

                            if (i > 1) {
                                out.print(" ");
                            }

                            out.print(parts[i]);
                        }

                        out.println();
                    }

                } else {

                    for (int i = 1; i < parts.length; i++) {

                        if (i > 1) {
                            System.out.print(" ");
                        }

                        System.out.print(parts[i]);
                    }

                    System.out.println();
                }

            // -----------------------------------------
            // type
            // -----------------------------------------
            } else if (commandPart.startsWith("type")) {

                String[] parts = tokenize(commandPart);

                if (parts.length > 1) {

                    String result = type(parts[1]);

                    if (outputFile != null) {

                        File file =
                            new File(currentDirectory, outputFile);

                        try (java.io.PrintStream out =
                                 new java.io.PrintStream(file)) {

                            out.println(result);
                        }

                    } else {
                        System.out.println(result);
                    }
                }

            // -----------------------------------------
            // External commands
            // -----------------------------------------
            } else {

                String[] parts = tokenize(commandPart);

                if (parts.length == 0) {
                    continue;
                }

                String programName = parts[0];

                String executablePath = findExecutable(programName);

                if (executablePath != null) {

                    ProcessBuilder processBuilder =
                        new ProcessBuilder(parts);

                    processBuilder.directory(currentDirectory);

                    if (outputFile != null) {

                        File file =
                            new File(currentDirectory, outputFile);

                        // stdout -> file
                        processBuilder.redirectOutput(
                            ProcessBuilder.Redirect.to(file)
                        );

                        // stderr -> terminal
                        processBuilder.redirectError(
                            ProcessBuilder.Redirect.INHERIT
                        );

                    } else {

                        // stdout + stderr -> terminal
                        processBuilder.inheritIO();
                    }

                    Process process = processBuilder.start();
                    process.waitFor();

                } else {

                    System.out.println(
                        programName + ": command not found"
                    );
                }
            }
        }

        sc.close();
    }


    // =====================================================
    // Parse > and 1>
    // =====================================================
    public static String[] parseRedirection(String command) {

        // Check 1> first
        int redirectIndex = command.indexOf("1>");

        if (redirectIndex != -1) {

            String commandPart =
                command.substring(0, redirectIndex).trim();

            String outputFile =
                command.substring(redirectIndex + 2).trim();

            return new String[] {
                commandPart,
                outputFile
            };
        }

        // Check >
        redirectIndex = command.indexOf(">");

        if (redirectIndex != -1) {

            String commandPart =
                command.substring(0, redirectIndex).trim();

            String outputFile =
                command.substring(redirectIndex + 1).trim();

            return new String[] {
                commandPart,
                outputFile
            };
        }

        // No redirection
        return new String[] {
            command,
            null
        };
    }


    // =====================================================
    // Tokenizer
    // =====================================================
    public static String[] tokenize(String command) {

        List<String> arguments = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        boolean insideSingleQuotes = false;
        boolean insideDoubleQuotes = false;

        for (int i = 0; i < command.length(); i++) {

            char ch = command.charAt(i);

            // -----------------------------------------
            // Backslash outside quotes
            // -----------------------------------------
            if (ch == '\\'
                    && !insideSingleQuotes
                    && !insideDoubleQuotes) {

                if (i + 1 < command.length()) {
                    i++;
                    current.append(command.charAt(i));
                }

            // -----------------------------------------
            // Backslash inside double quotes
            // Only escapes " and \
            // -----------------------------------------
            } else if (ch == '\\' && insideDoubleQuotes) {

                if (i + 1 < command.length()) {

                    char next = command.charAt(i + 1);

                    if (next == '"' || next == '\\') {

                        i++;
                        current.append(next);

                    } else {

                        // Backslash stays literal
                        current.append('\\');
                    }
                }

            // -----------------------------------------
            // Single quote
            // -----------------------------------------
            } else if (ch == '\'' && !insideDoubleQuotes) {

                insideSingleQuotes = !insideSingleQuotes;

            // -----------------------------------------
            // Double quote
            // -----------------------------------------
            } else if (ch == '"' && !insideSingleQuotes) {

                insideDoubleQuotes = !insideDoubleQuotes;

            // -----------------------------------------
            // Whitespace outside quotes
            // -----------------------------------------
            } else if (Character.isWhitespace(ch)
                    && !insideSingleQuotes
                    && !insideDoubleQuotes) {

                if (current.length() > 0) {

                    arguments.add(current.toString());
                    current.setLength(0);
                }

            // -----------------------------------------
            // Normal character
            // -----------------------------------------
            } else {

                current.append(ch);
            }
        }

        if (current.length() > 0) {
            arguments.add(current.toString());
        }

        return arguments.toArray(new String[0]);
    }


    // =====================================================
    // type command
    // =====================================================
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

                return command + " is " +
                       file.getAbsolutePath();
            }
        }

        return command + ": not found";
    }


    // =====================================================
    // Find executable
    // =====================================================
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