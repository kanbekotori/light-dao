package com.jxs.ld;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by jiangxingshang on 15/11/28.
 */
public interface Page<T> {

    int getStart();

    int getLimit();

    Map<String, String> getParams();

    void setTotal(int total);

    int getTotal();

    void setData(Collection<T> data);

    Collection<T> getData();
}
