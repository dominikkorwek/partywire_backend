package com.kumple.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(length = 255)
    private String hostAuthSubject;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("joinedAt ASC")
    private List<Player> players = new ArrayList<>();

    @OneToOne(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private GameSession gameSession;

    protected Room() {}

    public Room(String code, int maxPlayers, String hostAuthSubject) {
        this.code = code;
        this.maxPlayers = maxPlayers;
        this.hostAuthSubject = hostAuthSubject;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Instant getCreatedAt() { return createdAt; }
    public String getHostAuthSubject() { return hostAuthSubject; }
    public List<Player> getPlayers() { return players; }
    public int getMaxPlayers() { return maxPlayers; }
    public GameSession getGameSession() { return gameSession; }
    public void setGameSession(GameSession gameSession) { this.gameSession = gameSession; }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public Player addPlayer(String nickname, boolean isHost, String avatarAnimal, String avatarColor) {
        Player player = new Player(nickname, isHost, avatarAnimal, avatarColor, this);
        players.add(player);
        return player;
    }

    public boolean removePlayer(String playerId) {
        return players.removeIf(p -> p.getPlayerId().equals(playerId));
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public Player getHost() {
        return players.stream()
                .filter(Player::isHost)
                .findFirst()
                .orElse(null);
    }

    public Player findByPlayerId(String playerId) {
        return players.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
}
