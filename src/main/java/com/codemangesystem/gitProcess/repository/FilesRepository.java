package com.codemangesystem.gitProcess.repository;

import com.codemangesystem.gitProcess.model_Data.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilesRepository extends JpaRepository<Files, Long> {
}