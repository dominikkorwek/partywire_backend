package com.kumple.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "player_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "player_id"})
)
public class PlayerAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private Answer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voted_answer_id")
    private Answer votedAnswer;

    @Column(length = 500)
    private String freeText;

    @Column(nullable = false)
    private boolean awardedPoint;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PlayerAnswer() {}

    public PlayerAnswer(Round round, Player player, Answer answer, String freeText) {
        this.round = round;
        this.player = player;
        this.answer = answer;
        this.freeText = freeText;
    }

    public Long getId() { return id; }
    public Round getRound() { return round; }
    public Player getPlayer() { return player; }
    public Answer getAnswer() { return answer; }
    public Answer getVotedAnswer() { return votedAnswer; }
    public void setVotedAnswer(Answer votedAnswer) { this.votedAnswer = votedAnswer; }
    public String getFreeText() { return freeText; }
    public boolean isAwardedPoint() { return awardedPoint; }
    public void setAwardedPoint(boolean awardedPoint) { this.awardedPoint = awardedPoint; }
    public Instant getCreatedAt() { return createdAt; }
}
