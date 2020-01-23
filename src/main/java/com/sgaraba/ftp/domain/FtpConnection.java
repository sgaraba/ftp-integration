package com.sgaraba.ftp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtpConnection {
    private String host;
    private int port;
    private String username;
    private String password;
}
