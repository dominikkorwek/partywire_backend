package com.kumple.dto;

import com.kumple.model.Room;

import java.util.List;

public record RoomResponse(
        String code,
        int maxPlayers,
        int currentPlayers,
        List<PlayerResponse> players
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getCode(),
                room.getMaxPlayers(),
                room.getPlayers().size(),
                room.getPlayers().stream().map(PlayerResponse::from).toList()
        );
    }
}
