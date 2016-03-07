package com.rhaut.css;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class RequestHandler extends Thread {

    private Server server;
    private Socket socket;
    private String id;

    public RequestHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            this.server.log("Connection started");
            String input;
            String request;
            this.id = "105624150451843135167";
            do {
                input = in.readLine();
                this.server.log(input);
                JSONObject requestJSON = new JSONObject(input);
                request = requestJSON.getString("request");
                String result = "";
                if(request.equals("add_user")) {
                    result = this.server.addUser(requestJSON);
                } else if(request.equals("create_game")) {
                    result = this.server.createGame(requestJSON);
                } else if(request.equals("get_games")) {
                    result = this.server.getGames(requestJSON);
                } else if(request.equals("join_game")) {
                    result = this.server.joinGame(requestJSON);
                } else if(request.equals("get_players")) {
                    result = this.server.getPlayers(requestJSON);
                } else if(request.equals("update_coordinates")) {
                    result = this.server.updateCoordinates(requestJSON);
                } else if(request.equals("quit_game")) {
                    result = this.server.quitGame(requestJSON);
                } else if(request.equals("delete_game")) {
                    result = this.server.deleteGame(requestJSON);
                } else if(request.equals("send_message")) {
                    result = this.server.sendMessage(requestJSON);
                } else if(request.equals("get_messages")) {
                    result = this.server.getMessages(requestJSON);
                }
                this.server.log(result);
                out.println(result);
            } while (!request.equals("QUIT"));
            this.server.log("User quit");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}