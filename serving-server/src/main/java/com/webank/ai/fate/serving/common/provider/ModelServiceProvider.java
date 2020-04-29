package com.webank.ai.fate.serving.common.provider;


import com.webank.ai.fate.api.mlmodel.manager.ModelServiceProto;
import com.webank.ai.fate.serving.core.bean.Context;
import com.webank.ai.fate.serving.core.bean.ReturnResult;
import com.webank.ai.fate.serving.core.rpc.core.FateService;
import com.webank.ai.fate.serving.core.rpc.core.FateServiceMethod;
import com.webank.ai.fate.serving.core.rpc.core.InboundPackage;
import com.webank.ai.fate.serving.guest.provider.AbstractServingServiceProvider;
import com.webank.ai.fate.serving.model.ModelManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@FateService(name = "modelService", preChain = {
         "requestOverloadBreaker",
//        "guestBatchParamInterceptor",
//        "guestModelInterceptor",
//        "mo"
}, postChain = {
        //  "cache",

})
@Service
public class ModelServiceProvider extends AbstractServingServiceProvider {
    @Autowired
    ModelManager modelManager;


    @FateServiceMethod(name = "MODEL_LOAD")
    public Object load(Context context, InboundPackage data) {
        ModelServiceProto.PublishRequest publishRequest = (ModelServiceProto.PublishRequest) data.getBody();
        ReturnResult returnResult = modelManager.load(context, publishRequest);
        return returnResult;
    }

    @FateServiceMethod(name = "MODEL_PUBLISH_ONLINE")
    public Object bind(Context context, InboundPackage data) {
        ModelServiceProto.PublishRequest req = (ModelServiceProto.PublishRequest) data.getBody();
        ReturnResult returnResult = modelManager.bind(context, req);
        return returnResult;
    }

    @FateServiceMethod(name = "QUERY_MODEL")
    public ModelServiceProto.QueryModelResponse queryModel(Context context, InboundPackage data) {
        ModelServiceProto.QueryModelRequest req = (ModelServiceProto.QueryModelRequest) data.getBody();
        String content = modelManager.queryModel(context, req);
        ModelServiceProto.QueryModelResponse.Builder builder = ModelServiceProto.QueryModelResponse.newBuilder();
//        for(Model  model :modelList){
//            ModelServiceProto.ModelInfoEx.Builder modelInfoExBuilder = ModelServiceProto.ModelInfoEx.newBuilder();
//            modelInfoExBuilder.setNamespace(model.getNamespace()).setTableName(model.getTableName()).build();
//
//            builder.addModelInfos(modelInfoExBuilder.build());
//        }
        builder.setMessage(content);
        return builder.build();
    }


//        @Override
//    public Object doService(Context context, InboundPackage data, OutboundPackage outboundPackage) {
//
//        String  actionType  = context.getActionType();
//
//        switch (actionType){
//            case   "GET_MODEL_BY_SERVICE_ID" :
//
//                data.getBody();
//                String  serviceId = null;
//                modelManager.getModelByServiceId(serviceId);
//                break;
//            case   "MODEL_LOAD" :
//                ModelServiceProto.PublishRequest  publishRequest = (ModelServiceProto.PublishRequest)data.getBody();
//                ReturnResult returnResult = modelManager.load(context,publishRequest);
//                return  returnResult;
//
//            case   "MODEL_PUBLISH_ONLINE" :
//                ModelServiceProto.PublishRequest req=  (ModelServiceProto.PublishRequest)data.getBody();
//                modelManager.bind(context,req);
////                break;
////            case  "UNBIND":
////                modelManager.unbind();
////                break;
////
////            case  "UNLOAD":
////                modelManager.unload();
////                break;
////            case  "LIST_ALL_MODEL":
////                return  modelManager.listAllModel();
////                break;
////            case "GET_MODEL_BY_TABLE_NAME_AND_NAMESPACE":
////                modelManager.getModelByTableNameAndNamespace()
////                break;
//                default:
//        }
//
//        return null;
//    }
}