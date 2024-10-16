package com.example.codemangesystem;

import com.example.codemangesystem.GitProcess.model_Data.Files;
import com.example.codemangesystem.GitProcess.service.GetDataBse;
import com.example.codemangesystem.GitProcess.service.GitCloner;
import com.example.codemangesystem.GitProcess.service.GitDiffAnalyzer;
import com.example.codemangesystem.LoginProcess.model_response.LoginINFO;
import com.example.codemangesystem.LoginProcess.model_response.LoginResponse;
import com.example.codemangesystem.LoginProcess.model_user.MyUser;
import com.example.codemangesystem.LoginProcess.model_response.RegisterResponse;
import com.example.codemangesystem.LoginProcess.services.MyUserService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final GitCloner gitCloner;
    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final GetDataBse getDataBse;
    private final MyUserService myUserService;

    @Autowired
    public ApiController(GitCloner gitCloner, GitDiffAnalyzer gitDiffAnalyzer, GetDataBse getDataBse
            , MyUserService myUserService) {
        this.gitCloner = gitCloner;
        this.gitDiffAnalyzer = gitDiffAnalyzer;
        this.getDataBse = getDataBse;
        this.myUserService = myUserService;
    }

    /* Git 資料處理 */
    // (前端未使用) 負責分類存儲庫的 api
    @PostMapping("/fetch-repo/categorize")
    public ResponseEntity<List<Files>> categorizeCode(@RequestParam("Path") String Path) {
        logger.info("嘗試分類 Data by " + Path);
        return new ResponseEntity<>(gitDiffAnalyzer.analyzeCommits(Path), HttpStatus.OK);
    }

    // 負責 clone 存儲庫的 api ，並將資料做分類存入資料庫
    @PostMapping("/fetch-repo")
    public ResponseEntity<?> fetchRepo(@RequestParam("url") String url) {
        try {
            logger.info("嘗試抓取 " + url + " 資料");
            return ResponseEntity.ok(gitCloner.cloneRepository(url));
        } catch (GitAPIException | IOException e) {
            logger.error("Error cloning or accessing repository: ", e);
            return ResponseEntity.status(500).body("clone 或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
    }

    // 獲取目前擁有的資料
    @GetMapping("/getProjectNames")
    public ResponseEntity<?> getProjectIdInDataBase() {
        try {
            List<String> listNames = getDataBse.getAllProjectNames();

            logger.info("目前有");
            for (String listName : listNames) {
                logger.info("Name: " + listName);
            }

            return new ResponseEntity<>(listNames, HttpStatus.OK);
        } finally {
            logger.info("結束輸出 projectNames");
        }
    }

    // 透過 ProjectName 獲取 Files 資料
    @PostMapping("/getData")
    public ResponseEntity<List<Files>> getData(@RequestParam("ProjectName") String ProjectName) {
        logger.info("嘗試抓取 Data by " + ProjectName);
        return new ResponseEntity<>(getDataBse.getFilesByProjectName(ProjectName), HttpStatus.OK);
    }

    // 手動刪除資料
    @GetMapping("/deleteData")
    public String deleteData(@RequestParam("ProjectName") String projectName) {
        return getDataBse.deleteData(projectName);
    }

    /* 登入系統 */
    // 登入 api
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginINFO loginINFO) {
        logger.info("嘗試登入 使用者: " + loginINFO);
        LoginResponse loginResponse = myUserService.checkUser(loginINFO);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    // 註冊
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody MyUser myUser) {
        logger.info("嘗試註冊");
        RegisterResponse registerResult = myUserService.userRegister(myUser);
        return new ResponseEntity<>(registerResult, HttpStatus.OK);
    }

    // 手動加入
    @GetMapping("/addSuperAccount")
    public void addSuperAccount() {
        logger.info("手動加入超級帳號");
        myUserService.AddSuperAccount();
    }
}
