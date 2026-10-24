package com.studenthub.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A course the student attends. Owns attendance records and timetable slots
 * (both are deleted with the subject at database level).
 */
@Entity
@Table(name = "subjects")
public class Subject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String code;

    /** Hex colour used for timetable and calendar chips, e.g. #6366F1. */
    @Column(nullable = false, length = 7)
    private String color = "#6366F1";

    @Column(name = "target_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetPercentage = new BigDecimal("75.00");

    /** Classes attended before the student started tracking in StudentHub. */
    @Column(name = "prior_attended", nullable = false)
    private Integer priorAttended = 0;

    /** Classes held before the student started tracking in StudentHub. */
    @Column(name = "prior_total", nullable = false)
    private Integer priorTotal = 0;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(BigDecimal targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    public Integer getPriorAttended() {
        return priorAttended;
    }

    public void setPriorAttended(Integer priorAttended) {
        this.priorAttended = priorAttended;
    }

    public Integer getPriorTotal() {
        return priorTotal;
    }

    public void setPriorTotal(Integer priorTotal) {
        this.priorTotal = priorTotal;
    }
}
