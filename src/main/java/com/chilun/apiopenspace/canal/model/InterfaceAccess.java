package com.chilun.apiopenspace.canal.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author 齿轮
 * @date 2024-03-07-16:44
 */
@Data
public class InterfaceAccess implements Serializable {
    private String accesskey;
    private Integer verifyType;
    private String secretkey;
    private BigDecimal remainingAmount;
    private Integer remainingTimes;
    private Long interfaceId;
    private Long userid;
    private Integer callTimes;
    private Integer failedCallTimes;
    private Date createTime;
    private Date updateTime;
    private Integer isDeleted;
    private static final long serialVersionUID = 1L;
}
