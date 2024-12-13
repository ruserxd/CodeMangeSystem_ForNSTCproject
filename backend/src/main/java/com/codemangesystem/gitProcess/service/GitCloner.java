package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_DataBase.PersonalINFO;
import com.codemangesystem.gitProcess.model_DataBase.Project;
import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.model_Repo.RepositoryINFO;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 處理有關 Git clone 的操作
 */
// TODO: 遇上分支上的問題
// TODO: Git 原本的 diff
@Slf4j
@Service
public class GitCloner {
    // clone 存放的檔案位置
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final MyUserRepository myUserRepository;
    private final GitPuller gitPuller;
    private final PersonalRepository personalRepository;
    private final ProjectRepository projectRepository;

    public GitCloner(GitDiffAnalyzer gitDiffAnalyzer, MyUserRepository myUserRepository, GitPuller gitPuller, PersonalRepository personalRepository, ProjectRepository projectRepository) {
        this.gitDiffAnalyzer = gitDiffAnalyzer;
        this.myUserRepository = myUserRepository;
        this.gitPuller = gitPuller;
        this.personalRepository = personalRepository;
        this.projectRepository = projectRepository;
    }

    // TODO: 使用者 GitHub 的權限

    /**
     * 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
     */
    public GitResult cloneRepository(String repoUrl, String commitId, Long userId) throws GitAPIException, IOException {
        log.info("Clone by {} {} {}", repoUrl, commitId, userId);
        RepositoryINFO repoINFO = RepositoryINFO.builder()
                                                .repoName(GitFunction.getRepoNameFromUrl(repoUrl))
                                                .localPath(CLONE_LOCAL_BASE_PATH + GitFunction.getRepoNameFromUrl(repoUrl))
                                                .build();

        MyUser user = myUserRepository.findByUserId(userId)
                                      .orElse(null);

        log.info("當前 repoINFO path : {}  name : {}", repoINFO.localPath, repoINFO.repoName);
        try {
            // 如果 user 資料庫內已經存在， 直接回傳 GitResult
            List<String> projectNames = personalRepository.findProjectNameByUserId(userId);
            for (String projectName : projectNames) {
                if (projectName.equals(repoINFO.repoName)) {
                    log.info("Repository already exists at: {}", repoINFO.localPath);
                    return GitResult.builder()
                                    .message("此帳戶已經有 clone 過 " + projectName)
                                    .status(GitStatus.CLONE_FAILED)
                                    .build();
                }
            }

            log.info("Cloning to {}", repoUrl);

            // 當本地端有該儲存庫的處理
            if (GitFunction.isCloned(repoINFO.localPath)) {
                log.info("這項專案已經有人 Clone 過並存放於 {}", repoINFO.localPath);
                log.info("改執行 pull");
                GitResult result = gitPuller.pullLocalRepository(repoINFO);

                if (result.isPullSuccess()) {
                    result.setMessage("因為本地端有該存儲庫，因此改為 Pull 並成功 Pull 更新資料");
                }

                Project project = projectRepository.findByProjectName(repoINFO.repoName).orElse(null);
                if (project != null) {
                    log.info("成功獲取 {}", repoINFO.repoName);
                } else {
                    log.warn("獲取 {} 失敗", repoINFO.repoName);
                }

                PersonalINFO personalINFO = PersonalINFO.builder()
                                                        .user(user)
                                                        .project(project)
                                                        .build();
                try {
                    // 加入 HeadRevstr
                    try (Repository repo = new FileRepository(repoINFO.localPath + "/.git")) {
                        ObjectId objectId;

                        log.info("獲取 SHA1 by {}", commitId);
                        if (Objects.equals(commitId, "HEAD")) {
                            objectId = repo.resolve(Constants.HEAD);
                        } else {
                            objectId = repo.resolve(commitId);
                        }

                        if (objectId == null) {
                            log.error("無法解析 commit ID: {}", commitId);
                            return GitResult.builder()
                                            .status(GitStatus.CLONE_FAILED)
                                            .message("無法獲取正確的 commit reference")
                                            .build();
                        }

                        String headRevstr = objectId.getName();
                        personalINFO.setHeadRevstr(headRevstr);
                        personalRepository.save(personalINFO);

                        return result;
                    }
                } catch (IOException e) {
                    log.error("讀取 repository 時發生錯誤: {}", e.getMessage());
                    return GitResult.builder()
                                    .status(GitStatus.CLONE_FAILED)
                                    .message("讀取 repository 時發生錯誤: " + e.getMessage())
                                    .build();
                }
            }

            /*
             未來會用到的使用者資訊加入
             UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(login, password);
             clone.setCredentialsProvider(user);
             clone.call()
            */

            CloneCommand command = Git.cloneRepository()
                                      .setURI(repoUrl)
                                      .setDirectory(new File(repoINFO.localPath));
            /*
             將資料 clone 下來，try 達到 close
             只是要透過 Git 物件將資料 clone 下來
             clone 成功接續將資料分類存入資料庫內
            */
            try (Git git = command.call()) {

                // 當有指定的 commitId
                if (!commitId.equals("HEAD")) {
                    checkToCommitId(git, commitId);
                }

                log.info("成功 clone: {}", repoINFO.localPath);
                log.info("嘗試分類 -> gitDiffAnalyzer");

                // 執行分析專案
                return gitDiffAnalyzer.analyzeAllCommits(repoINFO.localPath, user);
            }
        } catch (GitAPIException | RevisionSyntaxException | IOException e) {
            log.error("Failed clone to {}", repoUrl, e);
            return GitResult.builder()
                            .status(GitStatus.CLONE_FAILED)
                            .message("Clone 發生 " + e)
                            .build();
        }
    }

    /**
     * 切換到指定的 commitId
     */
    public void checkToCommitId(Git git, String commitId) throws RevisionSyntaxException, IOException, GitAPIException {
        ObjectId specifyCommit = git.getRepository()
                                    .resolve(commitId);
        // 指定的 commitId 不存在
        if (specifyCommit == null) {
            log.error("Commit {} not found in repository", commitId);
            throw new IllegalArgumentException("指定的 commitId 不存在");
        }

        git.checkout()
           .setName(specifyCommit.getName())
           .call();
        log.info("成功 checked out commit: {}", commitId);
    }
}
