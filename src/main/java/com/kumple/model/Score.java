package com.kumple.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_session_id", "player_id"})
)
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int points;

    protected Score() {}

    public Score(GameSession gameSession, Player player) {
        this.gameSession = gameSession;
        this.player = player;
    }

    public Long getId() { return id; }
    public GameSession getGameSession() { return gameSession; }
    public Player getPlayer() { return player; }
    public int getPoints() { return points; }
    public void addPoints(int points) { this.points += points; }
}
