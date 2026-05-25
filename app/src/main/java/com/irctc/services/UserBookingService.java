package com.irctc.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irctc.Util.UserServiceUtil;
import com.irctc.entities.User;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UserBookingService {

    private User user;
    private List<User> userList;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_PATH = "app/src/main/java/com/irctc/localDB/users.json";

    public UserBookingService(User user1) throws IOException {
        this.user = user1;
        File users = new File(USER_PATH);
        userList = objectMapper.readValue(users, new TypeReference<List<User>>(){});
    }

    public Boolean loginUser() {
        Optional<User> foundUser = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceUtil.checkPassword(user.getPassword(), user1.getHashPassword());
        }).findFirst();
        return foundUser.isPresent();
    }

    public Boolean signUp(User user1) {
        try {
            userList.add(user1);
            saveUserListToFile();
            return Boolean.TRUE;
        } catch (IOException ex) {
            return Boolean.FALSE;
        }
    }

    private void saveUserListToFile() throws IOException{
        File usersFile = new File(USER_PATH);
        objectMapper.writeValue(usersFile,userList);
    }
}