package com.university.sigac.modules.requests.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of the course table on MI-FOR-028 (up to six). */
@Entity
@Table(name = "request_courses")
@Getter
@Setter
@NoArgsConstructor
public class RequestCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private CouncilRequest request;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "course_name", nullable = false, length = 150)
    private String courseName;

    @Column(name = "group_code", length = 20)
    private String groupCode;

    @Column(length = 20)
    private String subgroup;

    @Column(name = "course_number", length = 20)
    private String courseNumber;

    @Column(name = "grade_number", length = 10)
    private String gradeNumber;

    @Column(name = "grade_words", length = 40)
    private String gradeWords;

    @Column(name = "attended_regularly")
    private Boolean attendedRegularly;

    @Column(name = "failing_course")
    private Boolean failingCourse;
}
