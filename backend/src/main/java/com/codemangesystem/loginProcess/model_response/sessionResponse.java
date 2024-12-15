package com.codemangesystem.loginProcess.model_response;

import com.codemangesystem.loginProcess.model_user.MyUser;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SessionResponse {
    boolean success;
    String message;
    MyUser myUser;
}
