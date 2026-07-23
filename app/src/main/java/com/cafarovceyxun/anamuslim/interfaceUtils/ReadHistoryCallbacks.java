/*
 * (c) Faisal Khan. Created on 20/11/2021.
 */

package com.cafarovceyxun.anamuslim.interfaceUtils;

import com.cafarovceyxun.anamuslim.components.readHistory.ReadHistoryModel;

public interface ReadHistoryCallbacks {
    void onReadHistoryRemoved(ReadHistoryModel model);

    void onReadHistoryAdded(ReadHistoryModel model);
}
