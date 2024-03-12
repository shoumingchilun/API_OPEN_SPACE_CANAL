package com.chilun.apiopenspace.canal.handler;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.chilun.apiopenspace.canal.model.InterfaceAccess;
import com.chilun.apiopenspace.canal.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.alibaba.otter.canal.protocol.CanalEntry.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 齿轮
 * @date 2024-03-07-13:08
 */
@Slf4j
@Component
public class CanalMessageHandler implements ApplicationRunner {
    @Resource
    private CanalConnector connector;

    @Resource
    private RedisService redisService;

    @Override
    public void run(ApplicationArguments args) {
        int batchSize = 1000;
        //空闲空转计数器
        int emptyCount = 0;
        try {
            connector.connect();
            // 监听api_open_platform数据库下的interface_access表
            connector.subscribe("api_open_platform.interface_access");
            // 回滚到未进行 ack 的地方，下次fetch的时候，可以从最后一个没有 ack 的地方开始拿
            connector.rollback();
            // 如果3600*24*30s内没有监听到更改，则报错并停止运行
            int totalEmptyCount = 3600*24*30;
            while (emptyCount < totalEmptyCount) {
                log.info("正在监听canal Server: " + new Date());
                Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    emptyCount++;
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    //计数器重新置零
                    emptyCount = 0;
                    handlerMessage(message.getEntries());
                }
                connector.ack(batchId); // 提交确认
                // connector.rollback(batchId); // 处理失败, 回滚数据
            }
            log.error("已经监听了" + totalEmptyCount + "秒，无任何消息，请检查canal是否正常运行或连接是否成功......");
        } finally {
            connector.disconnect();
        }
    }

    public void handlerMessage(List<Entry> entrys) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Entry entry : entrys) {
            //如果不是ROWDATA，则忽略
            if (entry.getEntryType() != EntryType.ROWDATA) {
                continue;
            }
            RowChange rowchange = null;
            try {
                //获取变更的row数据
                rowchange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                log.error("解析Entry获得RowChange失败：" + entry, e);
                return;
            }
            //获取变动类型
            EventType eventType = rowchange.getEventType();
            log.info(String.format("================binlog[%s:%s] , name[%s,%s] , eventType : %s",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), entry.getHeader().getTableName(), eventType));

            for (RowData rowData : rowchange.getRowDatasList()) {
                //由于使用逻辑删除，delete实现方式为将is_deleted属性设置为1，所以删除逻辑写在update中
                if (eventType == EventType.UPDATE) {
                    InterfaceAccess interfaceAccess = parseRowDateIntoInterfaceAccess(rowData);
                    if (interfaceAccess.getIsDeleted() == 1) {
                        //说明已经删除，需要同步删除
                        redisService.deleteInterfaceAccess(interfaceAccess);
                        log.info("从redis中删除{}", interfaceAccess.getAccesskey());
                    } else {
                        //说明未删除，需要同步更新
                        redisService.updateInterfaceAccess(interfaceAccess);
                        log.info("在redis中同步{}", interfaceAccess.getAccesskey());
                    }
                }
            }
        }
    }

    private InterfaceAccess parseRowDateIntoInterfaceAccess(RowData rowData) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        InterfaceAccess interfaceAccess = new InterfaceAccess();
        String accesskey = rowData.getAfterColumns(0).getValue();
        Integer verifyType = Integer.parseInt(rowData.getAfterColumns(1).getValue());
        //允许为空的参数如果为空，则column.getValue().equals("")
        String secretkey = rowData.getAfterColumns(2).getValue().equals("") ? null : rowData.getAfterColumns(2).getValue();
        BigDecimal remainingAmount = new BigDecimal(rowData.getAfterColumns(3).getValue());
        BigDecimal cost = new BigDecimal(rowData.getAfterColumns(4).getValue());

        Long interfaceId = Long.parseLong(rowData.getAfterColumns(6).getValue());
        Long userid = Long.parseLong(rowData.getAfterColumns(7).getValue());
        Integer callTimes = Integer.parseInt(rowData.getAfterColumns(8).getValue());
        Integer failedCallTimes = Integer.parseInt(rowData.getAfterColumns(9).getValue());
        Date expiration = null;
        Date createTime = null;
        Date updateTime = null;
        try {
            expiration = sdf.parse(rowData.getAfterColumns(5).getValue());
            createTime = sdf.parse(rowData.getAfterColumns(10).getValue());
            updateTime = sdf.parse(rowData.getAfterColumns(11).getValue());
        } catch (ParseException e) {
            log.error("解析日期失败：", e);
        }
        Integer isDeleted = Integer.parseInt(rowData.getAfterColumns(12).getValue());
        interfaceAccess.setAccesskey(accesskey);
        interfaceAccess.setVerifyType(verifyType);
        interfaceAccess.setSecretkey(secretkey);
        interfaceAccess.setRemainingAmount(remainingAmount);
        interfaceAccess.setCost(cost);
        interfaceAccess.setExpiration(expiration);
        interfaceAccess.setInterfaceId(interfaceId);
        interfaceAccess.setUserid(userid);
        interfaceAccess.setCallTimes(callTimes);
        interfaceAccess.setFailedCallTimes(failedCallTimes);
        interfaceAccess.setCreateTime(createTime);
        interfaceAccess.setUpdateTime(updateTime);
        interfaceAccess.setIsDeleted(isDeleted);
        return interfaceAccess;
    }
}
