package com.jxs.ld;

import java.util.List;
import java.util.Map;

/**
 * @author jiangxingshang
 */
public interface Page<T> {

    int getStart();

    int getLimit();

    Map<String, String> getParams();

    void setTotal(int total);

    int getTotal();

    void setData(List<T> data);

    List<T> getData();
}
