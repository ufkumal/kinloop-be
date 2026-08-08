package com.kinloop.backend.dto.home;

public record HomeStatusResponse(String state) {

    public static HomeStatusResponse newUser() {
        return new HomeStatusResponse("new-user");
    }

    public static HomeStatusResponse returningUser() {
        return new HomeStatusResponse("returning-user");
    }
}
