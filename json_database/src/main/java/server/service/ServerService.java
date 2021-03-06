package server.service;

import com.google.gson.*;
import server.Response;
import server.Status;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerService {

    private final String FILE_PATH = "./src/main/java/server/data/db.json";
    private final String REASON = "No such key";
    ReadWriteLock lock = new ReentrantReadWriteLock();
    public boolean exit = false;

    public Response parseString(String request) {

        JsonObject jo = JsonParser.parseString(request).getAsJsonObject();

        switch (jo.get("type").getAsString()) {
            case "get":
                return readFile(jo);
            case "set":
            case "delete":
                return writeToFile(jo);
            case "exit":
                Response response = new Response();
                response.setStatus(Status.OK);
                exit = true;
                return response;
            default:
                throw new IllegalArgumentException();
        }
    }


    public Response writeToFile(JsonObject request) {

        JsonElement jsonElement = read();

        Lock writeLock = lock.writeLock();
        writeLock.lock();

        Response response = new Response();

        try (Writer writer = Files.newBufferedWriter(Path.of(FILE_PATH), StandardCharsets.UTF_8)) {

            Gson gson = new Gson();

            if (!request.get("key").isJsonArray()) {
                gson.toJson(request, writer);
                response.setStatus(Status.OK);
            } else {

                JsonObject jo = jsonElement.getAsJsonObject();

                JsonArray keyArray = request.get("key").getAsJsonArray();
                JsonElement lastElement = keyArray.get(keyArray.size() - 1);

                if ("launches".equals(lastElement.getAsString())) {

                    JsonObject value = jo.get("value").getAsJsonObject();
                    JsonObject rocket = value.get("rocket").getAsJsonObject();
                    rocket.add("launches", request.get("value"));
                    value.add("rocket", rocket);
                    jo.add("value", value);

                    gson.toJson(jo, writer);
                    response.setStatus(Status.OK);
                } else if ("year".equals(lastElement.getAsString())) {

                    JsonObject value = jo.get("value").getAsJsonObject();
                    JsonObject car = value.get("car").getAsJsonObject();
                    car.add("year", null);
                    value.add("car", car);
                    jo.add("value", value);

                    gson.toJson(jo, writer);
                    response.setStatus(Status.OK);
                } else {
                    response.setStatus(Status.ERROR);
                    response.setReason(REASON);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        writeLock.unlock();

        return response;
    }


    public Response readFile(JsonObject request) {

        Response response = new Response();

        JsonElement jsonElement = read();

        JsonObject db = jsonElement.getAsJsonObject();

        Gson gson = new Gson();

        if ("person".equals(db.get("key").getAsString())) {

            JsonArray keyArray = request.get("key").getAsJsonArray();

            JsonElement lastElement = keyArray.get(keyArray.size() - 1);

            if ("person".equals(lastElement.getAsString())) {
                response.setStatus(Status.OK);
                response.setValue(gson.toJsonTree(db.get("value")));
            } else if ("name".equals(lastElement.getAsString())) {
                JsonObject value = db.get("value").getAsJsonObject();
                response.setStatus(Status.OK);
                response.setValue(gson.toJsonTree(value.get("name")));
            } else {
                response.setStatus(Status.OK);
                response.setValue(db.get("value"));
            }
        } else {
            response.setStatus(Status.ERROR);
            response.setReason(REASON);
        }
        return response;
    }


    public JsonElement read() {

        Lock readLock = lock.readLock();
        readLock.lock();

        JsonElement elem = null;

        try (Reader reader = Files.newBufferedReader(Path.of(FILE_PATH), StandardCharsets.UTF_8)) {

            elem = JsonParser.parseReader(reader);

        } catch (IOException e) {
            e.printStackTrace();
        }

        readLock.unlock();

        return elem;
    }
}
