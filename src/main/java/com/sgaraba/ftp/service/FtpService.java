package com.sgaraba.ftp.service;

import com.sgaraba.ftp.FtpIntegrationApplication;
import com.sgaraba.ftp.domain.FtpConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FtpService {
    public final File baseFolder = new File("target" + File.separator + "toSend");
    private final IntegrationFlowContext flowContext;
    private final TaskExecutor taskExecutor;
    private final Map<String, IntegrationFlowContext.IntegrationFlowRegistration> flows = new HashMap<>();

    public FtpService(IntegrationFlowContext flowContext, TaskExecutor taskExecutor) {
        this.flowContext = flowContext;
        this.taskExecutor = taskExecutor;
    }

    public SessionFactory<FTPFile> createFTPSessionFactory(FtpConnection conection) {
        DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
        sf.setHost(conection.getHost());
        sf.setPort(conection.getPort());
        sf.setUsername(conection.getUsername());
        sf.setPassword(conection.getPassword());
        return new CachingSessionFactory<>(sf);
    }

    public void copyToFtp(FtpConnection conection) throws IOException {
        final File fileToSendA = new File(baseFolder, "a.txt");
        final File fileToSendB = new File(baseFolder, "b.txt");

        final InputStream inputStreamA = FtpIntegrationApplication.class.getResourceAsStream("/test-files/a.txt");
        final InputStream inputStreamB = FtpIntegrationApplication.class.getResourceAsStream("/test-files/b.txt");

        FileUtils.copyInputStreamToFile(inputStreamA, fileToSendA);
        FileUtils.copyInputStreamToFile(inputStreamB, fileToSendB);

        final Message<File> messageA = MessageBuilder.withPayload(fileToSendA).build();
        final Message<File> messageB = MessageBuilder.withPayload(fileToSendB).build();

        IntegrationFlowContext.IntegrationFlowRegistration flow = resolve(conection);

        flow.getMessagingTemplate().send(messageA);
        flow.getMessagingTemplate().send(messageB);

        //flowContext.remove(flow.getId());
    }

    public IntegrationFlowContext.IntegrationFlowRegistration resolve(FtpConnection conection) {
        IntegrationFlowContext.IntegrationFlowRegistration flow = flows.get(conection.getHost());
        if (flow == null) {
            flow = buildFlow(conection);
            flows.put(conection.getHost(), flow);
        }
        return flow;
    }

    private IntegrationFlowContext.IntegrationFlowRegistration buildFlow(FtpConnection conection) {
        log.info("Build flow for host {}", conection.getHost());
        StandardIntegrationFlow flow = IntegrationFlows.from(new ExecutorChannel(taskExecutor))
                .handle(Ftp.outboundAdapter(createFTPSessionFactory(conection), FileExistsMode.REPLACE)
                        .useTemporaryFileName(false)
                        .fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
                        .remoteDirectory("/")
                ).get();
        return flowContext.registration(flow).register();
    }
}
