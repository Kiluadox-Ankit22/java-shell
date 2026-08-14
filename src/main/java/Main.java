import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage;

        while (true) {
            System.out.print("$ ");

            Scanner sc = new Scanner(System.in);
            String command = sc.nextLine();

            if (command.equals("exit")) {
                break;

            } else if (command.startsWith("echo ")) {
                System.out.println(command.substring(5));

            } else if (command.startsWith("type ")) {
                String typeCommand = command.substring(5);

                if (typeCommand.equals("echo") ||
                    typeCommand.equals("exit") ||
                    typeCommand.equals("type")) {

                    System.out.println(typeCommand + " is a shell builtin");

                } else {
                    System.out.println(typeCommand + ": not found");
                }

            } else {
                System.out.println(command + ": command not found");
            }
        }
    }
}