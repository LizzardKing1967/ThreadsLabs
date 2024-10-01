package com.example;
import java.util.*;


public class Main {

    public static void main(String[] args) {
        TaskManager taskManager = new TaskManager();
        Scanner scanner = new Scanner(System.in);
        String input;

        while (true) {
            System.out.print("Введите команду: ");
            input = scanner.nextLine().trim();
            String[] tokens = input.split("\\s+");
            String command = tokens[0];

            switch (command) {
                case "start":
                    if (tokens.length == 2) {
                        try {
                            double epsilon = Double.parseDouble(tokens[1]);
                            taskManager.startTask(epsilon);
                        } catch (NumberFormatException e) {
                            System.out.println("Некорректный параметр погрешности.");
                        }
                    } else {
                        System.out.println("Неверный формат команды.");
                    }
                    break;

                case "await":
                    if (tokens.length == 2) {
                        try {
                            int taskId = Integer.parseInt(tokens[1]);
                            taskManager.awaitTask(taskId);
                        } catch (NumberFormatException e) {
                            System.out.println("Некорректный номер задачи.");
                        }
                    } else {
                        System.out.println("Неверный формат команды.");
                    }
                    break;

                case "exit":
                    taskManager.exitProgram();
                    return;

                default:
                    System.out.println("Неизвестная команда.");
                    break;
            }
        }
    }
}