package com.kumple.model;

import jakarta.persistence.*;

@Entity
@Table(name = "question_options")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false)
    private int displayOrder;

    protected QuestionOption() {}

    public QuestionOption(Question question, String content, int displayOrder) {
        this.question = question;
        this.content = content;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public String getContent() { return content; }
    public int getDisplayOrder() { return displayOrder; }
}
