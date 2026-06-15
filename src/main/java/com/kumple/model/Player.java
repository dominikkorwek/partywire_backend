package com.kumple.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String playerId;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false)
    private boolean isHost;

    @Column(nullable = false, length = 20)
    private String avatarAnimal;

    @Column(nullable = false, length = 30)
    private String avatarColor;

    @Column(nullable = false)
    private Instant joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    protected Player() {}

    public Player(String nickname, boolean isHost, String avatarAnimal, String avatarColor, Room room) {
        this.playerId = UUID.randomUUID().toString().substring(0, 8);
        this.nickname = nickname;
        this.isHost = isHost;
        this.avatarAnimal = avatarAnimal != null ? avatarAnimal : "cat";
        this.avatarColor = avatarColor != null ? avatarColor : "#f97316";
        this.joinedAt = Instant.now();
        this.room = room;
    }

    public Long getId() { return id; }
    public String getPlayerId() { return playerId; }
    public String getNickname() { return nickname; }
    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }
    public String getAvatarAnimal() { return avatarAnimal; }
    public String getAvatarColor() { return avatarColor; }
    public Instant getJoinedAt() { return joinedAt; }
    public Room getRoom() { return room; }
}
