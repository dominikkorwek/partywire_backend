package com.kumple.model;

import com.kumple.model.enums.RoundType;
import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoundType roundType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private QuestionCategory category;

    @Column(nullable = false)
    private boolean predefined;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id")
    private GameSession gameSession;

    protected Question() {}

    public Question(String content, RoundType roundType, QuestionCategory category, boolean predefined, GameSession gameSession) {
        this.content = content;
        this.roundType = roundType;
        this.category = category;
        this.predefined = predefined;
        this.gameSession = gameSession;
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public RoundType getRoundType() { return roundType; }
    public QuestionCategory getCategory() { return category; }
    public boolean isPredefined() { return predefined; }
    public GameSession getGameSession() { return gameSession; }
}
