package com.codemangesystem;

import com.codemangesystem.gitProcess.model_Data.Files;
import com.codemangesystem.gitProcess.service.GetDataBse;
import com.codemangesystem.gitProcess.service.GitCloner;
import com.codemangesystem.loginProcess.model_response.LoginINFO;
import com.codemangesystem.loginProcess.model_response.LoginResponse;
import com.codemangesystem.loginProcess.model_response.RegisterResponse;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class ApiController {

    private final GitCloner gitCloner;
    private final GetDataBse getDataBse;
    private final UserService userService;

    @Autowired
    public ApiController(GitCloner gitCloner, GetDataBse getDataBse, UserService userService) {
        this.gitCloner = gitCloner;
        this.getDataBse = getDataBse;
        this.userService = userService;
    }

    /* Git 資料處理
     *  負責 clone 存儲庫的 api
     * 並將資料做分類存入資料庫前端沒有輸入的話，commId 預設為 HEAD*/
    @PostMapping("/fetch-repo")
    public ResponseEntity<?> fetchRepository(@RequestParam("url") String url, @RequestParam("commitId") String commitId) {
        try {
            log.info("嘗試抓取 url: {} commitId: {} 的資料", url, commitId);
            return ResponseEntity.ok(gitCloner.cloneRepository(url, commitId));
        } catch (GitAPIException | IOException e) {
            log.error("Error cloning or accessing repository: ", e);
            return ResponseEntity.status(500)
                    .body("clone 或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
    }

    /**
     * 獲取目前擁有的資料
     */
    @GetMapping("/getProjectNames")
    public ResponseEntity<?> getProjectNames() {
        try {
            List<String> listNames = getDataBse.getAllProjectNames();

            log.info("目前有");
            for (String listName : listNames) {
                log.info("Name: {}", listName);
            }

            return new ResponseEntity<>(listNames, HttpStatus.OK);
        } finally {
            log.info("結束輸出 projectNames");
        }
    }

    /**
     * 透過 ProjectName 獲取 Files 資料
     */
    @PostMapping("/getData")
    public ResponseEntity<List<Files>> getFileDataByProjectName(@RequestParam("ProjectName") String projectName) {
        log.info("嘗試抓取 Data by {}", projectName);
        return new ResponseEntity<>(getDataBse.getFilesByProjectName(projectName), HttpStatus.OK);
    }

    /**
     * 刪除 by ProjectName 的資料
     */
    @GetMapping("/deleteData")
    public String deleteDataByProjectName(@RequestParam("ProjectName") String projectName) {
        return getDataBse.deleteData(projectName);
    }

    /* 登入系統 */
    /* 登入 api */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginINFO loginINFO) {
        log.info("嘗試登入 使用者: {}", loginINFO);
        LoginResponse loginResponse = userService.checkUser(loginINFO);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    /**
     * 註冊
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody MyUser myUser) {
        log.info("嘗試註冊");
        RegisterResponse registerResult = userService.userRegister(myUser);
        return new ResponseEntity<>(registerResult, HttpStatus.OK);
    }

    /**
     * 手動加入
     */
    @GetMapping("/addSuperAccount")
    public void addSuperAccount() {
        log.info("手動加入超級帳號");
        userService.addSuperAccount();
    }
}
