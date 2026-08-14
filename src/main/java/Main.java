import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage;
        while (true) {
        System.out.print("$ ");

        Scanner sc = new Scanner(System.in);
        String command = sc.nextLine();
        if (command.equals("exit")){
            break;
        } else if (command.startsWith("echo")){
            System.out.println(command.substring(5));
        } else {
            System.out.println(command + ": command not found");
        }
    }
}
