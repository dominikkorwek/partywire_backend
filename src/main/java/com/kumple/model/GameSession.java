package com.kumple.model;

import com.kumple.model.enums.GameStatus;
import com.kumple.model.enums.RoundType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameStatus status = GameStatus.LOBBY;

    @Column(nullable = false)
    private int pointLimit = 100;

    @Column(nullable = false)
    private int timePerAnswer = 30;

    @Column(nullable = false)
    private int currentRoundNumber = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_round_id")
    private Round currentRound;

    @ManyToMany
    @JoinTable(
            name = "game_session_excluded_categories",
            joinColumns = @JoinColumn(name = "game_session_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<QuestionCategory> excludedCategories = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "game_session_excluded_round_types",
            joinColumns = @JoinColumn(name = "game_session_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", length = 40)
    private Set<RoundType> excludedRoundTypes = new HashSet<>();

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundNumber ASC")
    private List<Round> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Score> scores = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected GameSession() {}

    public GameSession(Room room) {
        this.room = room;
        room.setGameSession(this);
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public int getPointLimit() { return pointLimit; }
    public void setPointLimit(int pointLimit) { this.pointLimit = pointLimit; }
    public int getTimePerAnswer() { return timePerAnswer; }
    public void setTimePerAnswer(int timePerAnswer) { this.timePerAnswer = timePerAnswer; }
    public int getCurrentRoundNumber() { return currentRoundNumber; }
    public void setCurrentRoundNumber(int currentRoundNumber) { this.currentRoundNumber = currentRoundNumber; }
    public Round getCurrentRound() { return currentRound; }
    public void setCurrentRound(Round currentRound) { this.currentRound = currentRound; }
    public Set<QuestionCategory> getExcludedCategories() { return excludedCategories; }
    public Set<RoundType> getExcludedRoundTypes() { return excludedRoundTypes; }
    public List<Round> getRounds() { return rounds; }
    public List<Score> getScores() { return scores; }
    public Instant getCreatedAt() { return createdAt; }
}
