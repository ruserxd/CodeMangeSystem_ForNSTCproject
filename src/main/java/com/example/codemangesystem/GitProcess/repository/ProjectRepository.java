package com.example.codemangesystem.GitProcess.repository;

import com.example.codemangesystem.GitProcess.model_Data.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("SELECT p.projectName FROM Project p")
    List<String> findAllProjectNames();

    Project findByProjectName(String projectName);
}

