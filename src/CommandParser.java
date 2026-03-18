import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands;
    private final Map<String, String> commandDescriptions;

    public CommandParser() {
        this.commands = new HashMap<>();
        this.commandDescriptions = new HashMap<>();
    }

    public void registerCommand(String name, String description, Command command) {
        Objects.requireNonNull(name, "name не может быть null");
        Objects.requireNonNull(description, "description не может быть null");
        Objects.requireNonNull(command, "command не может быть null");

        String key = name.trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Имя команды не может быть пустым");
        }

        commands.put(key, command);
        commandDescriptions.put(key, description.trim());
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Objects.requireNonNull(commandName, "commandName не может быть null");
        Objects.requireNonNull(scanner, "scanner не может быть null");
        Objects.requireNonNull(system, "system не может быть null");

        Command command = commands.get(commandName);
        if (command == null) {
            throw new IllegalArgumentException("Неизвестная команда: " + commandName);
        }

        command.execute(scanner, system);
    }

    public void printHelp() {
        if (commands.isEmpty()) {
            System.out.println(FormatUtils.formatBox("Команды не зарегистрированы."));
            return;
        }

        System.out.println(ConsoleUtils.formatHeader("Доступные команды"));
        var names = commands.keySet().stream().sorted().toList();
        var rows = new java.util.ArrayList<String[]>();
        for (String name : names) {
            String desc = commandDescriptions.getOrDefault(name, "").trim();
            rows.add(new String[]{name, desc.isBlank() ? "—" : desc});
        }
        System.out.println(FormatUtils.formatTable(new String[]{"Command", "Description"}, rows));
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        Objects.requireNonNull(scanner, "scanner не может быть null");
        Objects.requireNonNull(system, "system не может быть null");

        if (input == null || input.isBlank()) {
            return;
        }

        String trimmed = input.trim();
        int firstSpace = indexOfWhitespace(trimmed);
        String commandName = firstSpace == -1 ? trimmed : trimmed.substring(0, firstSpace);
        String args = firstSpace == -1 ? "" : trimmed.substring(firstSpace).trim();

        if (commandName.equalsIgnoreCase("help")) {
            printHelp();
            return;
        }

        Command command = commands.get(commandName);
        if (command == null) {
            System.out.println("Неизвестная команда: " + commandName + ". Введите help для списка команд.");
            return;
        }

        if (args.isEmpty()) {
            command.execute(scanner, system);
        } else {
            try (Scanner argsScanner = new Scanner(args)) {
                command.execute(argsScanner, system);
            }
        }
    }

    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
