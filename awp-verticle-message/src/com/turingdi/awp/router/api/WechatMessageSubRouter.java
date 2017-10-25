package com.turingdi.awp.router.api;

import com.turingdi.awp.entity.db.Account;
import com.turingdi.awp.entity.wechat.TemplateMessage;
import com.turingdi.awp.router.LanAccessSubRouter;
import com.turingdi.awp.router.SubRouter;
import com.turingdi.awp.util.wechat.WxApiClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.turingdi.awp.router.EventBusNamespace.*;


/**
 * @author Leibniz.Hu
 * Created on 2017-10-24 16:03.
 */
public class WechatMessageSubRouter extends LanAccessSubRouter implements SubRouter {
    private Logger log = LoggerFactory.getLogger(getClass());
    private Vertx vertx;

    @Override
    public Router getSubRouter() {
        if (vertx == null) {
            throw new IllegalStateException("Please set Vertx before you call getSubRouter()!!!");
        }
        Router wxMsgRouter = Router.router(vertx);
        wxMsgRouter.put("/kf").handler(this::sendCustomerServiceMessage);
        wxMsgRouter.put("/tp").handler(this::sendTemplateMessage);
        return wxMsgRouter;
    }

    @Override
    public SubRouter setVertx(Vertx vertx) {
        this.vertx = vertx;
        return this;
    }

    /**
     * 发送客服消息
     */
    private void sendCustomerServiceMessage(RoutingContext rc) {
        if(refuseNonLanAccess(rc)) return;
        JsonObject params = rc.getBodyAsJson();
        String openId = params.getString("openId");
        String content = params.getString("content");
        int eid = params.getInteger("eid");
        vertx.eventBus().<JsonObject>send(ADDR_ACCOUNT_DB.get(), makeMessage(COMMAND_GET_ACCOUNT_BY_ID, eid), ar -> {
            HttpServerResponse response = rc.response();
            if(ar.succeeded()){
                JsonObject acc = ar.result().body();
                vertx.executeBlocking(future -> {
                    JsonObject result = WxApiClient.sendCustomTextMessage(openId, content, acc.mapTo(Account.class));
                    future.complete(result);
                }, res -> {
                    if(res.succeeded()){
                        response.putHeader("content-type", "application/json;charset=UTF-8").end(res.result().toString());
                    } else {
                        log.error("向公众号"+acc.getString("name")+"的粉丝"+openId+"发送客服消息时抛出异常", res.cause());
                        response.setStatusCode(500).end(res.cause().getMessage());
                    }
                });
            } else {
                log.error("EventBus消息响应错误", ar.cause());
                response.setStatusCode(500).end("EventBus error!");
            }
        });
    }

    /**
     * 发送模板消息
     */
    private void sendTemplateMessage(RoutingContext rc) {
        if(refuseNonLanAccess(rc)) return;
        JsonObject params = rc.getBodyAsJson();
        String openId = params.getString("openId");
        String tmpId = params.getString("tmpId");
        String url = params.getString("url");
        int eid = params.getInteger("eid");
        Map<String, String> dataMap = getDataMapFromParam(params);
        TemplateMessage tmpMsg = new TemplateMessage().setOpenid(openId).setTemplateId(tmpId).setUrl(url).setDataMap(dataMap);
        vertx.eventBus().<JsonObject>send(ADDR_ACCOUNT_DB.get(), makeMessage(COMMAND_GET_ACCOUNT_BY_ID, eid), ar -> {
            HttpServerResponse response = rc.response();
            if(ar.succeeded()){
                JsonObject acc = ar.result().body();
                vertx.executeBlocking(future -> {
                    JsonObject result = WxApiClient.sendTemplateMessage(tmpMsg, acc.mapTo(Account.class));
                    future.complete(result);
                }, res -> {
                    if(res.succeeded()){
                        response.putHeader("content-type", "application/json;charset=UTF-8").end(res.result().toString());
                    } else {
                        log.error("向公众号"+acc.getString("name")+"的粉丝"+openId+"发送模板消息(ID="+tmpId+")时抛出异常", res.cause());
                        response.setStatusCode(500).end(res.cause().getMessage());
                    }
                });
            } else {
                log.error("EventBus消息响应错误", ar.cause());
                response.setStatusCode(500).end("EventBus error!");
            }
        });
    }

    private Map<String, String> getDataMapFromParam(JsonObject params) {
        Map<String, String> dataMap = new HashMap<>();
        for(Entry<String, Object> entry : params.getJsonObject("data").getMap().entrySet()){
            dataMap.put(entry.getKey(), entry.getValue().toString());
        }
        return dataMap;
    }
}