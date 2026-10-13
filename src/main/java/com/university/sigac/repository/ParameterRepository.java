package com.university.sigac.repository;

import com.university.sigac.entity.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParameterRepository extends JpaRepository<Parameter, String> {
}
