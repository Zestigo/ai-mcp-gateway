package com.c.domain.protocol.model.entity;

import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageCommandEntity {

    /**
     * 协议列表数据
     */
    private List<HTTPProtocolVO> httpProtocolVOS;

}
