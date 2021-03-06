package com.sgaraba.ftp.controller;

import com.sgaraba.ftp.domain.FtpConnection;
import com.sgaraba.ftp.service.FtpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class FtpController {
    private final FtpService ftpService;

    public FtpController(FtpService ftpService) {
        this.ftpService = ftpService;
    }

    @GetMapping("/{host}/{port}")
    String start(@PathVariable String host, @PathVariable Integer port) throws IOException {
        ftpService.copyToFtp(
                FtpConnection.builder()
                        .host(host)
                        .password("password")
                        .username("testuser")
                        .port(port)
                        .build()
        );
        return String.format("Start copy to ftp host %s", host);
    }
}
