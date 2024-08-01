package com.example.codemangesystem;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GitCloner {
    private static final Logger logger = LoggerFactory.getLogger(GitCloner.class);
    private static final String DEFAULT_BRANCH = "main";

    // 可能丟出 GitAPIException(Git操作錯誤) 和 IOException(檔案操作錯誤)
    public String cloneRepository(String repoUrl) throws GitAPIException, IOException {
        String repoName = getRepoNameFromUrl(repoUrl);
        String localPath = "src/cloneCode/" + repoName;
        // 先嘗試複製儲存庫至臨時的資料夾 -> 取得commit的時間 -> 移動資料夾至最終路徑 -> 回傳最終路徑
        try (Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath))
                .call()) {
            // 取得最新提交的時間戳
            String timestamp = getCommitTimestamp(localPath);
            String finalPath = "src/cloneCode/" + repoName + "_" + timestamp;

            Path sourcePath = Paths.get(localPath);
            Path targetPath = Paths.get(finalPath);
            Files.move(sourcePath, targetPath);

            logger.info("Repository cloned to: {}", finalPath);
            return finalPath;
        } finally {
            // 最後刪除臨時的資料夾
            deleteDirectory(new File(localPath));
        }
    }

    private String getRepoNameFromUrl(String repoUrl) {
        // 將網址透過"/"分開
        String[] parts = repoUrl.split("/");
        // 抓取最後面的專案名稱
        String repoNameWithExtension = parts[parts.length - 1];
        // .git的部分換成 ""
        return repoNameWithExtension.replace(".git", "");
    }

    private String getCommitTimestamp(String localPath) throws IOException, GitAPIException {
        try (Git git = Git.open(new File(localPath))) {
            Repository repository = git.getRepository();
            ObjectId headCommitId = repository.resolve(DEFAULT_BRANCH);
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(headCommitId);
                Instant commitTime = Instant.ofEpochSecond(commit.getCommitTime());
                return commitTime.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            }
        }
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}
