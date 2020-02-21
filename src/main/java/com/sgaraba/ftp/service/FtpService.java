package com.sgaraba.ftp.service;

import com.sgaraba.ftp.FtpIntegrationApplication;
import com.sgaraba.ftp.domain.FtpConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.dsl.FtpMessageHandlerSpec;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    public CachingSessionFactory<FTPFile> createFTPSessionFactory(FtpConnection conection) {
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

        CachingSessionFactory<FTPFile> csf = createFTPSessionFactory(conection);

        IntegrationFlowContext.IntegrationFlowRegistration flow = buildFlow(csf);

        flow.getMessagingTemplate().send(messageA);
        flow.getMessagingTemplate().send(messageB);

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        flowContext.remove(flow.getId());
        csf.destroy();
    }

   /* public IntegrationFlowContext.IntegrationFlowRegistration resolve(String host, CachingSessionFactory csf) {
        IntegrationFlowContext.IntegrationFlowRegistration flow = flows.get(csf);
        if (flow == null) {
            flow = buildFlow(csf);
            flows.put(host, flow);
        }
        return flow;
    }
*/
    private IntegrationFlowContext.IntegrationFlowRegistration buildFlow(CachingSessionFactory csf) {
        StandardIntegrationFlow flow = IntegrationFlows.from(new ExecutorChannel(Executors.newSingleThreadExecutor()))
                .handle(Ftp.outboundAdapter(csf, FileExistsMode.REPLACE)
                        .useTemporaryFileName(false)
                        .fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
                        .remoteDirectory("/")
                ).get();

        return flowContext.registration(flow)
                .register();
    }
}
