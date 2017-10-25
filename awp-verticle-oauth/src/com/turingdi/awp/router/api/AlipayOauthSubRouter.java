package com.turingdi.awp.router.api;

import com.turingdi.awp.router.SubRouter;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Leibniz.Hu
 * Created on 2017-09-26 16:54.
 */
public class AlipayOauthSubRouter implements SubRouter {
    private Logger log = LoggerFactory.getLogger(getClass());
    private Vertx vertx;

    @Override
    public Router getSubRouter() {
        if (vertx == null) {
            throw new IllegalStateException("Please set Vertx before you call getSubRouter()!!!");
        }
        Router zfbOauthRouter = Router.router(vertx);
//        zfbOauthRouter.get("/oauth/:body").handler(this::applyForOauth);//申请授权
        return zfbOauthRouter;
    }

    @Override
    public SubRouter setVertx(Vertx vertx) {
        this.vertx = vertx;
        return this;
    }

    /*
     * TODO 支付宝授权
     */
}