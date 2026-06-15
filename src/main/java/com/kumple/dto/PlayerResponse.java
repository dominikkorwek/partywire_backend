package com.kumple.dto;

import com.kumple.model.Player;

public record PlayerResponse(
        String id,
        String nickname,
        boolean isHost,
        String avatarAnimal,
        String avatarColor
) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getPlayerId(),
                player.getNickname(),
                player.isHost(),
                player.getAvatarAnimal(),
                player.getAvatarColor()
        );
    }
}
