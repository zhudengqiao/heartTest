package jlu.zdq;

import io.netty.channel.*;

import java.net.InetAddress;
import java.util.HashMap;

public class ServerHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private static HashMap<String, String> AUTH_IP_MAP = new HashMap<>();
    private static final String SUCCESS_KEY = "auth_success_key";

    static {
        AUTH_IP_MAP.put("127.0.0.1", "1234");
        AUTH_IP_MAP.put("172.16.88.37", "1234");
    }

    private boolean auth(ChannelHandlerContext ctx, Object msg) {
        String[] ret = ((String)msg).split(",");
        String auth = AUTH_IP_MAP.get(ret[0]);
        if (auth != null && auth.equals(ret[1])) {
            ctx.writeAndFlush("auth_success_key");
            return true;
        } else {
            ctx.writeAndFlush("auth failure !").addListener(ChannelFutureListener.CLOSE);
            return false;
        }
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端启动");
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("服务端开始读数据");
        if (msg instanceof String) {
            this.auth(ctx, msg);
        } else if (msg instanceof RequestInfo) {
            RequestInfo info = (RequestInfo)msg;
            System.out.println("--------------------------------------------");
            System.out.println("当前主机ip为: " + info.getIp());
            System.out.println("当前主机cpu情况: ");
            HashMap<String, Object> cpu = info.getCpuPercMap();
            System.out.println("总使用率: " + cpu.get("combined"));
            System.out.println("用户使用率: " + cpu.get("user"));
            System.out.println("系统使用率: " + cpu.get("sys"));
            System.out.println("等待率: " + cpu.get("wait"));
            System.out.println("空闲率: " + cpu.get("idle"));
            System.out.println("当前主机memory情况: ");
            HashMap<String, Object> memory = info.getMemoryMap();
            System.out.println("内存总量: " + memory.get("total"));
            System.out.println("当前内存使用量: " + memory.get("used"));
            System.out.println("当前内存剩余量: " + memory.get("free"));
            System.out.println("--------------------------------------------");
            ctx.writeAndFlush("info received!");
        } else {
            ctx.writeAndFlush("connect failure!").addListener(ChannelFutureListener.CLOSE);
        }

    }

}
