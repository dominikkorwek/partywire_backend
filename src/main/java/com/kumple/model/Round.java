package com.kumple.model;

import com.kumple.model.enums.RoundStatus;
import com.kumple.model.enums.RoundType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoundType roundType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoundStatus status;

    @Column(nullable = false)
    private int roundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_player_id")
    private Player selectedPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_answer_id")
    private Answer winningAnswer;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerAnswer> playerAnswers = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant answerPhaseStartedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private boolean tiebreakRevote = false;

    protected Round() {}

    public Round(GameSession gameSession, RoundType roundType, int roundNumber, Player selectedPlayer, Question question, RoundStatus status) {
        this.gameSession = gameSession;
        this.roundType = roundType;
        this.roundNumber = roundNumber;
        this.selectedPlayer = selectedPlayer;
        this.question = question;
        this.status = status;
    }

    public Long getId() { return id; }
    public GameSession getGameSession() { return gameSession; }
    public RoundType getRoundType() { return roundType; }
    public RoundStatus getStatus() { return status; }
    public void setStatus(RoundStatus status) { this.status = status; }
    public int getRoundNumber() { return roundNumber; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public Player getSelectedPlayer() { return selectedPlayer; }
    public Answer getWinningAnswer() { return winningAnswer; }
    public void setWinningAnswer(Answer winningAnswer) { this.winningAnswer = winningAnswer; }
    public List<Answer> getAnswers() { return answers; }
    public List<PlayerAnswer> getPlayerAnswers() { return playerAnswers; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAnswerPhaseStartedAt() { return answerPhaseStartedAt; }
    public void setAnswerPhaseStartedAt(Instant answerPhaseStartedAt) { this.answerPhaseStartedAt = answerPhaseStartedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public boolean isTiebreakRevote() { return tiebreakRevote; }
    public void setTiebreakRevote(boolean tiebreakRevote) { this.tiebreakRevote = tiebreakRevote; }
}
