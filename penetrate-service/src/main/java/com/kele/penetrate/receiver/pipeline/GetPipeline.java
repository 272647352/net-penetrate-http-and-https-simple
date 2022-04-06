package com.kele.penetrate.receiver.pipeline;

import com.kele.penetrate.enumeration.HypertextTransferProtocolType;
import com.kele.penetrate.enumeration.RequestType;
import com.kele.penetrate.factory.annotation.Autowired;
import com.kele.penetrate.factory.annotation.Recognizer;
import com.kele.penetrate.factory.annotation.Register;
import com.kele.penetrate.pojo.PipelineTransmission;
import com.kele.penetrate.protocol.GetRequest;
import com.kele.penetrate.service.ConnectHandler;
import com.kele.penetrate.service.ConnectManager;
import com.kele.penetrate.utils.UUIDUtils;
import com.kele.penetrate.utils.http.AnalysisHttpGetRequest;
import com.kele.penetrate.utils.Func;
import com.kele.penetrate.utils.PageTemplate;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Register
@SuppressWarnings("unused")
@Slf4j
@Recognizer
public class GetPipeline implements Func<PipelineTransmission, Boolean>
{

    @Autowired
    private ConnectManager connectManager;
    @Autowired
    private UUIDUtils uuidUtils;

    @Override
    public Boolean func(PipelineTransmission pipelineTransmission)
    {
        FullHttpRequest fullHttpRequest = pipelineTransmission.getFullHttpRequest();
        ChannelHandlerContext channelHandlerContext = pipelineTransmission.getChannelHandlerContext();
        HypertextTransferProtocolType hypertextTransferProtocolType = pipelineTransmission.getHypertextTransferProtocolType();
        if (AnalysisHttpGetRequest.getRequestType(fullHttpRequest) == RequestType.GET)
        {
            HttpHeaders headers = fullHttpRequest.headers();
            String contentType = headers.get("Content-Type");
            if (contentType != null)
            {
                log.error("get 不支持携带请求体");
                channelHandlerContext.writeAndFlush(PageTemplate.getAccessDeniedTemplate()).addListener(ChannelFutureListener.CLOSE);
            }
            else
            {
                Map<String, String> requestHeaders = AnalysisHttpGetRequest.getRequestHeaders(fullHttpRequest);
                String mappingName = AnalysisHttpGetRequest.getHomeUser(fullHttpRequest);
                if (mappingName == null || !connectManager.isExist(mappingName))
                {
                    channelHandlerContext.writeAndFlush(PageTemplate.getNotFoundTemplate()).addListener(ChannelFutureListener.CLOSE);
                }
                else
                {
                    ConnectHandler connectHandler = connectManager.get(mappingName);
                    if (connectHandler != null)
                    {
                        String requestUrl = AnalysisHttpGetRequest.getRequestUrl(fullHttpRequest, connectHandler.isFilterMappingName());
                        requestUrl = hypertextTransferProtocolType.getCode() + "://" + connectHandler.getMappingIp() + ":" + connectHandler.getPort() + requestUrl;

                        GetRequest getRequest = new GetRequest();
                        getRequest.setRequestId(uuidUtils.getUUID());
                        getRequest.setRequestUrl(requestUrl);
                        getRequest.setHeaders(requestHeaders);

                        connectManager.recordMsg(getRequest, channelHandlerContext);
                        connectHandler.reply(getRequest);
                    }
                    else
                    {
                        FullHttpResponse serviceUnavailableTemplate = PageTemplate.getNotFoundTemplate();
                        channelHandlerContext.writeAndFlush(serviceUnavailableTemplate).addListener(ChannelFutureListener.CLOSE);
                    }
                }
            }

            return true;
        }
        return false;
    }
}