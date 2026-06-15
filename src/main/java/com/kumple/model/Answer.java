package com.kumple.model;

import jakarta.persistence.*;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_player_id")
    private Player author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_player_id")
    private Player targetPlayer;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int voteCount;

    protected Answer() {}

    public Answer(Round round, String content, Player author, Player targetPlayer, boolean correct) {
        this.round = round;
        this.content = content;
        this.author = author;
        this.targetPlayer = targetPlayer;
        this.correct = correct;
    }

    public Long getId() { return id; }
    public Round getRound() { return round; }
    public String getContent() { return content; }
    public Player getAuthor() { return author; }
    public Player getTargetPlayer() { return targetPlayer; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    public void incrementVoteCount() { voteCount++; }
}
