package com.university.sigac.core.parameter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Global key-value parameter, changeable without redeployment (RN-04). */
@Entity
@Table(name = "parameters")
@Getter @Setter @NoArgsConstructor
public class Parameter {

    @Id
    @Column(name = "param_key", length = 100)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = 500)
    private String paramValue;

    @Column(length = 300)
    private String description;

    public Parameter(String paramKey, String paramValue, String description) {
        this.paramKey = paramKey;
        this.paramValue = paramValue;
        this.description = description;
    }
}
