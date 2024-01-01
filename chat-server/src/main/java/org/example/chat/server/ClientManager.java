package org.example.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {

    private final Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private String name;

    public final static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) {
                    // для  macOS
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

//    private void broadcastMessage(String message) {
//        for (ClientManager client : clients) {
//            try {
//                String[] strings = message.split(" ");
//                String firstString = strings[1];
//                char firstSymbol = firstString.charAt(0);
//                if (firstSymbol == '@') {
//                    String stringWithOutFirstChar = firstString.substring(1);
//                    if (client.name.equals(stringWithOutFirstChar)) {
//                        client.bufferedWriter.write(message);
//                        client.bufferedWriter.newLine();
//                        client.bufferedWriter.flush();
//                        break;
//                    }
//                }
//                if  (!client.name.equals(name)) {
//                    client.bufferedWriter.write(message);
//                    client.bufferedWriter.newLine();
//                    client.bufferedWriter.flush();
//                }
//
//            } catch (IOException e) {
//                closeEverything(socket, bufferedReader, bufferedWriter);
//            }
//        }
//    }


    private void broadcastMessage(String message) {
        String[] strings = message.split(" ");

        // Проверка наличия элементов в массиве strings
        if (strings.length <= 1) {
            // В массиве недостаточно элементов, чтобы выполнить дальнейшую обработку
            return;
        }

        String firstString = strings[1];
        char firstSymbol = firstString.charAt(0);

        for (ClientManager client : clients) {
            try {
                if (firstSymbol == '@') {
                    String stringWithoutFirstChar = firstString.substring(1);
                    if (client.name.equals(stringWithoutFirstChar)) {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                        return; // Прерывание цикла, так как сообщение отправлено конкретному пользователю
                    }
                }
                else {
                    // Отправка сообщений всем клиентам, исключая отправителя
                    if (!client.name.equals(name)) {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                }


            } catch (IOException e) {
                // Обработка ошибок ввода/вывода для каждого клиента
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }
}
