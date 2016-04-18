package com.rhaut.css;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.sql.*;

public class Server {

    private Connection connection;
    private static final String TABLE = "gamedb";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public void start() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" +
                    "localhost" +
                    "/" +
                    TABLE +
                    "?user=" + USER +
                    "&password=" + PASSWORD
            );
            try (ServerSocket listener = new ServerSocket(9898)) {
                while (true) {
                    new RequestHandler(this, listener.accept()).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String login(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "username", "password")) {
            try {
                String sql = "SELECT id FROM users WHERE user_name=? AND password=?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, requestJSON.getInt("username"));
                preparedStatement.setString(2, requestJSON.getInt("password"));
                ResultSet resultSet = preparedStatement.executeQuery();
                result = new JSONObject();
                JSONObject user = new JSONObject();
                result.put("id", resultSet.getInt(1));
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("id", new JSONObject());
            }
        }
        return result.toString();
    }

    public String addUser(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_name")) {
            try {
                String sql = "INSERT INTO users (user_name) VALUES(?)";
                PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, requestJSON.getString("user_name"));
                result.put("successful", preparedStatement.executeUpdate() > 0);
                if(preparedStatement.executeUpdate() > 0) {
                    ResultSet resultSet = preparedStatement.getGeneratedKeys();
                    if(resultSet.next()) {
                        result.put("successful", true);
                        result.put("user_id", resultSet.getInt(1));
                    } else {
                        result.put("successful", false);
                    }
                } else {
                    result.put("successful", false);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("successful", false);
            }
        }
        return result.toString();
    }

    public String createGame(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "name", "has_pass", "hash_pass", "teams")) {
            try {
                String sql = "INSERT INTO games (user_id, game_name, has_pass, hash_pass) VALUES(?, ?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setInt(1, requestJSON.getInt("user_id"));
                preparedStatement.setString(2, requestJSON.getString("name"));
                preparedStatement.setBoolean(3, requestJSON.getBoolean("has_pass"));
                preparedStatement.setString(4, requestJSON.getString("hash_pass"));
                if(preparedStatement.executeUpdate() > 0) {
                    ResultSet resultSet = preparedStatement.getGeneratedKeys();
                    if(resultSet.next()) {
                        int gameId = resultSet.getInt(1);
                        sql = "INSERT INTO teams (game_id) VALUES " + getTeamValues(gameId, requestJSON.getInt("teams"));
                        preparedStatement = connection.prepareStatement(sql);
                        result.put("successful", preparedStatement.executeUpdate() > 0);
                    } else {
                        result.put("successful", false);
                    }
                } else {
                    result.put("successful", false);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("successful", false);
            }
        }
        return result.toString();
    }

    public String getGames(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id")) {
            try {
                String sql = "SELECT games.game_id, games.user_id, games.game_name, games.has_pass, players.player_count, teams.team_count, users.user_name FROM users, games LEFT JOIN ( (SELECT players.game_id, COUNT(players.user_id) AS player_count FROM players GROUP BY game_id) players ) ON games.game_id=players.game_id LEFT JOIN ( (SELECT teams.game_id, COUNT(teams.team_id) AS team_count FROM teams GROUP BY game_id) teams ) ON games.game_id=teams.game_id WHERE games.user_id=users.user_id AND teams.team_count > 0";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery();
                result = new JSONObject();
                JSONArray games = new JSONArray();
                while (resultSet.next()) {
                    JSONObject row = new JSONObject();
                    row.put("game_id", resultSet.getInt(1));
                    row.put("user_id", resultSet.getInt(2));
                    row.put("game_name", resultSet.getString(3));
                    row.put("has_pass", resultSet.getBoolean(4));
                    row.put("player_count", resultSet.getInt(5));
                    row.put("team_count", resultSet.getInt(6));
                    row.put("user_name", resultSet.getString(7));
                    games.put(row);
                }
                result.put("games", games);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("GAMES", new JSONArray());
            }
        }
        return result.toString();
    }

    public String joinGame(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "game_id")) {
            try {
                String sql = "INSERT INTO players (user_id, game_id) VALUES(?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, requestJSON.getInt("user_id"));
                preparedStatement.setInt(2, requestJSON.getInt("game_id"));
                result.put("successful", preparedStatement.executeUpdate() > 0);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("successful", false);
            }
        }
        return result.toString();
    }

    public String getPlayers(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "game_id")) {
            try {
                String sql = "SELECT users.user_id, users.user_name, players.team_id, players.points, users.latitude, users.longitude FROM users, players, games WHERE users.user_id=players.user_id AND players.game_id=games.game_id AND games.game_id=?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, requestJSON.getInt("game_id"));
                ResultSet resultSet = preparedStatement.executeQuery();
                result = new JSONObject();
                JSONArray games = new JSONArray();
                while (resultSet.next()) {
                    JSONObject row = new JSONObject();
                    row.put("user_id", resultSet.getInt(1));
                    row.put("user_name", resultSet.getString(2));
                    row.put("team_id", resultSet.getInt(3));
                    row.put("points", resultSet.getInt(4));
                    row.put("latitude", resultSet.getDouble(5));
                    row.put("longitude", resultSet.getDouble(6));
                    games.put(row);
                }
                result.put("players", games);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("players", new JSONArray());
            }
        }
        return result.toString();
    }

    public String updateCoordinates(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "longitude", "latitude")) {
            try {
                String sql = "UPDATE users SET longitude=?, latitude=? WHERE users.user_id=?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setDouble(1, requestJSON.getDouble("longitude"));
                preparedStatement.setDouble(2, requestJSON.getDouble("latitude"));
                preparedStatement.setInt(3, requestJSON.getInt("user_id"));
                result.put("successful", preparedStatement.executeUpdate() > 0);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("successful", false);
            }
        }
        return result.toString();
    }

    public String quitGame(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "game_id")) {
            try {
                String sql = "DELETE FROM players WHERE  user_id=? AND game_id=?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, requestJSON.getInt("user_id"));
                preparedStatement.setInt(2, requestJSON.getInt("game_id"));
                result.put("successful", preparedStatement.executeUpdate() > 0);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("successful", false);
            }
        }
        return result.toString();
    }

    public String deleteGame(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "game_id")) {
            try {
                String sql = "DELETE FROM games WHERE game_id=? AND user_id=?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, requestJSON.getInt("game_id"));
                preparedStatement.setInt(2, requestJSON.getInt("user_id"));
                result.put("successful", preparedStatement.executeUpdate() > 0);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("successful", false);
            }
        }
        return result.toString();
    }

    public String getMessages(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "game_id", "team_id")) {
            try {
                String sql = "SELECT message_id, user_id, user_name, message FROM users natural join messages WHERE game_id=?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, requestJSON.getInt("game_id"));
                ResultSet resultSet = preparedStatement.executeQuery();
                result = new JSONObject();
                JSONArray games = new JSONArray();
                while (resultSet.next()) {
                    JSONObject row = new JSONObject();
                    row.put("message_id", resultSet.getInt(1));
                    row.put("user_id", resultSet.getInt(2));
                    row.put("user_name", resultSet.getString(3));
                    row.put("message", resultSet.getString(4));
                    games.put(row);
                }
                result.put("messages", games);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("messages", new JSONArray());
            }
        }
        return result.toString();
    }

    public String sendMessage(JSONObject requestJSON) {
        JSONObject result = new JSONObject();
        if(contains(requestJSON, "user_id", "game_id", "team_only", "message")) {
            try {
                String sql = "INSERT INTO messages (user_id, game_id, team_only, message) VALUES(?, ?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, requestJSON.getInt("user_id"));
                preparedStatement.setInt(2, requestJSON.getInt("game_id"));
                preparedStatement.setBoolean(3, requestJSON.getBoolean("team_only"));
                preparedStatement.setString(4, requestJSON.getString("message"));
                result.put("successful", preparedStatement.executeUpdate() > 0);
            } catch (SQLException e) {
                e.printStackTrace();
                result.put("successful", false);
            }
        }
        return result.toString();
    }

    private boolean contains(JSONObject object, String... parameters) {
        boolean contains = true;
        for(int x = 0; x < parameters.length && contains; x++) {
            contains = object.has(parameters[x]);
        }
        return contains;
    }

    public String verify(String idToken) {
        String result = "";
        try {
            String url = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + idToken;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            if(con.getResponseCode() == 200) {
                JSONObject responseJSON = new JSONObject(response.toString());
                log(responseJSON.toString());
                result = responseJSON.getString("sub");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getTeamValues(int gameId, int rows) {
        String result = "";
        for(int x = 0; x < rows - 1; x++) {
            result += "(" + gameId + "),";
        }
        return result + "(" + gameId + ");";
    }

    public void log(String message) {
        System.out.println(message);
    }
}
