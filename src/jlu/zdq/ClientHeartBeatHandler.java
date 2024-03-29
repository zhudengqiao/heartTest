package jlu.zdq;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClientHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> heartBeat;
    //主动发送认证信息
    private InetAddress addr;

    private static final String SUCCESS_KEY = "auth_success_key";

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端启动");
        addr = InetAddress.getLocalHost();
        String ip = addr.getHostAddress();
        String key = "1234";
        String auth = ip+","+ key;
        ctx.writeAndFlush(auth);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("开始读数据");
        try {
            if (msg instanceof String) {
                String ret = (String)msg;
                if ("auth_success_key".equals(ret)) {
                    this.heartBeat = this.scheduler.scheduleWithFixedDelay(new ClientHeartBeatHandler.HeartBeatTask(ctx), 0L, 2L, TimeUnit.SECONDS);
                    System.out.println(msg);
                } else {
                    System.out.println(msg);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

//    @Override
//    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
//        if ("auth_success_key".equals(msg)) {
//            this.heartBeat = this.scheduler.scheduleWithFixedDelay(new ClientHeartBeatHandler.HeartBeatTask(ctx), 0L, 2L, TimeUnit.SECONDS);
//            System.out.println(msg);
//        } else {
//            System.out.println(msg);
//        }
//    }

    private class HeartBeatTask implements Runnable {
        private final ChannelHandlerContext ctx;

        private HeartBeatTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void run() {
            try {
                RequestInfo info = new RequestInfo();
                info.setIp(ClientHeartBeatHandler.this.addr.getHostAddress());
                Sigar sigar = new Sigar();
                CpuPerc cpuPerc = sigar.getCpuPerc();
                HashMap<String, Object> cpuPercMap = new HashMap();
                cpuPercMap.put("combined", cpuPerc.getCombined());
                cpuPercMap.put("user", cpuPerc.getUser());
                cpuPercMap.put("sys", cpuPerc.getSys());
                cpuPercMap.put("wait", cpuPerc.getWait());
                cpuPercMap.put("idle", cpuPerc.getIdle());
                Mem mem = sigar.getMem();
                HashMap<String, Object> memoryMap = new HashMap();
                memoryMap.put("total", mem.getTotal() / 1024L);
                memoryMap.put("used", mem.getUsed() / 1024L);
                memoryMap.put("free", mem.getFree() / 1024L);
                info.setCpuPercMap(cpuPercMap);
                info.setMemoryMap(memoryMap);
                this.ctx.writeAndFlush(info);
            } catch (Exception var7) {
                var7.printStackTrace();
            }

        }

        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            if (heartBeat != null) {
                heartBeat.cancel(true);
                heartBeat = null;
            }

            ctx.fireExceptionCaught(cause);
        }
    }
}
