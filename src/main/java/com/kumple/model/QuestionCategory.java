package com.kumple.model;

import jakarta.persistence.*;

@Entity
@Table(name = "question_categories")
public class QuestionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    protected QuestionCategory() {}

    public QuestionCategory(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
