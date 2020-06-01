package com.webank.ai.fate.serving.proxy.rpc.services;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.webank.ai.fate.api.networking.proxy.DataTransferServiceGrpc;
import com.webank.ai.fate.api.networking.proxy.Proxy;
import com.webank.ai.fate.serving.core.bean.Context;
import com.webank.ai.fate.serving.core.bean.Dict;
import com.webank.ai.fate.serving.core.bean.GrpcConnectionPool;
import com.webank.ai.fate.serving.core.constant.StatusCode;
import com.webank.ai.fate.serving.core.rpc.core.AbstractServiceAdaptor;
import com.webank.ai.fate.serving.core.rpc.core.FateService;
import com.webank.ai.fate.serving.core.rpc.core.InboundPackage;
import com.webank.ai.fate.serving.core.rpc.core.OutboundPackage;
import com.webank.ai.fate.serving.core.rpc.router.RouterInfo;
import com.webank.ai.fate.serving.proxy.security.AuthUtils;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Description TODO
 * @Author
 **/

@Service
// TODO utu: may load from cfg file is a better choice compare to using annotation?
@FateService(name = "unaryCall", preChain = {
        "requestOverloadBreaker",
        "federationParamValidator",
        "defaultAuthentication",
        "defaultServingRouter"})

public class UnaryCallService extends AbstractServiceAdaptor<Proxy.Packet, Proxy.Packet> {
//    static final String RETURN_CODE = "retcode";
    static Map<String, String> fateErrorCodeMap = Maps.newHashMap();

    static {
        fateErrorCodeMap.put(StatusCode.PARAM_ERROR, "500");
        fateErrorCodeMap.put(StatusCode.INVALID_ROLE_ERROR, "501");
        fateErrorCodeMap.put(StatusCode.SERVICE_NOT_FOUND, "502");
        fateErrorCodeMap.put(StatusCode.SYSTEM_ERROR, "503");
        fateErrorCodeMap.put(StatusCode.OVER_LOAD_ERROR, "504");
        fateErrorCodeMap.put(StatusCode.NET_ERROR, "507");
        fateErrorCodeMap.put(StatusCode.SHUTDOWN_ERROR, "508");
        fateErrorCodeMap.put(StatusCode.GUEST_ROUTER_ERROR, "509");
    }

//    @Autowired
//    IMetricFactory metricFactory;
    GrpcConnectionPool grpcConnectionPool = GrpcConnectionPool.getPool();
    @Autowired
    AuthUtils authUtils;
    Logger logger = LoggerFactory.getLogger(UnaryCallService.class);
    @Value("${proxy.grpc.unaryCall.timeout:3000}")
    private int timeout;

    @Override
    public Proxy.Packet doService(Context context, InboundPackage<Proxy.Packet> data, OutboundPackage<Proxy.Packet> outboundPackage) {

        RouterInfo routerInfo = data.getRouterInfo();
        ManagedChannel managedChannel = null;
        try {
            Proxy.Packet sourcePackage = data.getBody();
            sourcePackage = authUtils.addAuthInfo(sourcePackage);

            managedChannel = grpcConnectionPool.getManagedChannel(routerInfo.getHost(), routerInfo.getPort());
            DataTransferServiceGrpc.DataTransferServiceFutureStub stub1 = DataTransferServiceGrpc.newFutureStub(managedChannel);

            stub1.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS);

//            metricFactory.counter("grpc.unaryCall.service", "in doService", "direction", "out", "result", "success").increment();

            context.setDownstreamBegin(System.currentTimeMillis());

            ListenableFuture<Proxy.Packet> future = stub1.unaryCall(sourcePackage);

            Proxy.Packet packet = future.get(timeout, TimeUnit.MILLISECONDS);

//            metricFactory.counter("grpc.unaryCall.service", "in doService", "direction", "in", "result", "success").increment();

            return packet;

        } catch (Exception e) {
//            metricFactory.counter("grpc.unaryCall.service", "in doService", "direction", "in", "result", "error").increment();

            e.printStackTrace();
            logger.error("unaryCall error ", e);
        } finally {
            long end = System.currentTimeMillis();
            context.setDownstreamCost(end - context.getDownstreamBegin());
        }
        return null;
    }

    @Override
    protected Proxy.Packet transformErrorMap(Context context, Map data) {
        Proxy.Packet.Builder builder = Proxy.Packet.newBuilder();
        Proxy.Data.Builder dataBuilder = Proxy.Data.newBuilder();
        Map fateMap = Maps.newHashMap();
        fateMap.put(Dict.RET_CODE, transformErrorCode(data.get(Dict.CODE).toString()));
        fateMap.put(Dict.RET_MSG, data.get(Dict.MESSAGE));
        builder.setBody(dataBuilder.setValue(ByteString.copyFromUtf8(JSON.toJSONString(fateMap))));
        return builder.build();
    }

    private String transformErrorCode(String errorCode) {
        String result = fateErrorCodeMap.get(errorCode);
        if (result != null) {
            return fateErrorCodeMap.get(errorCode);
        } else {
            return "";
        }

    }

}