package com.pyin.plugin.client.dict;

import com.pyin.plugin.client.dict.model.DictItem;
import java.util.List;
import java.util.Map;

public interface PyinDictClient {

    String getLabel(String typeCode, String itemValue);

    List<DictItem> getItems(String typeCode);

    Map<String, String> getDictMap(String typeCode);
}
