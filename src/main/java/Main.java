import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        File currentDirectory =
                new File(System.getProperty("user.dir"));

        while (true) {

            System.out.print("$ ");
            String command = sc.nextLine();

            if (command.equals("exit")) {
                break;
            }

            // -----------------------------------------
            // Parse redirection
            // Supports:
            // >
            // 1>
            // 2>
            // -----------------------------------------

            String[] redirectParts =
                    parseRedirection(command);

            String commandPart = redirectParts[0];
            String outputFile = redirectParts[1];
            String errorFile = redirectParts[2];

            // -----------------------------------------
            // cd
            // -----------------------------------------

            if (commandPart.startsWith("cd ")) {

                String target =
                        commandPart.substring(3).trim();

                File directory;

                if (target.equals("~")) {

                    String home = System.getenv("HOME");
                    directory = new File(home);

                } else if (target.startsWith("/")) {

                    directory = new File(target);

                } else {

                    directory =
                            new File(currentDirectory, target);
                }

                if (directory.isDirectory()) {

                    currentDirectory =
                            directory.getCanonicalFile();

                } else {

                    if (errorFile != null) {

                        File file =
                                getOutputFile(
                                        currentDirectory,
                                        errorFile
                                );

                        try (PrintStream err =
                                     new PrintStream(file)) {

                            err.println(
                                    "cd: " + target +
                                    ": No such file or directory"
                            );
                        }

                    } else {

                        System.err.println(
                                "cd: " + target +
                                ": No such file or directory"
                        );
                    }
                }
            }

            // -----------------------------------------
            // pwd
            // -----------------------------------------

            else if (commandPart.equals("pwd")) {

                String output =
                        currentDirectory.getAbsolutePath();

                if (outputFile != null) {

                    File file =
                            getOutputFile(
                                    currentDirectory,
                                    outputFile
                            );

                    try (PrintStream out =
                                 new PrintStream(file)) {

                        out.println(output);
                    }

                } else {

                    System.out.println(output);
                }
            }

            // -----------------------------------------
            // echo
            // -----------------------------------------

            else if (commandPart.startsWith("echo")) {

                String[] parts =
                        tokenize(commandPart);

                if (outputFile != null) {

                    File file =
                            getOutputFile(
                                    currentDirectory,
                                    outputFile
                            );

                    try (PrintStream out =
                                 new PrintStream(file)) {

                        for (int i = 1;
                             i < parts.length;
                             i++) {

                            if (i > 1) {
                                out.print(" ");
                            }

                            out.print(parts[i]);
                        }

                        out.println();
                    }

                } else {

                    for (int i = 1;
                         i < parts.length;
                         i++) {

                        if (i > 1) {
                            System.out.print(" ");
                        }

                        System.out.print(parts[i]);
                    }

                    System.out.println();
                }
            }

            // -----------------------------------------
            // type
            // -----------------------------------------

            else if (commandPart.startsWith("type")) {

                String[] parts =
                        tokenize(commandPart);

                if (parts.length > 1) {

                    String result =
                            type(parts[1]);

                    if (outputFile != null) {

                        File file =
                                getOutputFile(
                                        currentDirectory,
                                        outputFile
                                );

                        try (PrintStream out =
                                     new PrintStream(file)) {

                            out.println(result);
                        }

                    } else {

                        System.out.println(result);
                    }
                }
            }

            // -----------------------------------------
            // External commands
            // -----------------------------------------

            else {

                String[] parts =
                        tokenize(commandPart);

                if (parts.length == 0) {
                    continue;
                }

                String programName = parts[0];

                String executablePath =
                        findExecutable(programName);

                if (executablePath != null) {

                    ProcessBuilder processBuilder =
                            new ProcessBuilder(parts);

                    processBuilder.directory(
                            currentDirectory
                    );

                    // ---------------------------------
                    // stdout
                    // ---------------------------------

                    if (outputFile != null) {

                        File file =
                                getOutputFile(
                                        currentDirectory,
                                        outputFile
                                );

                        processBuilder.redirectOutput(
                                ProcessBuilder.Redirect.to(file)
                        );

                    } else {

                        processBuilder.redirectOutput(
                                ProcessBuilder.Redirect.INHERIT
                        );
                    }

                    // ---------------------------------
                    // stderr
                    // ---------------------------------

                    if (errorFile != null) {

                        File file =
                                getOutputFile(
                                        currentDirectory,
                                        errorFile
                                );

                        processBuilder.redirectError(
                                ProcessBuilder.Redirect.to(file)
                        );

                    } else {

                        processBuilder.redirectError(
                                ProcessBuilder.Redirect.INHERIT
                        );
                    }

                    Process process =
                            processBuilder.start();

                    process.waitFor();

                } else {

                    // command not found is stderr
                    if (errorFile != null) {

                        File file =
                                getOutputFile(
                                        currentDirectory,
                                        errorFile
                                );

                        try (PrintStream err =
                                     new PrintStream(file)) {

                            err.println(
                                    programName +
                                    ": command not found"
                            );
                        }

                    } else {

                        System.err.println(
                                programName +
                                ": command not found"
                        );
                    }
                }
            }
        }

        sc.close();
    }


    // =====================================================
    // Get output/error file
    // =====================================================

    public static File getOutputFile(
            File currentDirectory,
            String outputFile) {

        String[] parts =
                tokenize(outputFile);

        if (parts.length == 0) {
            return new File(currentDirectory, outputFile);
        }

        String path = parts[0];

        // Absolute path
        if (path.startsWith("/")) {
            return new File(path);
        }

        // Relative path
        return new File(currentDirectory, path);
    }


    // =====================================================
    // Parse redirection
    //
    // Supports:
    // >
    // 1>
    // 2>
    // =====================================================

    public static String[] parseRedirection(
            String command) {

        boolean insideSingleQuotes = false;
        boolean insideDoubleQuotes = false;

        for (int i = 0;
             i < command.length();
             i++) {

            char ch = command.charAt(i);

            // -----------------------------------------
            // Backslash
            // -----------------------------------------

            if (ch == '\\' && !insideSingleQuotes) {

                if (i + 1 < command.length()) {
                    i++;
                }

                continue;
            }

            // -----------------------------------------
            // Single quote
            // -----------------------------------------

            if (ch == '\'' && !insideDoubleQuotes) {

                insideSingleQuotes =
                        !insideSingleQuotes;

                continue;
            }

            // -----------------------------------------
            // Double quote
            // -----------------------------------------

            if (ch == '"' && !insideSingleQuotes) {

                insideDoubleQuotes =
                        !insideDoubleQuotes;

                continue;
            }

            // -----------------------------------------
            // Redirection operator
            // -----------------------------------------

            if (ch == '>'
                    && !insideSingleQuotes
                    && !insideDoubleQuotes) {

                String commandPart;
                String filePart;

                // 2>
                if (i > 0 &&
                        command.charAt(i - 1) == '2') {

                    commandPart =
                            command.substring(
                                    0,
                                    i - 1
                            ).trim();

                    filePart =
                            command.substring(
                                    i + 1
                            ).trim();

                    return new String[] {
                            commandPart,
                            null,
                            filePart
                    };
                }

                // 1>
                if (i > 0 &&
                        command.charAt(i - 1) == '1') {

                    commandPart =
                            command.substring(
                                    0,
                                    i - 1
                            ).trim();

                    filePart =
                            command.substring(
                                    i + 1
                            ).trim();

                    return new String[] {
                            commandPart,
                            filePart,
                            null
                    };
                }

                // >
                commandPart =
                        command.substring(
                                0,
                                i
                        ).trim();

                filePart =
                        command.substring(
                                i + 1
                        ).trim();

                return new String[] {
                        commandPart,
                        filePart,
                        null
                };
            }
        }

        // No redirection
        return new String[] {
                command,
                null,
                null
        };
    }


    // =====================================================
    // Tokenizer
    // =====================================================

    public static String[] tokenize(
            String command) {

        List<String> arguments =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean insideSingleQuotes = false;
        boolean insideDoubleQuotes = false;

        for (int i = 0;
             i < command.length();
             i++) {

            char ch = command.charAt(i);

            // -----------------------------------------
            // Backslash outside quotes
            // -----------------------------------------

            if (ch == '\\'
                    && !insideSingleQuotes
                    && !insideDoubleQuotes) {

                if (i + 1 < command.length()) {

                    i++;

                    current.append(
                            command.charAt(i)
                    );
                }
            }

            // -----------------------------------------
            // Backslash inside double quotes
            // Only escapes " and \
            // -----------------------------------------

            else if (ch == '\\'
                    && insideDoubleQuotes) {

                if (i + 1 < command.length()) {

                    char next =
                            command.charAt(i + 1);

                    if (next == '"' ||
                            next == '\\') {

                        i++;

                        current.append(next);

                    } else {

                        current.append('\\');
                    }
                }
            }

            // -----------------------------------------
            // Single quote
            // -----------------------------------------

            else if (ch == '\''
                    && !insideDoubleQuotes) {

                insideSingleQuotes =
                        !insideSingleQuotes;
            }

            // -----------------------------------------
            // Double quote
            // -----------------------------------------

            else if (ch == '"'
                    && !insideSingleQuotes) {

                insideDoubleQuotes =
                        !insideDoubleQuotes;
            }

            // -----------------------------------------
            // Whitespace outside quotes
            // -----------------------------------------

            else if (Character.isWhitespace(ch)
                    && !insideSingleQuotes
                    && !insideDoubleQuotes) {

                if (current.length() > 0) {

                    arguments.add(
                            current.toString()
                    );

                    current.setLength(0);
                }
            }

            // -----------------------------------------
            // Normal character
            // -----------------------------------------

            else {

                current.append(ch);
            }
        }

        if (current.length() > 0) {

            arguments.add(
                    current.toString()
            );
        }

        return arguments.toArray(
                new String[0]
        );
    }


    // =====================================================
    // type command
    // =====================================================

    public static String type(
            String command) {

        String[] commands = {
                "exit",
                "echo",
                "type",
                "pwd",
                "cd"
        };

        String path =
                System.getenv("PATH");

        String[] pathDirs =
                path.split(":");

        // Builtins
        for (int i = 0;
             i < commands.length;
             i++) {

            if (commands[i].equals(command)) {

                return command +
                        " is a shell builtin";
            }
        }

        // PATH
        for (int i = 0;
             i < pathDirs.length;
             i++) {

            File file =
                    new File(
                            pathDirs[i],
                            command
                    );

            if (file.exists()
                    && file.canExecute()) {

                return command +
                        " is " +
                        file.getAbsolutePath();
            }
        }

        return command + ": not found";
    }


    // =====================================================
    // Find executable
    // =====================================================

    public static String findExecutable(
            String command) {

        String path =
                System.getenv("PATH");

        String[] pathDirs =
                path.split(":");

        for (int i = 0;
             i < pathDirs.length;
             i++) {

            File file =
                    new File(
                            pathDirs[i],
                            command
                    );

            if (file.exists()
                    && file.canExecute()) {

                return file.getAbsolutePath();
            }
        }

        return null;
    }
}